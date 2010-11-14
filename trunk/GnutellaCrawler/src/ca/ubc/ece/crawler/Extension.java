package ca.ubc.ece.crawler;

import java.util.Vector;

public class Extension {

	private Vector<String> sortedFiles;
	private String numFiles;
	private String[] results;
	private String[] tempResults;
	private Vector<String> nodeExt;
	private String[] finalResults;
	public void initExt() {
		sortedFiles = new Vector<String>(500, 100);
		numFiles = new String();
		results = new String[400];
		nodeExt = new Vector<String>(500, 100);
		finalResults = new String[1000];
		tempResults = new String[2];
	}
	
	/*public void getFiles(String files) {
		
		numFiles = files;
		
	}
	public void split() {
		results = numFiles.split("\0");
		
	}
	//display all files accumulated in the crawl
	public void displaytest(){
		for (int i = 0; i < results.length; i++){
			System.out.println(results[i]);
		}
	}
	*/
	//Calculate the extensions for the most current files.
	public void calcExt(String files)
	{
		String friend;
		numFiles = files;
		results = numFiles.split("\0");
		
		for (int i = 0; i < results.length; i++){
		results[i] = results[i].substring((results[i].length())-5);
		//System.out.println(results[i]);
		}
		for (int i = 0; i < results.length; i++){		
		tempResults = results[i].split("\\.");
		System.out.println(tempResults[0]);
		System.out.println(tempResults[1]);
		System.out.println(nodeExt.size());
		friend = tempResults[1];
		System.out.println(friend);
		nodeExt.addElement(tempResults[1]);
		}
	}
	
	public void commonExt()
	{		
	System.out.println("hi, i'm here");	
	}
	
	public String returnFiles() {
		
		return numFiles;
	}
	
	
}
