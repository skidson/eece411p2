/*	Name: node.java
 * 		- node object to store information about crawled nodes
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 
 * 	Last updated: December 17, 2010
 */

package ca.ubc.ece.crawler;

import java.io.Serializable;
import java.util.Vector;

public class Node implements Serializable {
	private String address;
	private String hostname;
	private String agent;
	private int portNum;
	private Status status;
	
	public Node(String address, int portNum) {
		this.address = address;
		this.portNum = portNum;
	}
	
	public boolean equals(Node other) {
		if (this.address.equals(other.getAddress()) && this.portNum == other.getPortNum())
			return true;
		return false;
	}
	
	public int getPortNum() {
		return portNum;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Status getStatus() {
		return this.status;
	}
	
	public boolean containedIn(Vector<Node> list) {
		for (Node other : list) {
			if (this.equals(other))
				return true;
		}
		return false;
	}
	
	public String toString() {
		String ret = "Address: " + this.address + "\n" +
			"Port: " + this.portNum + "\r\n" + 
			"Status: " + this.status + "\r\n";
		if (this.status == Status.CONNECTED) {
			ret += "Hostname: " + this.hostname + "\r\n" +
				"Agent: " + this.agent + "\r\n";
		}
		return ret;
	}

}
