package ca.ubc.ece.crawler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class NodeManager {
	public static final int NUM_FELLOWSHIPS = 10;
	public static final int RING_SIZE  = 10;
	
	public static final int TRACKER_PORT = Master.DEFAULT_PORTNUM + 2;
	public static final int DUMP_PORT = Master.DEFAULT_PORTNUM + 3;
	public static final int WAKE_PORT = Master.DEFAULT_PORTNUM + 4;
	
	private Vector<WorkerNode> nodes;
	
	
	/* ************ INITIALIZATION ************ */
	public NodeManager(String[] nodes) {
		for (String address : nodes) {
			WorkerNode worker = new WorkerNode(address);
			this.nodes.add(worker);
		}
		for (int i = 0; i < getNumTotal(); i++)
			this.nodes.get(i).setFellowshipID(i/(getNumTotal()/NUM_FELLOWSHIPS));
	}
	
	
	/* ************ HELPER METHODS ************ */
	public void check(int index) {
		long time = System.currentTimeMillis();
		try {
			Socket socket = connect(index);
			nodes.get(index).setLatency(System.currentTimeMillis() - time);
			socket.close();
			nodes.get(index).setAlive(true);
		} catch (Exception e) {
			nodes.get(index).setAlive(false);
		}
	}
	
	private Vector<WorkerNode> getFellowshipList(int fellowshipID) {
		Vector<WorkerNode> fellowship = new Vector<WorkerNode>();
		for (WorkerNode node : nodes) {
			if (node.getFellowshipID() == fellowshipID)
				fellowship.add(node);
		}
			return fellowship;
	}
	
	// Wakes one member of each fellowship which then wakes up a ring
	public void wakeFellowships() {
		for (int i = 0; i < NUM_FELLOWSHIPS; i++) {
			Vector<WorkerNode> fellowship = getFellowshipList(i);
			for (WorkerNode node : fellowship) {
				try {
					Socket socket = connect(node, WAKE_PORT);
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeInt(RING_SIZE);
					socket.close();
				} catch (UnknownHostException e) { continue;
				} catch (IOException e) { continue; }
				break;
			}
		}
	}
	
	public void requestDump(int index) {
		try {
			Socket socket = connect(index, DUMP_PORT);
			socket.close();
		} catch (UnknownHostException e) {
		} catch (IOException e) {}
	}
	
	private Socket connect(WorkerNode node, int port) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(node.getAddress()), port);
		return socket;
	}
	
	private Socket connect(WorkerNode node) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(node.getAddress()), TRACKER_PORT);
		return socket;
	}
	
	private Socket connect(int index) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(nodes.get(index).getAddress()), TRACKER_PORT);
		return socket;
	}
	
	private Socket connect(int index, int port) throws UnknownHostException, IOException {
		Socket socket = null;
		socket = new Socket(InetAddress.getByName(nodes.get(index).getAddress()), port);
		return socket;
	}
	
	public int getNumAlive() {
		int count = 0;
		for (WorkerNode node : nodes) {
			if (node.isAlive())
				count++;
		}
		return count;
	}
	
	public int getNumDead() {
		return (getNumTotal() - getNumAlive());
	}
	
	public int getNumTotal() {
		return nodes.size();
	}
	
	public String getAddress(int index) {
		return nodes.get(index).getAddress();
	}
	
	
	/* ************ EMBEDDED CLASSES ************ */
	private class WorkerNode {
		private String address;
		private boolean alive;
		private long latency;
		private int fellowshipID;
		
		public WorkerNode(String address) {
			this.address = address;
		}
		
		protected void setLatency(long latency) {
			this.latency = latency;
		}
		
		protected void setAlive(boolean alive) {
			this.alive = alive;
		}
		
		protected void setFellowshipID(int fellowshipID) {
			this.fellowshipID = fellowshipID;
		}
		
		public String getAddress() { return this.address; }
		public long getLatency() { return this.latency; }
		public int getFellowshipID() { return this.fellowshipID; }
		public boolean isAlive() { return this.alive; }
	}
	
}
