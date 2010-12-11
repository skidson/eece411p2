package ca.ubc.ece.crawler;

import java.io.IOException;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
	public static final int WHISPER_PORT = 1338;
	public static final int REFRESH_RATE = 1000;
	
	private String hostName;
	private int portNum;
	private int duration;
	private int timeout;
	private int fellowshipID;
	private boolean full;
	
	private InetSocketAddress master;
	private InetSocketAddress backup;
	
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer readBuffer = ByteBuffer.allocate(50000);
	
	private Worker worker;
	
	private Vector<Node> ultraList;
	private Vector<Node> leafList;
	private Vector<Node> workList = new Vector<Node>();
	private Vector<Node> dumpList = new Vector<Node>();
	private String[] ringList;
	
	private List<ChangeRequest> changeRequests = new Vector();
	
	private Map pendingData = new HashMap();
	private Object syncA = new Integer(1);
	private Object syncB = new Integer(2);
	
	public static void main(String[] args) {
		String crawlAddress = "localhost";
		int crawlPort =  0;
		int timeout = 20;
		int duration = 30;
		boolean full = false;
		
		// Parse command-line bootstrap parameters
    	for (String arg : args) {
    		if (arg.equals("-full")) {
    			full = true;
    		} else if (arg.equals("-minimal")) {
    			full = false;
    		} else if (arg.startsWith("timeout=")) {
    			String[] temp = arg.split("=");
    			timeout = Integer.parseInt(temp[1])*MS_TO_SEC;
    		} else if (arg.indexOf(":") != -1) {
    			String[] temp = arg.split(":");
    			crawlAddress = temp[0];
    			crawlPort = Integer.parseInt(temp[1]);
    		} else {
    			duration = Integer.parseInt(arg);
    		}
        }
		new Thread(new Slave(full, timeout, duration)).start();
	}
	
	public Slave(boolean full, int timeout, int duration) {
		ultraList = new Vector<Node>();
		leafList = new Vector<Node>();
		workList = new Vector<Node>();
		dumpList = new Vector<Node>();
		
		Node test = new Node("137.82.84.242", 5627);
		Node test1 = new Node("99.233.17.243", 49461);
		ultraList.add(test);
		ultraList.add(test1);
		try {
			this.hostName = InetAddress.getLocalHost().getHostName();
			this.portNum = 9090;
			this.selector = initSelector();
		} catch (IOException e) {}
		// TODO populate ringlist
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
		new Thread(new Worker()).start();
		new Thread(new Crawler((Integer)syncA)).start();
		new Thread(new Crawler((Integer)syncB)).start();
		
		while(true) {
			try {
				synchronized (this.changeRequests) {
					Iterator changes = this.changeRequests.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
							break;
						case ChangeRequest.REGISTER:
							change.socket.register(this.selector, change.ops, new Integer(change.ID));
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
					if (key.isConnectable()) {
			            this.finishConnection(key);
			          } else if (key.isReadable()) {
			            this.read(key);
			          } else if (key.isWritable()) {
			            this.write(key);
			          }
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
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
			System.out.println("Connected");
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
	
		if(key.attachment().equals(syncA)){
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
		System.out.println(key.channel().toString());
		key.channel().close();
		byte[] data = new byte[numRead];
		System.arraycopy(this.readBuffer.array(), 0, data, 0, numRead);
		System.out.println(new String(data));
		// TODO ensure readBuffer contains a full entry and add it to workList
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

	private void send(SocketChannel socket, byte[] data, int ID) {
		this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE, ID));
		
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
	

	private SocketChannel createConnection(String address, int port, int ID) throws IOException{
		SocketChannel socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    // Kick off connection establishment
	    socketChannel.connect(new InetSocketAddress(address, port));
	  
	    // Queue a channel registration since the caller is not the 
	    // selecting thread. As part of the registration we'll register
	    // an interest in connection events. These are raised when a channel
	    // is ready to complete connection establishment.
	    changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT, ID));
	    System.out.println("created");
	    selector.wakeup();
	    return socketChannel;
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
	public void dump(InetSocketAddress target) throws IOException {
		Socket socket = new Socket();
		ObjectOutputStream oos;
    	socket.bind(target);
		oos = new ObjectOutputStream(socket.getOutputStream());
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
	
	/* Worker thread to parse collected byte arrays into Strings */
	public class Worker implements Runnable {
		
		public void run() {
			while(true) {
				while(workList.isEmpty()) {
					try {
						Thread.sleep(REFRESH_RATE);
					} catch (InterruptedException e) {}
				}
				// TODO parse infoz
			}
		}
	}
	
	public class Crawler implements Runnable {
		private Node node;
		SocketChannel sc;
		private Object id; //Used to determine which crawler needs to handle stuff
		
		public Crawler(Object ID){
			this.id = ID;
		}
		public void run(){
			while(true){
					if(ultraList.size() > 0)
						node = ultraList.remove(FRONT);
					else if(leafList.size() > 0)
						node = leafList.remove(FRONT);
					else{
						System.out.println("outta nodes");
						synchronized(id) {
							try {
								System.out.println("crawler : " + (Integer)id + " waiting");
								id.wait();
							} catch (InterruptedException e) {
								
							}
						}
					}
						
					try {
						sc = createConnection(node.getAddress(), node.getPortNum(), (Integer)id);
					} catch (IOException e) {
						//do stuff
					}
				
				//wait for connection to finish before writing	
				synchronized(id) {
					try {
						System.out.println("crawler : " + (Integer)id + " waiting");
						id.wait();
					} catch (InterruptedException e) {
						
					}
				}
				System.out.println("Attempting to write");
				sendRequest(sc);
				
				//wait for this connection to be closed so we can open another
				synchronized(id) {
					try {
						System.out.println("crawler : " + (Integer)id + " waiting");
						id.wait();
					} catch (InterruptedException e) {
						
					}
				}
			}	
		}
		
		public void sendRequest(SocketChannel sc){
			 StringBuffer request = new StringBuffer();
		     request.append(REQUEST);
		     byte[] bytes = request.toString().getBytes();
		     send(sc, bytes, (Integer)id);
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
						// Discard the incoming ringList
						ois.readObject();
					while(ois.available() > 0) {
						try {
							dumpList.add((Node)ois.readObject());
						} catch (ClassNotFoundException e) { continue; }
					}
					socket.close();
					
					String[] address = null;
					for (int i = 0; i < ringList.length; i++) {
						address = ringList[i].split(":");
						if (address[0].equals(hostName)) { // TODO hostName here must be this node's address
							address = ringList[(i+1)%(ringList.length)].split(":");
							break;
						}
					}
					dump(new InetSocketAddress(address[0], Integer.parseInt(address[1])));
				} catch (Exception e) { continue; }
			}
		}
	}
	
	public class Timer implements Runnable {
		public void run() {
			try {
				Thread.sleep(duration*MS_TO_SEC);
			} catch (InterruptedException e) { /* Forcibly quit */ }
			System.out.println("Timer has expired, terminating...");
			dump();
			System.exit(0);
		}
	}
	
	protected class ChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;
		
		private SocketChannel socket;
		private int type;
		private int ops;
		private int ID;
		
		public ChangeRequest(SocketChannel socket, int type, int ops, int ID) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;
			this.ID = ID;
		}
		
		public SocketChannel getSocketChannel() { return (socket); }
		
		public int getType() { return (type); }
		public int getOps() { return (ops); }
		public int getID() { return (ID); }
		
	}
}
