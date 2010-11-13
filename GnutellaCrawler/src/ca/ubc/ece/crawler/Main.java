package ca.ubc.ece.crawler;

import java.io.*;
import java.util.*;

public class Main {
	private static final String OUTPUT_FILE = "results.txt";
	private static final String DELIM = ",:";
	private static final int FRONT = 0;
    
    public static void main(String[] args) {
        int nodePort = 0;
        String nodeAddress = new String();
        Crawler mainCrawler = new Crawler();
        
        ArrayList<Node> visited = new ArrayList<Node>();
        ArrayList<Node> unvisited = new ArrayList<Node>();
        
        // parse parameters
        if (args.length != 2){
            System.out.println("Error: incorrect inputs\nUsage:\n\tMain <Node-address> <node-port>");
            return;
        } else {
            unvisited.add(new Node(args[0], Integer.parseInt(args[1])));
        }
        
        while(unvisited.size() != 0) {
		    CrawlResult info = mainCrawler.crawl(unvisited.get(FRONT).address, unvisited.get(FRONT).portNum);
		    visited.add(unvisited.get(FRONT));
		    unvisited.remove(FRONT);
		    print(info);
		    
		    /* Get info from each leaf node but do not traverse it's nodes yet */
		    String leaves = info.getLeaves();
		    StringTokenizer tokens = new StringTokenizer(leaves, DELIM);
		    while (tokens.hasMoreTokens()) {
			    Node leaf = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    
			    // ignore this node if we are already aware of it, otherwise add to back of list of nodes to visit
			    if (!contained(visited, leaf) && !contained(unvisited, leaf))
			    	unvisited.add(leaf);

		    }
		    
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
    
    private static void print(CrawlResult node) {
    	// output info to text file
    	PrintWriter out;
    	try {
        	out = new PrintWriter(new BufferedWriter(new FileWriter(OUTPUT_FILE, true)));
        	if (node != null) 
            	out.write(node.toString()); 
        } catch (IOException e) {
        	System.err.println("Could not write to 'results.txt'");
        }
        // output info to console
        if (node != null) 
        	node.print();
    }
    
}
