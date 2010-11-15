package ca.ubc.ece.crawler;

import java.io.*;
import java.util.*;

public class Main {
	private static final String DELIM = ",:";
	private static final int FRONT = 0;
	private static final int MILLI_TO_MIN = 60000;
	private static final int MS_TO_SEC = 1000;
	
	private static Vector<Extension> extensions;
	
	private static long totalNumOfFiles = 0;
	private static int smallestFile = -1;
	private static int largestFile = 0;
	private static long totalFileSize = 0;
	private static int maxNumOfFiles = 0;
	private static int num_timeout, num_success, num_unroutable, num_refused, num_internal, num_mute, num_noreply;
	
	private static int timeout = 30;
	private static int duration = -1;
	private static long startTime = 0;
	private static boolean full = false;
	
    public static void main(String[] args) {
        Crawler mainCrawler = new Crawler();
        ArrayList<Node> visited = new ArrayList<Node>();
        ArrayList<Node> unvisited = new ArrayList<Node>();
        extensions = new Vector<Extension>();
        
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
        startTime = System.currentTimeMillis();
        while(unvisited.size() != 0) {	
        	CrawlResult info = null;
        	
        	if(checkTime())
        		print(visited, unvisited);
        	
        	info = mainCrawler.crawl(unvisited.get(FRONT), timeout, full);
		    visited.add(unvisited.get(FRONT));
		    unvisited.remove(FRONT);
		    
		    update(info);
		    
		    /* Get info from each leaf node but do not traverse its nodes yet */
		    String leaves = info.getLeaves();
    		if(leaves == null || checkShielded(leaves))
    			continue;
		    
		    StringTokenizer tokens = new StringTokenizer(leaves, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
		    	if(checkTime()) 
		    		print(visited, unvisited);
		    	
			    Node leaf = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    
			    // ignore this node if we have already visited it, otherwise get its information
			    if (leaf.containedIn(visited)) // leaf nodes will never be contained in unvisited (unless leaves have leaves...)
			    	continue;
			    	
		    	CrawlResult leafInfo = null;
		    	
		    	leafInfo = mainCrawler.crawl(leaf, timeout, full);
		    	update(info);
		    	
		    	String leafPeers = leafInfo.getUltrapeers();
	    		if(leafPeers == null || checkShielded(leafPeers)) {
	    			visited.add(leaf);
	    			continue;
	    		}

		    	
		    	StringTokenizer leafTokens = new StringTokenizer(leafPeers, DELIM);
		    	
		    	/* Add unknown ultrapeers of this leaf to our list */
		    	while (leafTokens.hasMoreTokens()) {
		    		Node leafNode = new Node(leafTokens.nextToken(), Integer.parseInt(leafTokens.nextToken()));
		    		if (!leafNode.containedIn(unvisited) && !leafNode.containedIn(visited))
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
		    try {
			    visited.add(unvisited.get(FRONT));
			    unvisited.remove(FRONT);
		    } catch (IndexOutOfBoundsException e) {
		    	// this current node is null, abort
		    	System.exit(1);
		    }
		    
		    // repeat for next ultrapeer in list
        }
        
    }
    
    private static boolean contained(ArrayList<Node> list, Node node) {
    	// returns true if the node is in the list
    	for (int i = 0; i < list.size(); i++) {
    		if (list.get(i).equals(node))
    			return (true);
    	}
    	return (false);
    }
    
    private static void update(CrawlResult info){
    	try {
	    	switch(info.getStatus()) {
	    	case CONNECTED:
	    		num_success++;
	    		break;
	    	case UNROUTABLE:
	    		num_unroutable++;
	    		break;
	    	case REFUSED:
	    		num_refused++;
	    		break;
	    	case INTERNAL:
	    		num_internal++;
	    		break;
	    	case TIMEOUT:
	    		num_timeout++;
	    		break;
	    	case MUTE:
	    		num_mute++;
	    		break;
	    	case NOREPLY:
	    		num_noreply++;
	    		break;
	    	}
	    	
	    	if (full) {
	    		parseExtensions(info.getFilesList());
		    	if(info.getNumOfFiles() > maxNumOfFiles)
		    		maxNumOfFiles = info.getNumOfFiles();
	
		    	totalNumOfFiles += info.getNumOfFiles(); 
		    	if (info.getMaximumFileSize() > largestFile)
		    		largestFile = info.getMaximumFileSize();
		    	else if (info.getMinimumFileSize() < smallestFile || smallestFile == -1)
		    		smallestFile = info.getMinimumFileSize();
		    	
		    	totalFileSize += info.getTotalFileSize();
	    	}
    	} catch (NullPointerException e){
    		// could not obtain node information, nothing to update
    	}
    }
    
    private static boolean checkTime() {
    	System.out.println("Crawler has been active for " + (float)(System.currentTimeMillis()- startTime)/(double)MILLI_TO_MIN + " minute(s)");
    	if (duration != 0 && (Calendar.getInstance().getTimeInMillis()/MILLI_TO_MIN - startTime) >= duration) {
    		System.out.println("Execution duration reached, terminating...");
    		return true;
    	}
    	return false;
    }
    
    private static boolean checkShielded(String info) {
    	if (info.equals("LLA/0.6 503 Shielded leaf node") || info.equals("LA/0.6 503 Shielded leaf node"))
    		return true;
    	return false;
    }
    
    private static void print(ArrayList<Node> visited, ArrayList<Node> unvisited){
    	System.out.println("Number of Nodes Discovered : " + visited.size() + unvisited.size());
    	System.out.println("Nodes Discovered per Second : " + (double)(visited.size() + unvisited.size()) /((float)(System.currentTimeMillis() - startTime)/(double) MILLI_TO_MIN * 60.0));
    	System.out.println( "Number of Successful Crawls : " + num_success + "\r\n" +
    						"Number of Timeouts : " + num_timeout + "\r\n" +
    						"Number of Refused Connections : " + num_refused + "\r\n" + 
    						"Number of Internal Errors : " + num_internal + "\r\n" +
    						"Number of Unable to Send Request Errors : " + num_mute + "\r\n" +
    						"Number of Failed to Receieve Reply Errors : " + num_noreply + "\r\n" +
    						"Number of Discovered Nodes but have not visited yet : " + unvisited.size() + "\r\n");
    	
    	if (full) {
    		System.out.println( "Maximum Files on a node was : " + maxNumOfFiles + "\r\n" + 
    							"Average Files on all nodes was " + ((double)totalNumOfFiles/(double)num_success) + "\r\n" + 
    							"Smallest File found : " + smallestFile + "\r\n" +
    							"Largest File found : " + largestFile + "\r\n" +
    							"Average File size was : " + totalFileSize/totalNumOfFiles + "\r\n");
    		extensions = sort(extensions);
    		System.out.println("File Extension \t\tNumber of Occurences");
    		for (int i = 0; i < 10; i++) {
    			try {
    				System.out.println("." + extensions.get(i).getName() + "\t\t" + extensions.get(i).getCount());
    			} catch (NullPointerException e) {
    				break;
    			}
    			
    		}
    	}
    	System.exit(0);
    	
    }
    
    private static Vector<Extension> sort(Vector<Extension> list) {
    	// sorts the list of file extensions by decreasing popularity
    	int index = 1;
    	while (list.get(index) != null) {
    		if (list.get(index-1) == null) {
    			index++;
    		} else if (list.get(index).getCount() > list.get(index-1).getCount()){
    			list.insertElementAt(list.get(index), index - 1);
    			list.removeElementAt(index + 1);
    			index--;
    		} else {
    			index++;
    		}
    	}
    	return list;
    }
    
    private static void parseExtensions(String filelist) {
    	String[] files = filelist.split("\0");
    	for (int i = 0; i < files.length; i++) {
    		Extension ext = new Extension(Extension.findExtension(files[i]));
    		for (int j = 0; j < extensions.size(); i++) {
    			if (extensions.get(i).equals(ext)) {
    				extensions.get(i).increment();
    				return;
    			}
    		}
    	}
    }
}
