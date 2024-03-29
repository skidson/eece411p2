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
	private int portNum;
	private String address;
	private String hostName;
	private String agent;
	private String Peers;
	private String Leaves;
	
	
	
	private Status status;
	private byte[] data;

	public Node(String address, int portNum) {
		this.address = address;
		this.portNum = portNum;
	}
	
	public boolean containedIn(Vector<Node> list) {
		for (Node other : list) {
			if (this.equals(other))
				return true;
		}
		return false;
	}
	
	public boolean equals(Node other) {
		if (this.address.equals(other.getAddress()) && this.portNum == other.getPortNum())
			return true;
		return false;
	}
	
	/* ************** GETTERS ************** */
	public String getAddress() { return address; }
	
	public String getHostName() { return hostName; }

	public int getPortNum() { return portNum; }
	
	public byte[] getData() { return data; }

	public Status getStatus() throws NotParsedException { return this.status; }
	
	public String getAgent() throws NotParsedException { return agent; }
	
	public String getPeers() {return Peers; }
	
	public String getLeaves() { return Leaves; }
	
	/* ************** SETTERS ************** */
	public void setAddress(String address) {
		this.address = address;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setPortNum(int portNum) {
		this.portNum = portNum;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	public void setLeaves(String leaves){
		this.Leaves = leaves;
	}
	public void setPeers(String peers){
		this.Peers = peers;
	}
	
	
	public String toString() {
		String ret = "Address: " + this.address + "\n" +
			"Port: " + this.portNum + "\r\n" + 
			"Status: " + this.status + "\r\n";
		if (this.status == Status.CONNECTED) {
			ret += "Hostname: " + this.hostName + "\r\n" +
				"Agent: " + this.agent + "\r\n";
		}
		return ret;
	}

}
