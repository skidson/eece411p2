package ca.ubc.ece.crawler;

import java.util.Vector;

public class Extension {
//Constructor
	private String numFiles;
	private String[] results;
	private String[] tempResults;
	private Vector<String> nodeExt;
	private String[] finalResults;
	public Extension() {
		numFiles = new String();
		results = new String[400];
		nodeExt = new Vector<String>(500, 100);
		finalResults = new String[1000];
		
	}
	

	//Calculate the extensions for the most current files.
	public void calcExt(String files)
	{
		String[] tempResults = new String[5];
		numFiles = files;
		results = numFiles.split("\0");
		for (int i = 0; i < results.length; i++){
		results[i] = results[i].substring((results[i].length())-8);
		}
		for (int i = 0; i < results.length; i++){		
		tempResults = results[i].split("\\.");
			for(int j = tempResults.length; j >= 0; j--) {
				if (tempResults[tempResults.length-1] != null){
					nodeExt.add(tempResults[tempResults.length - 1]);
					//System.out.println(nodeExt.elementAt(nodeExt.size()-1));
					break;
				}								
			}
		}
	}
	//Takes data from all nodes(nodeExt), finds all different types and displays information.
	public String commonExt()
	{		
	Vector<String> extTypes = new Vector<String>(10,1);
	String Types;
	extTypes.add(nodeExt.elementAt(0));
	
	for (int i = 1; i < nodeExt.size(); i++)
	{
		if (!(extTypes.contains(nodeExt.elementAt(i)))) {
			extTypes.add(nodeExt.elementAt(i));
		}
	}
	Types = "Most Frequent File Extensions: ";
	for (int i = 0; i < extTypes.size(); i++)
	{
		Types = Types + extTypes.elementAt(i) + ", ";
	}
	return Types;
	}
	
	public String returnFiles() {
		
		return numFiles;
	}
	
	
}
