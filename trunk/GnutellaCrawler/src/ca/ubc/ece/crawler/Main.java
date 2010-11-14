package ca.ubc.ece.crawler;

import java.io.*;
import java.util.*;

public class Main {
	private static final String OUTPUT_FILE = "results.txt";
	private static final String DELIM = ",:";
	private static final int FRONT = 0;
	private static final int MILLI_TO_MIN = 60000;
	private static final int MS_TO_SEC = 1000;
	
	private int num_discovered = 0;
	private int num_timeouts = 0;
	private static double avgNumOfFiles = 0;
	private static long totalNumOfFiles = 0;
	private static int smallestFile = 999999;
	private static int largestFile = 0;
	private static double avgFileSize = 0;
	private static long totalFileSize = 0;
	private static int maxNumOfFiles = 0;
	private static int TimedOutNodes, SuccessfulCrawl, UnroutableIP, ConnectionRefused, InternalError, UnabletoSend, DidNotReceive, FoundNotCrawled;
	private static int timeout = 30;
	private static int duration = -1;
	private static long startTime = 0;
	private static boolean full = false;
	
    public static void main(String[] args) {
        Crawler mainCrawler = new Crawler();
		Extension extInfo = new Extension();
        startTime = Calendar.getInstance().getTimeInMillis()/MILLI_TO_MIN;
        ArrayList<Node> visited = new ArrayList<Node>();
        ArrayList<Node> unvisited = new ArrayList<Node>();
        
        /* Parse command-line bootstrap parameters */
        if (args.length < 2){
            System.out.println("Error: incorrect inputs\nUsage:\n\tMain <Node-address> <node-port>");
            return;
        } else {
        	for (int i = 0; i < args.length; i++) {
        		if (args[i].equals("-full")) {
        			System.out.println("Full output mode set");
        			full = true;
        		} else if (args[i].equals("-minimal")) {
        			System.out.println("Minimal output mode set");
        			full = false;
        		} else if (args[i].startsWith("timeout=")) {
        			String[] arg = args[i].split("=");
        			timeout = Integer.parseInt(arg[1])*MS_TO_SEC;
        			System.out.println("Connection timeout set for " + timeout/MS_TO_SEC + " second(s)");
        		} else if (args[i].indexOf(":") != -1) {
        			String[] arg = args[i].split(":");
        			unvisited.add(new Node(arg[0], Integer.parseInt(arg[1])));
        		} else {
        			duration = Integer.parseInt(args[i]);
        			System.out.println("Execution time set for " + duration + " minute(s)");
        		}
        	}
        }
        
        /* Initiate crawling */

        while(unvisited.size() != 0) {	
        	CrawlResult info = null;
        	if(checkTime())
        		calcStats(visited, unvisited, Calendar.getInstance().getTimeInMillis()/MILLI_TO_MIN, extInfo, info);
        	info = mainCrawler.crawl(unvisited.get(FRONT).address, unvisited.get(FRONT).portNum, timeout, full);
		    visited.add(unvisited.get(FRONT));
		    unvisited.remove(FRONT);
		    print(info, extInfo);
		    updateData(info);
		    
		    /* Get info from each leaf node but do not traverse its nodes yet */
		    String leaves;
		    try {
		    	leaves = info.getLeaves();
		    } catch (NullPointerException e) {
		    	// if unable to get info we likely timed out, ignore this node
		    	continue;
		    }
		    
		    StringTokenizer tokens = new StringTokenizer(leaves, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
		    	if(checkTime()) 
		    		calcStats(visited, unvisited, Calendar.getInstance().getTimeInMillis()/MILLI_TO_MIN, extInfo, info);
		    	
			    Node leaf = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    
			    // ignore this node if we have already visited it, otherwise get its information
			    if (contained(visited, leaf)) // leaf nodes will never be contained in unvisited (unless leaves have leaves...)
			    	continue;
			    	
		    	CrawlResult leafInfo = null;
		    	
		    	leafInfo = mainCrawler.crawl(leaf.address, leaf.portNum, timeout, full);
		    	print(leafInfo, extInfo);
		    	updateData(leafInfo);
		    	
		    	String leafPeers;
		    	try {
		    		leafPeers = leafInfo.getUltrapeers();
		    	} catch (NullPointerException e) {
		    		// if unable to get info we likely timed out, ignore this node
		    		continue;
		    	}
		    	
		    	StringTokenizer leafTokens = new StringTokenizer(leafPeers, DELIM);
		    	
		    	/* Add unknown ultrapeers of this leaf to our list */
		    	while (leafTokens.hasMoreTokens()) {
		    		Node leafNode = new Node(leafTokens.nextToken(), Integer.parseInt(leafTokens.nextToken()));
		    		if (!contained(unvisited, leafNode) && !contained(visited, leafNode))
		    			unvisited.add(leafNode);
		    	}
		    	// we are done with this leaf
		    	visited.add(leaf);
		    }
		    
		    /* Add new ultrapeers from this ultrapeer to our unvisited list */
		    String peers = info.getUltrapeers();
		    tokens = new StringTokenizer(peers, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
			    Node ultra = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    
			    if (!contained(unvisited, ultra) && !contained(visited, ultra))
			    	unvisited.add(ultra);
		    }
		    
		    // move this ultrapeer to visited
		    visited.add(unvisited.get(FRONT));
		    unvisited.remove(FRONT);
		    
		    // repeat for next ultrapeer in list
        }
        
    }
    
