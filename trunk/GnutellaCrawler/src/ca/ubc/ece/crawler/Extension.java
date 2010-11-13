package ca.ubc.ece.crawler;

import java.util.Vector;

public class Extension {

	private Vector<String> sortedFiles;
	private String numFiles;
	private String[] results;
	private String[] tempResults;
	private String[] finalResults;
	public void initExt() {
		sortedFiles = new Vector<String>(500, 100);
		numFiles = new String();
		results = new String[500];
		finalResults = new String[1000];
		tempResults = new String[2];
	}
	
	public void getFiles(String files) {
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
	public void calcExt()
	{
		for (int i = 0; i < results.length; i++){
		results[i] = results[i].substring((results[i].length())-5);
		//System.out.println(results[i]);
		}
		for (int i = 0; i < results.length; i++){		
			//for (int j = 0; j < 3; j++)
			//{
			//	tempResults[j].compareTo("0");
			//}
		tempResults = results[i].split(".");
		//if (!(tempResults[2].equals("0"))) {
		//	finalResults[i] = tempResults[2];
		//} else {
			//finalResults[i] = tempResults[1];
		//}
			System.out.println(tempResults[0]);
			System.out.println(tempResults[1]);
		}
		
	}
	public String returnFiles() {
		
		return numFiles;
	}
	
	
}
