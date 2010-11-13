package ca.ubc.ece.crawler;

import java.io.*;
import java.util.*;

public class Main {
	private static final String OUTPUT_FILE = "results.txt";
	private static final String DELIM = ",:";
	private static final int FRONT = 0;
    
    public static void main(String[] args) {
        Crawler mainCrawler = new Crawler();
        
        ArrayList<Node> visited = new ArrayList<Node>();
        ArrayList<Node> unvisited = new ArrayList<Node>();
        
        /* Parse command-line bootstrap parameters */
        if (args.length != 2){
            System.out.println("Error: incorrect inputs\nUsage:\n\tMain <Node-address> <node-port>");
            return;
        } else {
            unvisited.add(new Node(args[0], Integer.parseInt(args[1])));
        }
        
        /* Initiate crawling */
        while(unvisited.size() != 0) {
		    CrawlResult info = mainCrawler.crawl(unvisited.get(FRONT).address, unvisited.get(FRONT).portNum);
		    visited.add(unvisited.get(FRONT));
		    unvisited.remove(FRONT);
		    print(info);
		    
		    /* Get info from each leaf node but do not traverse its nodes yet */
		    String leaves = info.getLeaves();
		    StringTokenizer tokens = new StringTokenizer(leaves, DELIM);
		    
		    while (tokens.hasMoreTokens()) {
			    Node leaf = new Node(tokens.nextToken(), Integer.parseInt(tokens.nextToken()));
			    
			    // ignore this node if we have already visited it, otherwise get its information
			    if (contained(visited, leaf)) // leaf nodes will never be contained in unvisited (unless leaves have leaves...)
			    	continue;
			    	
		    	CrawlResult leafInfo = mainCrawler.crawl(leaf.address, leaf.portNum);
		    	print(leafInfo);
		    	String leafPeers = leafInfo.getUltrapeers();
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
