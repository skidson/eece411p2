/*	Name: CrawlResult.java
 * 		- storage object for node information
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 				(original by Samer Al-Kiswany)
 * 				
 * 	Last updated: November 17, 2010
 */

package ca.ubc.ece.crawler;

import ca.ubc.ece.crawler.Crawler.Status;

public class CrawlResult {
    private String ultrapeers;
    private String leaves;
    private String filesList;
    private String Agent;
    private String hostname;
    private int numOfFiles;

    private int minimumFileSize;
    private int maximumFileSize;
    private long totalFileSize; //File Size in bytes
    
    private Status status;
    
    /** Creates a new instance of CrawlResults */
    public CrawlResult() {
        ultrapeers = new String("N/A");
        leaves = new String("N/A");
        filesList = new String("N/A"); 
        Agent = new String("N/A");
        hostname = new String("N/A");
        numOfFiles = 0;
        minimumFileSize = -1;
        maximumFileSize = 0;
    }
    
    public int getMinimumFileSize(){
    	return minimumFileSize;
    }
    
    public void setMinimumFileSize(int fileSize){
    	minimumFileSize = fileSize;
    }
    
    public long getTotalFileSize(){
    	return totalFileSize;
    }
    
    public int getMaximumFileSize(){
    	return maximumFileSize;
    }
    
    public String getHostname() {
    	if (hostname == null) 
    		return ("N/A");
    	return hostname;
    }
    
    public void setHostname(String hostname) {
    	this.hostname = hostname;
    }
    
    public void setMaximumFileSize(int fileSize){
    	maximumFileSize = fileSize;
    }
    public void setUltrapeers(String upeers) {
        ultrapeers = upeers;
    }
    
    public void setStatus(Status status){
    	this.status = status;
    }
    
    public void addtotalFileSize(int fileSize){
    	totalFileSize += fileSize;
    }
    
    public Crawler.Status getStatus() {
    	return status;
    }
    
    public String getStatusMessage(){
    	switch(status) {
    	case CONNECTED:
    		return("Connected");
    	case UNROUTABLE:
    		return("Unroutable IP");
    	case REFUSED:
    		return("Connection Refused");
    	case INTERNAL:
    		return("Internal Error");
    	case TIMEOUT:
    		return("Connection Timeout");
    	case MUTE:
    		return("Connected but unable to send message");
    	case NOREPLY:
    		return("Connected, message sent, failed to recieve reply");
    	case SHIELDED:
    		return("Connected, message sent, but information was shielded");
    	}
    	return(" - ");
    }
    
    public String getUltrapeers() {
    	if (ultrapeers == null) 
    		return ("N/A");
        return ultrapeers;
    }
    
    public void setLeaves(String lvs) {
        leaves = lvs;
    }
    
    public String getLeaves() {
    	if (leaves == null) 
    		return ("N/A");
        return leaves;
    }
    
    public void addNumOfFiles(int num) {
        numOfFiles += num;
    }
    
    public int getNumOfFiles(){
        return numOfFiles;
    }
    
    public void addFilesList(String flist) {
        filesList += flist;
    }
    
    public String getFilesList() {
    	if (filesList == null) 
    		return ("N/A");
        return filesList;
    }
    
    public void setAgent(String agnt) {
        Agent = agnt;
    }
    
    public String getAgent() {
    	if (Agent == null) 
    		return ("N/A");
        return Agent;
    }
    
    public void print() {
    	System.out.println("Status : " + getStatus());
        System.out.println("Running Agent : " + getAgent());
        System.out.println("UltraPeers : " + getUltrapeers());
        System.out.println("Leaves : " + getLeaves());
        System.out.println("Number of files: " + getNumOfFiles());
    }
    
    public String toString() {
    	String stats = "Running Agent : " + getAgent() + "\n\r" +
        	"UltraPeers : " + getUltrapeers().replace(",", ", ") + "\n\r" +
        	"Leaves : " + getLeaves().replace(",", ", ") + "\n\r" +
        	"Number of files : " + getNumOfFiles();
        return stats;
    }
}
