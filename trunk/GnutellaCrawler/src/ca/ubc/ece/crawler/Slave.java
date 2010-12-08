package ca.ubc.ece.crawler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Slave implements Runnable {
	public static final int MS_TO_SEC = 1000;
	
	private String hostName;
	private int portNum;
	private int duration;
	private int timeout;
	private boolean full;
	
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	
	private Worker worker;
	
	private List<ChangeRequest> changeRequests = new Vector();
	private Vector<Node> nodeList;
	private Map pendingData = new HashMap();
	
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
		new Thread(new Slave(crawlAddress, crawlPort, full, timeout, duration)).start();
	}
	
	public Slave(String crawlAddress, int crawlPort, boolean full, int timeout, int duration) {
		Node node = new Node(crawlAddress, crawlPort);
		// TODO
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
		
		while(true) {
			try {
				Iterator<ChangeRequest> changes = this.changeRequests.iterator();
				while (changes.hasNext()) {
					ChangeRequest change = (ChangeRequest) changes.next();
					switch (change.getType()) {
					case ChangeRequest.CHANGEOPS:
						SelectionKey key = change.socket.keyFor(this.selector);
						key.interestOps(change.getOps());
					}
					this.changeRequests.clear();
				}
				
				// Blocks until an event arrives at a channel
				this.selector.select();
				
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					if (!key.isValid())
						continue;
					else if (key.isReadable()) 
						this.read(key);
					else if (key.isWritable())
						this.write(key);
					else if (key.isAcceptable())
						this.accept(key);
				}
			} catch (IOException e) {}
		}
	}
		
	/* ************ HELPER METHODS ************ */
	
	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		
		// Register this new channel with selector to notify when readable
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}
		
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
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
		byte[] dataCopy = new byte[numRead];
		System.arraycopy(this.readBuffer.array(), 0, dataCopy, 0, numRead);
		worker.process(dataCopy);
		
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
	
	private void send(SocketChannel socket, byte[] data) {
		this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		
		synchronized (this.pendingData){
			List queue = (List) this.pendingData.get(socket);
			if (queue == null) {
				queue = new Vector();
				this.pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		
		this.selector.wakeup();
	}
	
	public void dump() {
		// TODO dumps node information to master
	}
	
	/* Worker thread to parse collected byte arrays into Node objects */
	public class Worker implements Runnable {
		Vector<byte[]> queue = new Vector<byte[]>();
		
		public void run() {
			while(true) {
				while(queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				}
				// Parse data into Node object
				
			}
		}
		
		public void process(byte[] data) {
			queue.add(data);
		}
	}
	
	public class Timer implements Runnable {
		public void run() {
			try {
				Thread.sleep(duration*MS_TO_SEC);
			} catch (InterruptedException e) {
				// Forcibly quit
			}
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
		
		public ChangeRequest(SocketChannel socket, int type, int ops) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;
		}
		
		public SocketChannel getSocketChannel() {
			return (socket);
		}
		
		public int getType() {
			return (type);
		}
		
		public int getOps() {
			return (ops);
		}
		
	}
}
