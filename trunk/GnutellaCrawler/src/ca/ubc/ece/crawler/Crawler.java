package ca.ubc.ece.crawler;

import java.io.*;
import java.net.*;

/**
 *
 * @author Samer Al-Kiswany
 */
public class Crawler {
    private CrawlResult cResult;
    private final int DEFAULT_TIMEOUT = 20;

    public Crawler() {
        cResult=new CrawlResult();
    }
    
    public CrawlResult crawl(String ipAddress, int port) {
    	int timeout = DEFAULT_TIMEOUT;
        System.out.println("Crawling " + ipAddress + ":" + port);
        String nodePeers = crawlPeers(ipAddress,port, timeout);
        if(nodePeers == null){
            System.out.println("Failed to crawl the peer");
            return (null);
        }
        if (nodePeers.length() > 0){
            parsePeers(nodePeers);            
        } else{
            System.out.println("Error : Recieved zero-length list of peers");
            return cResult;
        }        
        
        listFiles(ipAddress,port, timeout);
        
        return cResult;
    }
    
    public CrawlResult crawl(String ipAddress, int port, int timeout){
        System.out.println("Crawling " + ipAddress + ":" + port);
        String nodePeers = crawlPeers(ipAddress,port, timeout);
        if(nodePeers == null){
            System.out.println("Failed to crawl the peer");
            return (null);
        }
        if (nodePeers.length() > 0){
            parsePeers(nodePeers);            
        } else{
            System.out.println("Error : Recieved zero-length list of peers");
            return cResult;
        }        
        
        listFiles(ipAddress,port, timeout);
        
        return cResult;
    }
    
public CrawlResult crawl(String ipAddress, int port, int timeout, boolean full){
        System.out.println("Crawling " + ipAddress + ":" + port);
        String nodePeers = crawlPeers(ipAddress,port, timeout);
        if(nodePeers == null){
            System.out.println("Failed to crawl the peer");
            return (null);
        }
        if (nodePeers.length() > 0){
            parsePeers(nodePeers);            
        } else{
            System.out.println("Error : Recieved zero-length list of peers");
            return cResult;
        }        
        
        if(full = true)
        	listFiles(ipAddress,port, timeout);
        
        return cResult;
    }
    
    private String crawlPeers(String ipAddress, int port, int timeout){
        InputStream in = null;
        OutputStream out = null;
        Socket socket = null;
        
        try {
            socket = new Socket(ipAddress, port);
            socket.setSoTimeout(timeout*1000);
            System.out.println("Connected to node : " + ipAddress + " on port " + port);
            cResult.setStatus("Connected");
            in = socket.getInputStream();
            out = socket.getOutputStream();
    	}catch (UnknownHostException ex) {
            System.out.println("Error: Failed to connect to node "+ipAddress+":"+port);
            cResult.setStatus("Unroutable IP address");
            ex.printStackTrace();
            return (null);
        } catch (SocketTimeoutException ex){
        	System.out.println("Timed out while connecting to node");
        	cResult.setStatus("Connection Timeout");
        	return (null);
        }catch (IOException ex) {
            System.out.println("Error: Failed to connect to node "+ipAddress+":"+port);
            cResult.setStatus("Random IOexception lul");
            ex.printStackTrace();
            return (null);
        }
        String GNodetName = socket.getInetAddress().getHostName();
        System.out.println("Host name is : " + GNodetName);
        String response  = new String();
        StringBuffer request = new StringBuffer();
        request.append("GNUTELLA CONNECT/0.6\r\n" +
                        "User-Agent: UBCECE (carwl)\r\n" +
                        "Query-Routing: 0.2\r\n"+
                        "X-Ultrapeer: False\r\n"+
                        "Crawler: 0.1\r\n" +
                        "\r\n");
   
        byte[] bytes = request.toString().getBytes();
        
        try {
            out.write(bytes);
            out.flush();
        } catch (IOException ex) {
            System.out.println("Error: Failed to send the crawl message to " + ipAddress + ":" + port);
            cResult.setStatus("Connected but unable to send message");
            ex.printStackTrace();
            return null;
        }

        String responseLine=new String();
        while (true){
            try {
                responseLine = ByteOrder.readLine(in);
            } catch (IOException ex) {
                System.out.println("Error: Failed to recieve the responce from the node " + ipAddress + ":" + port);
                cResult.setStatus("Connected, message sent, failed to receive reply");
                ex.printStackTrace();
                return null;
            }
            
            // reached the end of the response
            if (responseLine.length()<2)
                break;
            
            response+=responseLine;
            response+="\n";
        }

        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return response;
    }
    
    private void parsePeers(String topoCrawlResult){
        int beginIndex;
        int endIndex;
        String upeers = new String();
        String leaves = new String();
        
        beginIndex = topoCrawlResult.indexOf("Peers:");
        endIndex = topoCrawlResult.indexOf("\n",beginIndex);
        upeers = topoCrawlResult.substring(beginIndex+6,endIndex);
        upeers = upeers.trim();
        
        beginIndex = topoCrawlResult.indexOf("Leaves:");
        endIndex = topoCrawlResult.indexOf("\n", beginIndex);
        leaves = topoCrawlResult.substring(beginIndex+7,endIndex);
        leaves = leaves.trim();        
        
        cResult.setUltrapeers(upeers);
        cResult.setLeaves(leaves);
    }
   

