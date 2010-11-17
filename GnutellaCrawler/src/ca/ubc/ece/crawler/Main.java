package ca.ubc.ece.crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Main {
	private static final String DELIM = ",:";
	private static final int FRONT = 0;
	private static final int MILLI_TO_MIN = 60000;
	private static final int MS_TO_SEC = 1000;
	
	private static Vector<Extension> extensions = new Vector<Extension>();
	
	private static long totalNumOfFiles = 0;
	private static int smallestFile = -1;
	private static int largestFile = 0;
	private static long totalFileSize = 0;
	private static int maxNumOfFiles = 0;
	private static int num_timeout, num_success, num_unroutable, num_refused, num_internal, num_mute, num_noreply, num_shielded;
	
	private static int timeout = 30;
	private static double duration = -1;
	private static long startTime = 0;
	private static boolean full = false;
	private static boolean verbose = false;
	
    public static void main(String[] args) {
        Vector<Node> visited = new Vector<Node>();
        Vector<Node> unvisited = new Vector<Node>();
        
        /* Parse command-line bootstrap parameters */
        if (args.length < 2){
            System.out.println("Usage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
            return;
        } else {
        	for (int i = 0; i < args.length; i++) {
        		if (args[i].equals("-full")) {
        			System.out.println("Full output mode set");
        			full = true;
        		} else if (args[i].equals("-minimal")) {
        			System.out.println("Minimal output mode set");
        			full = false;
        		} else if (args[i].equals("-v")) {
        			System.out.println("Verbose mode set");
        			verbose = true;
        		} else if (args[i].startsWith("timeout=")) {
        			String[] arg = args[i].split("=");
        			timeout = Integer.parseInt(arg[1])*MS_TO_SEC;
        			System.out.println("Connection timeout set for " + timeout/MS_TO_SEC + " second(s)");
        		} else if (args[i].indexOf(":") != -1) {
        			String[] arg = args[i].split(":");
        			try {
						unvisited.add(new Node(InetAddress.getByName(arg[0]).getHostAddress(), Integer.parseInt(arg[1])));
					} catch (NumberFormatException e) {
						System.out.println("\nUsage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
						return;
					} catch (UnknownHostException e) {
						System.out.println("\nUsage:\n\tMain <-full | -minimal> timeout=XX <address:port> <timetorun>");
						return;
					}
        		} else {
        			duration = Double.parseDouble(args[i]);
        			System.out.println("Execution time set for " + duration + " minute(s)\n");
        		}
        	}
        }
        
        /* Initiate crawling */
        startTime = System.currentTimeMillis();
        while(unvisited.size() != 0) {
        	CrawlResult info = null;
        	
        	if(checkTime())
        		print(visited, unvisited);
        	
        	info = Crawler.crawl(unvisited.get(FRONT), timeout, full);
        	
        	if (verbose) 
        		System.out.println("Inside ultrapeer " + unvisited.get(FRONT).getAddress() + ":");
        	
		    visited.add(unvisited.get(FRONT));
		    unvisited.get(FRONT).setInfo(info);
		    unvisited.removeElementAt(FRONT);
		    
		    update(info);
		    
		    /* ******************** START OF LEAF CHECKING ******************** */
		    /* Get info from each leaf node but do not traverse its nodes yet */
		    String leaves = info.getLeaves();
    		if(info.getStatus() != Crawler.Status.CONNECTED)
    			continue;
		    
		    StringTokenizer tokens = new StringTokenizer(leaves, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
		    	if(checkTime()) 
		    		print(visited, unvisited);
		    	
			    Node leaf = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    if (verbose) 
			    	System.out.println("Inside leaf " + leaf.getAddress() + ":");
			    // ignore this node if we have already visited it, otherwise get its information
			    if (leaf.containedIn(visited)) {// leaf nodes will never be contained in unvisited (unless leaves have leaves...)
			    	if (verbose) 
			    		System.out.println("Leaf " + leaf.getAddress() + " has already been visited, skipping...");
			    	continue;
			    }
			    
			    visited.add(leaf);
		    	CrawlResult leafInfo = Crawler.crawl(leaf, timeout, full);
		    	update(leafInfo);
		    	leaf.setInfo(leafInfo);
		    	
		    	String leafPeers = leafInfo.getUltrapeers();
	    		if(leaf.getInfo().getStatus() != Crawler.Status.CONNECTED)
	    			continue;

		    	StringTokenizer leafTokens = new StringTokenizer(leafPeers, DELIM);
		    	
		    	/* Add unknown ultrapeers of this leaf to our list */
		    	if (verbose) 
		    		System.out.println("Checking ultrapeers of " + leaf.getAddress() + "...");
		    	while (leafTokens.hasMoreTokens()) {
		    		Node ultrapeer = new Node(leafTokens.nextToken(), Integer.parseInt(leafTokens.nextToken()));
		    		if (verbose) 
		    			System.out.println("Ultrapeer: " + ultrapeer.getAddress() + ":" + ultrapeer.getPortNum());
		    		if (!ultrapeer.containedIn(unvisited) && !ultrapeer.containedIn(visited)) {
		    			if (verbose) 
		    				System.out.println("Adding : " + ultrapeer.getAddress() + " to unvisited");
		    			unvisited.add(ultrapeer);
		    		}
		    	}
		    	
		    	// we are done with this leaf
		    }
		    /* ******************** END OF LEAF CHECKING ******************** */
		    
		    /* Add new ultrapeers from this ultrapeer to our unvisited list */
		    String peers = info.getUltrapeers();
		    tokens = new StringTokenizer(peers, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
			    Node ultra = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    if (verbose) 
			    	System.out.println("Checking ultrapeer: " + ultra.getAddress() + ":" + ultra.getPortNum());
			    if (!ultra.containedIn(unvisited) && !ultra.containedIn(visited)) {
			    	if (verbose) 
			    		System.out.println("Adding : " + ultra.getAddress() + " to unvisited");
			    	unvisited.add(ultra);
			    }
		    }
		    
		    // repeat for next ultrapeer in list
        }
        
        // list of unvisited nodes exhausted, print statistics and terminate
        print(visited, unvisited);
        
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
	    	case SHIELDED:
	    		num_shielded++;
	    		break;
	    	}
	    	
	    	if (full && info.getStatus() == Crawler.Status.CONNECTED) {
	    		if (info.getNumOfFiles() > 0)
		    		parseExtensions(info.getFilesList());
		    	if(info.getNumOfFiles() > maxNumOfFiles)
		    		maxNumOfFiles = info.getNumOfFiles();
		    	
		    	totalNumOfFiles += info.getNumOfFiles(); 
		    	
		    	if (info.getMaximumFileSize() > largestFile)
		    		largestFile = info.getMaximumFileSize();
		    	if ((info.getMinimumFileSize() < smallestFile || smallestFile == -1) && info.getMinimumFileSize() != -1)
		    		smallestFile = info.getMinimumFileSize();
		    	
		    	totalFileSize += info.getTotalFileSize();
	    	}
    	} catch (NullPointerException e){
    		// could not obtain node information, nothing to update
    	}
    }
    
    private static boolean checkTime() {
    	float formattedTime = Round((float)(System.currentTimeMillis() - startTime)/(float)MILLI_TO_MIN, 2);
    	if (verbose) 
    		System.out.println(">>> Crawler has been active for " + formattedTime + " minute(s) <<<");
    	
    	if (duration != -1 && (formattedTime >= duration)) {
    		System.out.println("Execution duration reached, terminating...");
    		return true;
    	}
    	return false;
    }
    
    private static void print(Vector<Node> visited, Vector<Node> unvisited){
    	float formattedTime = Round((float)(System.currentTimeMillis() - startTime)/(float)MILLI_TO_MIN, 2);
    	formattedTime *= 60;
    	PrintWriter out = null;
    	String output = "";
    	
    	try {
    		out = new PrintWriter(new BufferedWriter(new FileWriter("results.txt")));
    	} catch (IOException e) {
    		System.err.println("Could not write to 'results.txt'");
    	}
    	
    	for(int i = 0; i < visited.size(); i++){
    		output += visited.get(i).toString() + "\r\n\n";
    	}
    	output += "\nNumber of Nodes Discovered : " + (visited.size() + unvisited.size()) + "\r\n" +
    		"\nNumber of Nodes Discovered : " + (visited.size() + unvisited.size()) + "\r\n" +
    		"Nodes Discovered per Second : " + Round(((visited.size() + unvisited.size())/formattedTime), 2) + "\r\n" +
    		"Number of Successful Crawls : " + num_success + "\r\n" +
			"Number of Timeouts : " + num_timeout + "\r\n" +
			"Number of Refused Connections : " + num_refused + "\r\n" +
			"Number of Shielded nodes: " + num_shielded + "\r\n" +
			"Number of Internal Errors : " + num_internal + "\r\n" +
			"Number of Unable to Send Request Errors : " + num_mute + "\r\n" +
			"Number of Failed to Receieve Reply Errors : " + num_noreply + "\r\n" +
			"Number of Discovered Nodes but have not visited yet : " + unvisited.size() + "\r\n";
    	
    	if (full && totalNumOfFiles != 0) {
    		output += "\n\nMaximum Files on a node was : " + maxNumOfFiles + "\r\n" + 
				"Average Files on all nodes was " + Round((float)totalNumOfFiles/(float)num_success,2) + "\r\n" + 
				"Smallest File found : " + smallestFile + "B\r\n" +
				"Largest File found : " + largestFile + "B\r\n" +
				"Average File size was : " + totalFileSize/totalNumOfFiles + "B\r\n" +
				"\r\nFile Extension \tNumber of Occurences\r\n";
    		extensions = steveSort(extensions);
    		for (int i = 0; i < extensions.size(); i++) {
				if (extensions.get(i).getName().length() < 5)
					output += "." + extensions.get(i).getName() + "\t\t\t" + extensions.get(i).getCount()+ "\r\n";
				else
					output += "." + extensions.get(i).getName() + "\t\t" + extensions.get(i).getCount() + "\r\n";
    		}
    	}
    	out.write(output);
    	out.close();
    	System.out.println(output);
    	System.exit(0);
    }
    
    private static Vector<Extension> steveSort(Vector<Extension> list) {
		Vector<Extension> temp = new Vector<Extension>();
		int cacheIndex = 0;
		int max = 0, num = 0;
		int maxIndex;
		
		while(!list.isEmpty()) {
			maxIndex = 0;
			max = 0;
			for(int i = (cacheIndex%list.size()); i < list.size(); i++) {
				num = list.get(i).getCount();
				if (num > max) {
					max = num;
					cacheIndex = maxIndex;
					maxIndex = i;
				}
			}
			temp.add(list.get(maxIndex));
			list.removeElementAt(maxIndex);
		}
		return(temp);
	}
    
    private static void parseExtensions(String filelist) {
    	String[] files = filelist.split("\0");
    	for (int i = 0; i < files.length; i++) {
    		Extension ext = new Extension(Extension.findExtension(files[i]));
    		if (!ext.containedIn(extensions))
    			extensions.add(ext);
    	}
    }
    
    public static float Round(float Rval, int Rpl) {
    	  float p = (float)Math.pow(10,Rpl);
    	  Rval = Rval * p;
    	  float tmp = Math.round(Rval);
    	  return (float)tmp/p;
    }
    	
}
