package ca.ubc.ece.crawler;

import ca.ubc.ece.crawler.Crawler.Status;

public class CrawlResult {
    private String ultrapeers;
    private String leaves;
    private int numOfFiles;
    private String filesList;
    private String Agent;
    //private String status;
    private int minimumFileSize;
    private int maximumFileSize;
    private long totalFileSize; //File Size in bytes
    
    private Status status;
    
    /** Creates a new instance of CrawlResults */
    public CrawlResult() {
        ultrapeers = new String();
        leaves = new String();
        numOfFiles = 0;
        filesList = new String(); 
        Agent = new String();
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
    	}
    	return(" - ");
    }
    
    public String getUltrapeers() {
        return ultrapeers;
    }
    
    public void setLeaves(String lvs) {
        leaves = lvs;
    }
    
    public String getLeaves() {
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
        return filesList;
    }
    
    public void setAgent(String agnt) {
        Agent = agnt;
    }
    
    public String getAgent() {
        return Agent;
    }
    
    public void print() {
    	System.out.println("Status : " + getStatus());
        System.out.println("Running Agent : " + getAgent());
        System.out.println("UltraPeers : " + getUltrapeers());
        System.out.println("Leaves : " + getLeaves());
        System.out.println("Number of files: " + getNumOfFiles());
        if(getNumOfFiles() > 0)
            System.out.println("list of files : " + getFilesList().replace("\0","\n\r"));
    }
    
    public String toString() {
    	String stats = "Running Agent : " + getAgent() + "\n\r" +
        	"UltraPeers : " + getUltrapeers().replace(",", ", ") + "\n\r" +
        	"Leaves : " + getLeaves().replace(",", ", ") + "\n\r" +
        	"Number of files : " + getNumOfFiles();

       if(getNumOfFiles() > 0)
            stats += "List of files :\n\t" + getFilesList().replace("\0","\n"); 
        if(getNumOfFiles() > 0);
        	stats += "List of files : " + getFilesList().replace("\0","\n\r\t");
        
        return stats;
    }
}
