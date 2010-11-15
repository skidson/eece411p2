package ca.ubc.ece.crawler;

import java.util.ArrayList;

public class Node {
	private String address;
	private int portNum;
	
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
		return(portNum);
	}
	
	public String getAddress() {
		return(address);
	}
	
	public boolean containedIn(ArrayList<Node> list) {
		// determines if this node is contained in the list
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(this))
				return true;
		}
		return false;
	}

}