   private void listFiles(String ipAddress, int port, int timeout){
        InputStream in;
        OutputStream out;
        Socket socket;
        
        try {
            socket = new Socket(ipAddress, port);
            socket.setSoTimeout(timeout*1000);
            System.out.println("Connected to node : " + ipAddress + " on port " + port);

            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (UnknownHostException ex) {
            System.out.println("Error: Failed to connect to node " + ipAddress + ":" + port);
            cResult.setStatus("Unroutable IP address");
            ex.printStackTrace();
            return;
        } catch (IOException ex) {
            System.out.println("Error: Failed to connect to node " + ipAddress + ":" + port);
            cResult.setStatus("Port Failure O_o");
            ex.printStackTrace();
            return;
        }
        
        String response = new String();
        StringBuffer strBuf = new StringBuffer();
        
        String localAddress = socket.getLocalAddress().getHostAddress();
        String localPort = String.format("%d",socket.getLocalPort());
        
        strBuf.append("GET / HTTP/1.1\r\n" +
        		"Host: "+localAddress+"\r\n" +
        		"User-Agent: UBCECE/0.1\r\n" +
        		"Accept: text/html, application/x-gnutella-packets\r\n" +
        		"Connection: close\r\n" +
        		"\r\n");
        
        byte[] bytes = strBuf.toString().getBytes();
        
        try {
            out.write(bytes);
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        int parsingStage = 0;
        int headerEndIndex = 0;
        
        final int bufferSize = 5000; 
        byte[] buffer=new byte[bufferSize];
        int numOfBytesRead=0;
        int endIndex = 0;
        int moreAvailableInStream = 1;
        int qHitSize = 0;
        
        while (true){
            try {
                numOfBytesRead = ByteOrder.readBufferWithLimit(in , buffer, endIndex, bufferSize-endIndex);

                moreAvailableInStream = in.available();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            if(numOfBytesRead > 0)
                endIndex += numOfBytesRead;
            
            if (moreAvailableInStream <= 0 && endIndex <=0)
                break;                
            
            if(parsingStage == 0) {
                    headerEndIndex = findEndOfHeader(buffer);
                    if (headerEndIndex > 0) {
                        parseFilesListHeader(buffer, headerEndIndex);
                        endIndex -= headerEndIndex;
                        trimBufferBegining(buffer, headerEndIndex);
                        parsingStage = 1;
                    }
            }
            
            if(parsingStage == 1) {
                    if (endIndex <23)
                        continue;
                    
                    qHitSize = ByteOrder.leb2int(subBuffer(buffer,19,23),0);
                    
                    if(endIndex < qHitSize )
                        continue;
                    
                    byte[] qhit=subBuffer(buffer, 23,qHitSize+23);
                    processQueryHit(qhit);
                    endIndex -= (qHitSize + 23);
                    trimBufferBegining(buffer, qHitSize+23);
            }
        }

        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void parseFilesListHeader(byte[] flist, int headerEndIndex){

        String header = new String(subBuffer(flist, 0, headerEndIndex));
        int strBegin = header.indexOf("Server:");
        int strEnd = header.indexOf('\n', strBegin);
        header = header.substring(strBegin+8, strEnd);
        header.trim();

        strBegin=header.indexOf('/');
        if (strBegin == -1) 
            strBegin = header.indexOf(' ');
        
        header = header.substring(0,strBegin);
        cResult.setAgent(header);
           
    }

    private void processQueryHit(byte[] qhit){
        int beginIndex;
        int endIndex;
        int numOfFiles = qhit[0];
        String sentList = new String();
        String filesList = new String();
        String msg = new String();

        qhit = subBuffer(qhit,11,qhit.length);
        msg = new String(qhit);

        byte[] fname = new byte[512];
        int num = 0;
        while (qhit.length > 0 && num < numOfFiles){

            qhit=subBuffer(qhit, 8, qhit.length);         
            endIndex=bufferIndexOf(qhit, '\0');
            fname=subBuffer(qhit, 0, endIndex);
            qhit=subBuffer(qhit, endIndex+1, qhit.length);
            msg=new String(fname);
            filesList += msg;
            filesList += '\0';
                        
            endIndex = bufferIndexOf(qhit, '\0');
            qhit = subBuffer(qhit, endIndex+1, qhit.length);
            num++;
            if (qhit == null)
                break;
        }
        
        cResult.addNumOfFiles(numOfFiles);
        cResult.addFilesList(filesList);
    
    }
    
    private int findEndOfHeader(byte[] packet){

        for (int i = 0; i <= packet.length-4; i++){
            if (packet[i] == '\r' && packet[i+1] == '\n' && packet[i+2] == '\r' && packet[i+3] == '\n')
                return i+4;
        }
        return (-1);
    }
    
    private byte[] subBuffer(byte[] buffer,int beginIndex,int endIndex){
        
        if (beginIndex>buffer.length)
            return null;
            
        if (endIndex > buffer.length)
            endIndex = buffer.length;
        
        int bLen = endIndex-beginIndex;
        byte[] result=new byte[bLen];
     
        System.arraycopy(buffer,beginIndex,result,0,bLen);
        
        return result;
    }

    private void trimBufferBegining(byte[] buffer, int trimBeginIndex){
        
        if (trimBeginIndex > buffer.length)
            return;

        for (int i=trimBeginIndex ; i<buffer.length ; i++)
            buffer[i-trimBeginIndex] = buffer[i];
        
    }
    
    private int bufferIndexOf(byte[] buffer,char c){
        for (int i=0;i<buffer.length;i++){
            if (buffer[i]==c)
                return i;
        }
        return (-1);
    }
    
}
