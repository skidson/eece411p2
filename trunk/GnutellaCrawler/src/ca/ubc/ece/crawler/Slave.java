package ca.ubc.ece.crawler;

import java.io.IOException;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Slave implements Runnable {
	public static final int MS_TO_SEC = 1000;
	public static final int FRONT = 0;
	public static final String REQUEST = "GNUTELLA CONNECT/0.6\r\n" + "User-Agent: UBCECE (crawl)\r\n" + "Query-Routing: 0.2\r\n" + "X-Ultrapeer: False\r\n" + "Crawler: 0.1\r\n" + "\r\n";
	public static final int DUMP_THRESHOLD = 10;
	public static final int REFRESH_RATE = 1000;
	
	public static final int DEFAULT_PORTNUM = 1337;
	public static final int WHISPER_PORT = DEFAULT_PORTNUM + 1;
	public static final int TRACKER_PORT = DEFAULT_PORTNUM + 2;
	public static final int REQUEST_PORT = DEFAULT_PORTNUM + 3;
	public static final int WAKE_PORT = DEFAULT_PORTNUM + 4;
	
	public static final int NIO_PORT = 9090;
	
	private String hostName;
	private int portNum;
	private int duration;
	private int timeout;
	private int fellowshipID;
	private boolean full;
	
	private InetAddress master;
	private InetAddress backup;
	
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer readBuffer = ByteBuffer.allocate(50000);
	
	private Worker worker;
	private Whisper whisper;
	
	private Crawler crawlerA;
	private Crawler crawlerB;
	
	private IPCache ipCache;
	private boolean dumpFlag;
	
	private Vector<Node> ultraList;
	private Vector<Node> leafList;
	private Vector<Node> workList;
	private Vector<Node> dumpList;
	private String[] ringList;
	
	private List<ChangeRequest> changeRequests = new Vector<ChangeRequest>();
	
	private Map pendingData = new HashMap();
	private Object syncA = new Integer(1);
	private Object syncB = new Integer(2);
	
	/* ************ INITIALIZATION ************ */
	
	public static void main(String[] args) {
//		String crawlAddress = "localhost";
//		int crawlPort =  0;
//		int timeout = 20;
//		int duration = 30;
//		boolean full = false;
//		
//		// Parse command-line bootstrap parameters
//    	for (String arg : args) {
//    		if (arg.equals("-full")) {
//    			full = true;
//    		} else if (arg.equals("-minimal")) {
//    			full = false;
//    		} else if (arg.startsWith("timeout=")) {
//    			String[] temp = arg.split("=");
//    			timeout = Integer.parseInt(temp[1])*MS_TO_SEC;
//    		} else if (arg.indexOf(":") != -1) {
//    			String[] temp = arg.split(":");
//    			crawlAddress = temp[0];
//    			crawlPort = Integer.parseInt(temp[1]);
//    		} else {
//    			duration = Integer.parseInt(arg);
//    		}
//        }
//		new Thread(new Slave(full, timeout, duration)).start();
		new Thread(new Slave()).start();
		
		// TODO have slaves idle until woken by master (see NodeTracker)
		
	}
	
	public Slave() {
		init();
	}
	
	private void init() {
		ultraList = new Vector<Node>();
		leafList = new Vector<Node>();
		workList = new Vector<Node>();
		dumpList = new Vector<Node>();
		
		dumpFlag = false;
		
		Node test = new Node("137.82.84.242", 5627);
		Node test1 = new Node("99.233.17.243", 49461);
		Node test2 = new Node("67.183.131.236", 32516);
		ultraList.add(test);
		ultraList.add(test1);
		ultraList.add(test2);
		
		try {
			selector = initSelector();
		} catch (IOException e) {}
	}
	
	private Selector initSelector() throws IOException {
		Selector socketSelector = SelectorProvider.provider().openSelector();
		
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(new InetSocketAddress(this.hostName, this.portNum));
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		
		return(socketSelector);
	}
	
	public void run() {
		worker = new Worker();
		new Thread(worker).start();
		crawlerA = new Crawler((Integer)syncA);
		crawlerB = new Crawler((Integer)syncB);
		new Thread(crawlerA).start();
		new Thread(crawlerB).start();
		
//		idle();
		
		while(true) {
			try {
				synchronized (this.changeRequests) {
					Iterator<ChangeRequest> changes = this.changeRequests.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
							break;
						case ChangeRequest.REGISTER:
							System.out.println(change.attachment.ID);
							change.socket.register(this.selector, change.ops, change.attachment);
							break;
						}
					}
					this.changeRequests.clear();
				}
				// Blocks until an event arrives at a channel
				this.selector.select();
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					if (key.isConnectable())
						this.finishConnection(key);
					else if (key.isReadable())
						this.read(key);
					else if (key.isWritable())
						this.write(key);
				}
			} catch (IOException e) {}
		}
	}
		
	/* ************ HELPER METHODS ************ */
	/*private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		
		// Register this new channel with selector to notify when readable
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}*/
		
	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Attachment at = (Attachment) key.attachment();
		Node node = at.getNode();
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		System.out.println("IN FINISH");
		try {
			socketChannel.finishConnect();
			System.out.println("Connected");
			setStatus(node, Status.CONNECTED, at.ID, false);
		} catch (SocketTimeoutException ex){
        	System.out.println("Timed out while connecting to node");
        	setStatus(node, Status.TIMEOUT, at.ID, true);
			key.cancel();
			return;
    	} catch (UnknownHostException ex) {
            System.out.println("Error: Failed to connect to node "+node.getAddress() + ":" + node.getPortNum());
            setStatus(node, Status.UNROUTABLE, at.ID, true);
			key.cancel();
			return;
    	}catch (ConnectException ex) {
    		System.out.println("Error: Connection was refused by "+node.getAddress()+":"+ node.getPortNum());
    		setStatus(node, Status.REFUSED, at.ID, true);
			key.cancel();
			return;
        } catch (IOException ex) {
            System.out.println("Error: Failed to connect to node "+node.getAddress()+":"+ node.getPortNum());
            setStatus(node, Status.INTERNAL, at.ID, true);
			key.cancel();
			return;
        }
		

		if(at.ID == (Integer)syncA){
			synchronized(syncA){
				syncA.notifyAll();
			}
		}
		else{
			synchronized(syncB){
				syncB.notifyAll();
			}
		}
	}
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		System.err.println("READING");
		this.readBuffer.clear();
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();	
			return;
		}
		
		if (numRead == -1) {
			key.cancel();
			socketChannel.close();
			return;
		}
		
		Attachment at = (Attachment) key.attachment();
		Node node = at.getNode();
		key.channel().close();
		
		byte[] data = new byte[numRead];
		System.arraycopy(this.readBuffer.array(), 0, data, 0, numRead);		
		node.setData(data);
				
		workList.add(node);

		if(at.getID() == (Integer)syncA){
			synchronized(syncA){
				syncA.notifyAll();
			}
		} else {
			synchronized(syncB){
				syncB.notifyAll();
			}
		}
			
		// TODO 
		
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		synchronized(this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);
			
			// Write until no more data
			while (!queue.isEmpty()) {
				ByteBuffer buffer = (ByteBuffer) queue.get(0);
				socketChannel.write(buffer);
				if (buffer.remaining() > 0)
					break;
				queue.remove(0);
			}
			
			if (queue.isEmpty())
				key.interestOps(SelectionKey.OP_READ);
		}
	}

	private void send(SocketChannel socket, byte[] data, Attachment attachment) {
		this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE, attachment));
		
		synchronized (this.pendingData){
			List queue = (List) this.pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList();
				this.pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		this.selector.wakeup();
	}
	
	private SocketChannel createConnection(String address, int port, Attachment attachment) throws IOException{
		SocketChannel socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    // Kick off connection establishment
	    socketChannel.connect(new InetSocketAddress(address, port));
	  
	    // Queue a channel registration since the caller is not the 
	    // selecting thread. As part of the registration we'll register
	    // an interest in connection events. These are raised when a channel
	    // is ready to complete connection establishment.
	    changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT, attachment));
	    System.out.println("created");
	    selector.wakeup();
	    return socketChannel;
	}
	
	private void setStatus(Node node, Status status, int ID, boolean connect){
		node.setStatus(status);
		if(ID == (Integer)syncA){
			synchronized(syncA){
				crawlerA.setConnectFail(connect);
				syncA.notifyAll();
			}
		}
		else{
			synchronized(syncB){
				crawlerB.setConnectFail(connect);
				syncB.notifyAll();
			}
		}
	}
	
	// Dumps node information to master
	public void dump() {
        try {
			dump(master);
		} catch (IOException e) {
			// Master node has failed, revert to backup
			try {
				dump(backup);
			} catch (IOException e1) {}
		}
	}
	
	// Dumps node information to target
	public void dump(InetAddress target) throws IOException {
		Socket socket = new Socket(target, WHISPER_PORT);
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(fellowshipID);
		oos.writeObject(ringList);
		synchronized(dumpList) {
			for (Node node : dumpList)
				oos.writeObject(node);
			dumpList.clear();
		}
		oos.writeObject(ringList);
		socket.close();
	}
	
	public void idle() {
		int ringSize = 0;
		while(true) {
			try {
				ServerSocket server = new ServerSocket(WAKE_PORT);
				Socket socket = server.accept(); // Blocks until receives a wake signal
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				ringSize = (Integer)ois.readObject();
				if (ringSize  == -1)
					ringList = (String[])ois.readObject();
				socket.close();
				server.close(); // Ensure we close the server so this node is not rewoken
				break;
			} catch (IOException e) { continue; 
			} catch (ClassNotFoundException e) { continue; }
		}
		
		if (ringSize != -1) {
			// TODO Construct ringList from node_list_fellowship. This list need not
			// be all alive as Whisper will restructure if necessary
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader("node_list_fellowship"));
			} catch (FileNotFoundException e) {
				// TODO your fucked, maybe get from master?
			}
			
			for (int i = 0; i < ringSize; i++) {
				try {
					ringList[i] = br.readLine();
				} catch (IOException e) {}
			}
		}
		
		whisper = new Whisper();
		new Thread(whisper).start();
		run();
	}
	
	public void reset() {
		// TODO kill threads if not null
		init();
		idle();
	}
	
	public boolean wake(InetAddress address) {
		// Attempts to wake target fellowship member to join this
		// node's ring. True if success, false if failure
		
		try {
			Socket socket = new Socket(address, WAKE_PORT);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(-1); // Writes a ringSize of -1 to indicate the node is joining a ring
			oos.writeObject(ringList);
		} catch (IOException e) { return false; }
		return true;
	}
	
	/* ************ EMBEDDED THREADS ************ */
	
	/* Worker thread to parse collected byte arrays into Strings */
	public class Worker implements Runnable {
		public void run() {
			while(true) {
				while(workList.isEmpty()) {
					try {
						Thread.sleep(REFRESH_RATE);
					} catch (InterruptedException e) {}
				}				
				//TODO get data from each node in workList, call parseData on it done
				for (int i = 0; i < workList.size();i++){
					//System.out.println(workList.size());
					parseData(workList.elementAt(i).getData());
				}
			}
		}
		
		private void parseData(byte[] data){
			//TODO put shit from Node here, check against cachestuff, if not in, add to ultralist/leaflist appropriately.. add to dumpList
			// and cache it. DONE
			String[] tempArray;
			String[] tempArray2;
			String[] readArray;
			String ipPort;
			String dataS = new String(data);
	        String Peers = new String();
	        String Leaves = new String();
			int startIndex;
	        int endIndex;        
	        
	        startIndex = dataS.indexOf("Peers: ");
	        if (!(startIndex == -1)) {
	        endIndex = dataS.indexOf("\n", startIndex);
	        if (!(endIndex == -1)) {
	        Peers = dataS.substring(startIndex+7, endIndex);
			
			tempArray = Peers.split(",");
			for (int j = 0; j < tempArray.length; j++) {
				ipPort = tempArray[j];
				readArray = ipPort.split(":");
				if (!(ipCache.isCached(readArray[0].toString()))) {

					readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
					portNum = Integer.parseInt(readArray[1]);
					Node tempnode = new Node(readArray[0], portNum);
					ultraList.add(tempnode);
					synchronized(ultraList){
						ultraList.notifyAll();
					}
					ipCache.cache(readArray[0]);
					dumpList.add(tempnode);
				}
				}	
			}
	        }
	        startIndex = dataS.indexOf("Leaves: ");
	        if (!(startIndex == -1)) {
	        endIndex = dataS.indexOf("\n",startIndex);
	        if (!(endIndex == -1)) {
	        //System.out.println((startIndex+8) + "  " +  endIndex);
	        Leaves = dataS.substring(startIndex+8,endIndex);
			
			tempArray2 = Leaves.split(",");
			if (!(tempArray2.length < 2)) {
				for (int k = 0; k< tempArray2.length; k++) {
					ipPort = tempArray2[k];
					readArray = ipPort.split(":");
					if (!(ipCache.isCached(readArray[0].toString()))) { 
						readArray[1] = readArray[1].replaceAll("(\\r|\\n)", ""); 
						int portNum2 = Integer.parseInt(readArray[1]);
						
						Node tempnode = new Node(readArray[0], portNum2);
						leafList.add(tempnode);
						//System.out.println(readArray[0]);
						ipCache.cache(readArray[0]);
						dumpList.add(tempnode);
					}
				}
					}
				}
			}
		}
	}
	
	public class Crawler implements Runnable {
		private Node node;
		private boolean connectFail = false;
		SocketChannel socketChannel;
		private Object id; //Used to determine which crawler needs to handle stuff
		
		public Crawler(Object ID){
			this.id = ID;
		}
		
		public void run(){
			while(true){
				connectFail = false;
				if(ultraList.size() > 0)
					node = ultraList.remove(FRONT);
				else if(leafList.size() > 0)
					node = leafList.remove(FRONT);
				else{
					// Wait for more nodes
					synchronized(ultraList) {
						try {
							System.out.println("crawler : " + (Integer)id + " waiting");
							ultraList.wait();
						} catch (InterruptedException e) {}
					}
				}
				Attachment attachment = new Attachment((Integer)id, node);	
				try {
					socketChannel = createConnection(node.getAddress(), node.getPortNum(), attachment);
				} catch (IOException e) {
					//TODO duno wtf this exception does.   Cant connect to that node?
				}
				// Wait for connection to finish before writing	
				synchronized(id) {
					try {
						System.out.println("crawler : " + (Integer)id + " waiting");
						id.wait();
					} catch (InterruptedException e) {}
				}
				if(connectFail)
					continue;
				
				System.out.println("Attempting to write  " + id);
				sendRequest(socketChannel, attachment);

				// Wait for this connection to be closed so we can open another
				synchronized(id) {
					try {
						System.out.println("Crawler : " + (Integer)id + " waiting");
						id.wait();
					} catch (InterruptedException e) {}
				}
				System.out.println(new String(node.getData()));
			}	
		}
		
		private void sendRequest(SocketChannel sc, Attachment attachment){
			 StringBuffer request = new StringBuffer();
		     request.append(REQUEST);
		     byte[] bytes = request.toString().getBytes();
		     send(sc, bytes, attachment);
		}
		
		public void setConnectFail(boolean f){
			connectFail = f;
		}
	}

	public class Whisper implements Runnable {
		ServerSocket server;
		Socket socket;
		ObjectInputStream ois;
		ObjectOutputStream oos;
		
		public void run() {
			try {
				server = new ServerSocket(WHISPER_PORT);
			} catch (IOException e) {
				System.err.println("Error: Could not establish internode communication port");
				// This node will timeout of ring requests and thus be ignored
			}
			
			while(true) {
				try {
					// Block until a connection is received on this port
					socket = server.accept();
					ois = new ObjectInputStream(socket.getInputStream());
					if (fellowshipID == (Integer)ois.readObject())
						ringList = (String[])ois.readObject();
					else
						ois.readObject(); // Discard the incoming ringList
					while(ois.available() > 0) {
						try {
							dumpList.add((Node)ois.readObject());
						} catch (ClassNotFoundException e) { continue; }
					}
					socket.close();
					
					if (dumpList.size() > DUMP_THRESHOLD || dumpFlag) {
						dumpFlag = false;
						dump();
					}
					
					String address = null;
					int index = 0;
					for (int i = 0; i < ringList.length; i++) {
						address = ringList[i];
						if (address.equals(hostName)) { // TODO hostName here must be this node's address
							address = ringList[(i+1)%ringList.length];
							index = i+1;
							break;
						}
					}
					while(true) {
						InetAddress next = InetAddress.getByName(address);
						try {
							dump(next);
							break;
						} catch (Exception ex) {
							// Could not whisper, wake another fellowship member, add to ring
							BufferedReader br = new BufferedReader(new FileReader("node_list_fellowship"));
							while(true) {
								ringList[index] = br.readLine(); // TODO make sure not null
								next = InetAddress.getByName(ringList[index]);
								if (wake(next))
									break;
							}
						}
					}
				} catch (Exception e) { continue; }
			}
		}
	}
	
	protected class ChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;
		
		private SocketChannel socket;
		private int type;
		private int ops;
		private Attachment attachment;
		
		public ChangeRequest(SocketChannel socket, int type, int ops, Attachment attachment) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;
			this.attachment = attachment;
		}
		
		public SocketChannel getSocketChannel() { return (socket); }
		
		public int getType() { return (type); }
		public int getOps() { return (ops); }
		public Attachment getAttachment() { return (attachment); }
	}
	
	public class Attachment{
		int ID;
		Node node;
		public Attachment(int id, Node node){
			this.ID = id;
			this.node = node;
		}
		
		public int getID(){ return ID; }
		public Node getNode(){ return node; }
	}
	
	public class DumpAction implements Action {
		// Sets dumpFlag so next whisper will dump to master
		public void execute() {
			dumpFlag = true;
		}
	}
}
