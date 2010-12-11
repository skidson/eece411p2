package ca.ubc.ece.crawler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

public class Master implements Runnable {
	public static final String DEFAULT_HOSTNAME = "localhost";
	public static final String DEFAULT_OUTPUT = "results.txt";
	public static final int DEFAULT_PORTNUM = 1337;
	public static final int MS_TO_SEC = 1000;
	
	private InetAddress hostName;
	private int portNum;
	
	private int dumpCount = 0;
	
	private static int timeout = 20;
	private static int duration = 30; 
	private static boolean verbose = false;
	private static boolean full = false;
	
	private Vector<Node> nodeList;
	private Vector<ResultHandler> workerList;
	private ServerSocket listener;
	
	/* ************ INITIALIZATION ************ */
	
	public static void main(String[] args) {
		String hostName = DEFAULT_HOSTNAME;
		int portNum = DEFAULT_PORTNUM;
		
		// Parse command-line bootstrap parameters
        if (args.length < 2){
            System.out.println("Usage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
            return;
        } else {
        	for (int i = 0; i < args.length; i++) {
        		if (args[i].equals("-full")) {
        			full = true;
        		} else if (args[i].equals("-minimal")) {
        			full = false;
        		} else if (args[i].equals("-v")) {
        			verbose = true;
        		} else if (args[i].startsWith("timeout=")) {
        			String[] arg = args[i].split("=");
        			timeout = Integer.parseInt(arg[1])*MS_TO_SEC;
        		} else if (args[i].indexOf(":") != -1) {
        			String[] arg = args[i].split(":");
        			hostName = arg[0];
        			portNum = Integer.parseInt(arg[1]);
        		} else {
        			duration = Integer.parseInt(args[i]);
        		}
        		if (full)
        			System.out.println("Output mode: full");
        		else
        			System.out.println("Output mode: minimal");
        		System.out.println("Verbose mode: set");
        		System.out.println("Connection timeout: " + timeout/MS_TO_SEC + " second(s)");
        		System.out.println("Execution time: " + duration + " minute(s)\n");
        	}
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
		this.workerList = new Vector<ResultHandler>();
	}
	
	/* Loops forever, accepting connections and dispatching to workers */
	public void run() {
		new Thread(new Timer()).start();
		while (true) {
			try {
				listener = new ServerSocket();
				while(true) {
					// Blocks until connection established
					Socket client = listener.accept();
					ResultHandler worker;
					try {
						 worker = new ResultHandler(client);
					} catch (Exception e) {
						// Memory limit reached, delegate to already existing worker
						worker = workerList.get(dumpCount % workerList.size());
					}
					workerList.add(worker);
					new Thread(worker).start();
				}
			} catch (IOException e) {
				System.err.println("Error: Could not listen on port");
			}
		}
	}
	
	/* ************ HELPER METHODS ************ */

	public void print() {
		for (Node node : nodeList)
			System.out.println(node.toString());
	}
	
	/* ************ EMBEDDED THREADS ************ */
	public class ResultHandler implements Runnable {
		private Vector<Socket> queue = new Vector<Socket>();
		ObjectInputStream ois;
		
		public ResultHandler(Socket client) {
			this.process(client);
		}

		public void run() {
			while(this.getLoadCount() > 0) {
				try {
					ois = new ObjectInputStream(queue.remove(0).getInputStream());
					while (ois.available() > 0) {
						Node node = (Node) ois.readObject();
						// TODO check if node already in list
						nodeList.add(node);
					}
				} catch (IOException e) { 
					continue;
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
			// No work left to do, terminate this worker
			workerList.remove(this);
		}
		
		public void process(Socket client) {
			queue.add(client);
		}
		
		public int getLoadCount() {
			return(queue.size());
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
			
			//Need to kill all crawlers
			print();
			System.exit(0);
		}
	}
}
