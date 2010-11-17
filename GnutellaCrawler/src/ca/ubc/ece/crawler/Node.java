package ca.ubc.ece.crawler;

import java.util.Vector;

public class Node {
	private String address;
	private int portNum;
	
	private CrawlResult info;
	
	public Node(String address, int portNum) {
		this.address = address;
		this.portNum = portNum;
		this.info = null;
	}
	
	public boolean equals(Node other) {
		//System.err.println("this: " + this.address + " other: " + other.getAddress());
		if (this.address.equals(other.getAddress()) && this.portNum == other.getPortNum()) {
			//System.err.println("EQUAL");
			return true;
		}
		//System.err.println("NOT EQUAL");
		return false;
	}
	
	public int getPortNum() {
		return(portNum);
	}
	
	public String getAddress() {
		return(address);
	}
	
	public void setInfo(CrawlResult info) {
		this.info = info;
	}
	
	public CrawlResult getInfo() {
		return info;
	}
	
	public boolean containedIn(Vector<Node> list) {
		// determines if this node is contained in the list
		for (Node other : list) {
			if (this.equals(other))	{
				System.err.println(other.getAddress() + " was in the list already");
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String ret = "Address: " + this.address + "\n" +
		
		"Port: " + this.portNum + "\n" + 
		"Status: " + this.info.getStatus() + "\n";
		if (this.info.getStatus() == Crawler.Status.CONNECTED) {
			ret += "Hostname: " + this.info.getHostname() + "\n" +
				"Agent: " + this.info.getAgent() + "\n";
		}
		
		return ret;
	}

}
