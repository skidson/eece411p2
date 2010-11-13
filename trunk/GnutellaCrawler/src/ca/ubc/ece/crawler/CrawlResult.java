package ca.ubc.ece.crawler;
/**
 *
 * @author Samer Al-Kiswany
 */
public class CrawlResult {
    private String ultrapeers;
    private String leaves;
    private int numOfFiles;
    private String filesList;
    private String Agent;
    private String status;
    
    /** Creates a new instance of CrawlResults */
    public CrawlResult() {
        ultrapeers = new String();
        leaves = new String();
        numOfFiles = 0;
        filesList = new String(); 
        Agent = new String();
    }
    
    public void setUltrapeers(String upeers) {
        ultrapeers = upeers;
    }
    public void setStatus(String status){
    	this.status = status;
    }
    public String getStatus(){
    	return status;
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
        	"UltraPeers : " + getUltrapeers() + "\n\r" +
        	"Leaves : " + getLeaves() + "\n\r" +
        	"Number of files : " + getNumOfFiles();
        		
        if(getNumOfFiles() > 0)
            stats += "List of files : " + getFilesList().replace("\0","\n\r\t");
        
        return stats;
    }
}
