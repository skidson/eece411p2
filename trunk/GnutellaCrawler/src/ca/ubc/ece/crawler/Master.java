package ca.ubc.ece.crawler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class Master implements Runnable {
	public static final String DEFAULT_HOSTNAME = "localhost";
	public static final String DEFAULT_OUTPUT = "results.txt";
	public static final int DEFAULT_PORTNUM = 1337;
	public static final int WHISPER_PORT = 1338;
	
	public static final int NUM_FELLOWSHIPS = 10;
	public static final int RING_SIZE  = 10;
	public static final int AWOL_TIMER = 60;
	
	public static final int MS_TO_SEC = 1000;
	public static final int FRONT = 0;
	
	private InetAddress hostName;
	private int portNum;
	
	private static int timeout = 20;
	private static int duration = 30; 
	private static boolean verbose = false;
	private static boolean full = false;
	
	private String[][] fellowships;
	
	private Vector<Node> nodeList;
	private Vector<Logger> workerList;
	private ServerSocket server;
	
	/* ************ INITIALIZATION ************ */
	
	public static void main(String[] args) {
		String hostName = DEFAULT_HOSTNAME;
		int portNum = DEFAULT_PORTNUM;
		
		// Parse command-line bootstrap parameters
        if (args.length < 2){
            System.out.println("Usage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
            return;
        } else {
        	
        	for (String arg : args) {
        		if (arg.equals("-full")) {
        			full = true;
        		} else if (arg.equals("-minimal")) {
        			full = false;
        		} else if (arg.equals("-v")) {
        			verbose = true;
        		} else if (arg.startsWith("timeout=")) {
        			String[] temp = arg.split("=");
        			timeout = Integer.parseInt(temp[1])*MS_TO_SEC;
        		} else if (arg.indexOf(":") != -1) {
        			String[] temp = arg.split(":");
        			hostName = temp[0];
        			portNum = Integer.parseInt(temp[1]);
        		} else {
        			duration = Integer.parseInt(arg);
        		}
            }

    		if (full)
    			System.out.println("Output mode: full");
    		else
    			System.out.println("Output mode: minimal");
    		System.out.println("Verbose mode: set");
    		System.out.println("Connection timeout: " + timeout/MS_TO_SEC + " second(s)");
    		System.out.println("Execution time: " + duration + " minute(s)\n");
    	}
		try {
			new Thread(new Master(InetAddress.getByName(hostName), portNum, full, verbose, timeout, duration)).start();
		} catch (UnknownHostException e) {
			System.err.println("Error: Could not resolve host");
		}
	}
	
	public Master(InetAddress hostName, int portNum, boolean full, boolean verbose, int timeout, int duration) {
		this.hostName = hostName;
		this.portNum = portNum;
		this.full = full;
		this.verbose = verbose;
		this.timeout = timeout;
		this.duration = duration;
		this.workerList = new Vector<Logger>();
		this.fellowships = new String[NUM_FELLOWSHIPS][RING_SIZE];
	}
	
	/* Loops forever, accepting connections and dispatching to workers */
	public void run() {
		ObjectInputStream ois;
		Socket socket = new Socket();
		
		new Thread(new Timer(new TerminateAction(), duration*MS_TO_SEC)).start();
		try {
			server = new ServerSocket(WHISPER_PORT);
		} catch (IOException e) {
			System.err.println("Error: Could not listen on port. Terminating...");
			System.exit(1);
		}
		System.out.println("Internode communication server established.");
		
		// TODO wake up fellowships
		
		while(true) {
			try {
				socket = server.accept();
				ois = new ObjectInputStream(socket.getInputStream());
				int fellowshipID = (Integer)ois.readObject();
				fellowships[fellowshipID] = (String[])ois.readObject();
				while(ois.available() > 0) {
					nodeList.add((Node)ois.readObject());
					nodeList.notify();
				}
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			} catch (ClassCastException e) {}
		}
	}
	
	/* ************ HELPER METHODS ************ */

	public void print() {
		for (Node node : nodeList)
			System.out.println(node.toString());
	}
	
	public void wakeFellowship() {
		
	}
	
	/* ************ EMBEDDED THREADS ************ */
	public class Logger implements Runnable {
		public void run() {
			while(true) {
				if (nodeList.isEmpty()) {
					try {
						nodeList.wait();
					} catch (InterruptedException e) { continue; }
				}
				// TODO log info
				System.out.println(nodeList.remove(FRONT).toString());
			}
		}
	}
	
	private class TerminateAction implements Action {
		public void execute() {
			print();
			System.exit(0);
		}
	}
	
	private class FellowshipAWOLAction implements Action {
		int fellowshipID;
		
		public FellowshipAWOLAction(int fellowshipID) {
			this.fellowshipID = fellowshipID;
		}
		
		public void execute() {
			// TODO fellowship timer has expired, request dump
		}
	}
}
