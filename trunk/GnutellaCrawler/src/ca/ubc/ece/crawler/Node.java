/*	Name: node.java
 * 		- node object to store information about crawled nodes
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 
 * 	Last updated: November 17, 2010
 */

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
	
	public boolean containedIn(Vector<Node> list) {
		for (Node other : list) {
			if (this.equals(other))	{
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		String ret = "Address: " + this.address + "\n" +
		
		"Port: " + this.portNum + "\r\n" + 
		"Status: " + this.info.getStatus() + "\r\n";
		if (this.info.getStatus() == Crawler.Status.CONNECTED) {
			ret += "Hostname: " + this.info.getHostname() + "\r\n" +
				"Agent: " + this.info.getAgent() + "\r\n";
		}
		
		return ret;
	}

}
