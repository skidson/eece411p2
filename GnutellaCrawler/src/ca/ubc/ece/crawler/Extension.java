package ca.ubc.ece.crawler;

// Represents an extension type specified in name
public class Extension {
	private String name;
	private int count;
	
	public Extension(String name) {
		this.name = name;
		count = 0;
	}
	
	public static String findExtension(String filename) {
		String[] temp = filename.split(".");
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
		if (this.name.equals(other.getName()))
			return true;
		return false;
	}
	
	public String toString() {
		return(name);
	}
}
