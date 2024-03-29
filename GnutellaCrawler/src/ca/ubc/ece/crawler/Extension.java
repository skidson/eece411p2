/*	Name: Extension.java
 * 		- extension object to store information about discovered extensions
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 				
 * 	Last updated: November 17, 2010
 */

package ca.ubc.ece.crawler;

import java.util.Vector;

// Represents an extension type specified in name
public class Extension {
	private String name;
	private int count;
	
	public Extension(String name) {
		this.name = name;
		count = 1;
	}
	
	public static String findExtension(String filename) {
		String[] temp = filename.split("\\.");
		return(temp[temp.length - 1]);
	}
	
	public String getName() {
		return(name);
	}
	
	public int getCount() {
		return(count);
	}
	
	public void increment() {
		this.count++;
	}
	
	public boolean equals (Extension other) {
		if (this.name.equalsIgnoreCase(other.getName()))
			return true;
		return false;
	}
	
	public String toString() {
		return(name);
	}
	
	public boolean containedIn(Vector<Extension> list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(this)) {
				list.get(i).increment();
				return true;
			}
		}
		return false;
	}
}
