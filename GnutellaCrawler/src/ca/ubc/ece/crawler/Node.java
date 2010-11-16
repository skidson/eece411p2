package ca.ubc.ece.crawler;

import java.util.ArrayList;

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
		if (this.address.equals(other.getAddress()) && this.portNum == other.getPortNum())
			return true;
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
	
	public boolean containedIn(ArrayList<Node> list) {
		// determines if this node is contained in the list
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(this))
				return true;
		}
		return false;
	}
	
	public String toString() {
		String info = "Address: " + this.address + "\n" +
			"Port: " + this.portNum + "\n" + 
			"Status: " + this.info.getStatus() + "\n" +
			"Agent: " + this.info.getAgent() + "\n";
		
		return info;
	}

}