    private static boolean contained(ArrayList<Node> list, Node node) {
    	// returns true if the node is in the list
    	for (int i = 0; i < list.size(); i++) {
    		if (list.get(i).address == node.address && list.get(i).portNum == node.portNum)
    			return (true);
    	}
    	return (false);
    }
    
    private static void print(CrawlResult info, Extension extInfo) {
    	// output info to text file
    	if (full == true) {
	    	PrintWriter out;
	    	try {
	        	out = new PrintWriter(new BufferedWriter(new FileWriter(OUTPUT_FILE, true)));
	        	if (info != null) 
	            	out.write(info.toString()); 
	        } catch (IOException e) {
	        	System.err.println("Could not write to 'results.txt'");
	        }
    	}
        // output info to console
        if (info != null)
        	if(info.getStatus().equals("Connected"))
        		info.print();
        if (info.getNumOfFiles() > 0) {
    		extInfo.calcExt(info.getFilesList());
        }

    }
    private static void updateData(CrawlResult info){
    	updateStatus(info.getStatus());
    	if(full){
	    	if(info.getNumOfFiles() > maxNumOfFiles){
	    		maxNumOfFiles = info.getNumOfFiles();
	    	}
	    	totalNumOfFiles += info.getNumOfFiles(); 
	    	if(info.getMaximumFileSize() > largestFile){
	    		largestFile = info.getMaximumFileSize();
	    	}else if (info.getMinimumFileSize() < smallestFile){
	    		smallestFile = info.getMinimumFileSize();
	    	}
	    	totalFileSize += info.getTotalFileSize();
    	}
    }
    private static boolean checkTime() {
    	System.out.println("Crawler has been active for " + (float)((int)((Calendar.getInstance().getTimeInMillis()/(double)MILLI_TO_MIN - startTime)*100))/100 + " minute(s)");
    	if (duration != 0 && (Calendar.getInstance().getTimeInMillis()/MILLI_TO_MIN - startTime) >= duration) {
    		System.out.println("Execution duration reached, terminating...");
    		return true;
    	}
    	return false;
    }
    
    private static void updateStatus(String status){
    	if(status == "Connected"){
    		SuccessfulCrawl++;
    	}else if(status == "Unroutable IP"){
    		UnroutableIP++;
    	}else if(status == "Connection Refused"){
    		ConnectionRefused++;
    	}else if(status == "Internal Error"){
    		InternalError++;
    	}else if(status == "Connection Timeout"){
    		TimedOutNodes++;
    	}else if(status == "Connected but unable to send message"){
    		UnabletoSend++;
    	}else if(status == "Connected, message sent, failed to receive reply"){
    		DidNotReceive++;
    	}
    }
    private static void calcStats(ArrayList<Node> visited, ArrayList<Node> unvisited, long finaltime, Extension extInfo, CrawlResult info){
    	int num_nodes;
    	double nodesPerSecond;
    	long finalTimeSeconds = (finaltime - startTime) * 60;
		

    	num_nodes = visited.size() + unvisited.size();
    	nodesPerSecond = (double)num_nodes / (double)finalTimeSeconds;
    	FoundNotCrawled = unvisited.size();
    	avgNumOfFiles = totalNumOfFiles / SuccessfulCrawl;
    	System.out.println("Number of Nodes Discovered : " + num_nodes);
    	System.out.println("Nodes Discovered per Second : " + nodesPerSecond);
    	System.out.println( "Number of Successful Crawls : " + SuccessfulCrawl + "\r\n" +
    						"Number of TimeOuts : " + TimedOutNodes + "\r\n" +
    						"Number of Refused Connections : " + ConnectionRefused + "\r\n" + 
    						"Number of Internal Errors : " + InternalError + "\r\n" +
    						"Number of Unable to Send Request Errors : " + UnabletoSend + "\r\n" +
    						"Number of Failed to Receieve Reply Errors : " + DidNotReceive + "\r\n" +
    						"Number of Discovered Nodes but have not visited yet : " + FoundNotCrawled + "\r\n");
    	
    	if(full){
    		avgFileSize = (double)totalFileSize / (double)totalNumOfFiles;
    		avgNumOfFiles = (double)totalNumOfFiles / (double)SuccessfulCrawl;
    		String extensionTypes = extInfo.commonExt(); 
    		System.out.println( "Maximum Files on a node was : " + maxNumOfFiles + "\r\n" + 
    							"Average Files on all nodes was " + avgNumOfFiles + "\r\n" + 
    							"Smallest File found : " + smallestFile + "\r\n" +
    							"Largest File found : " + largestFile + "\r\n" +
    							"Average File size was : " + avgFileSize + "\r\n" +
    							extensionTypes);
    	}
    	System.exit(0);
    	
    }
}
