/*	Name: Crawler.java
 * 		- static set of functions to perform gnutella node crawling and return results
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 				(original by Samer Al-Kiswany)
 * 				
 * 	Last updated: November 17, 2010
 */

package ca.ubc.ece.crawler;

import java.io.*;
import java.net.*;

public class BlockingCrawler {
    private static CrawlResult cResult;
    
    public static CrawlResult crawl(Node node, int timeout, boolean full){
    	cResult = new CrawlResult();
        System.out.println("Crawling " + node.getAddress() + ":" + node.getPortNum() + "...");
        String nodePeers = crawlPeers(node.getAddress(), node.getPortNum(), timeout);
        
        if(nodePeers == null){
            System.out.println("Failed to crawl the peer\n");
            return cResult;
        }
        
        if (nodePeers.length() > 0){
            parsePeers(nodePeers);            
        } else{
            System.out.println("Error : Recieved zero-length peers");
            cResult.setStatus(Status.INTERNAL);
            return cResult;
        }        
        
        if(full == true)
        	listFiles(node.getAddress(), node.getPortNum(), timeout);
        
        return cResult;
    }
    
    private static String crawlPeers(String ipAddress, int port, int timeout){
        InputStream in = null;
        OutputStream out = null;
        Socket socket = new Socket();

        try {
        	InetSocketAddress address = new InetSocketAddress(ipAddress, port);
            socket.connect(address, timeout);
            System.out.println("Connected to node : " + ipAddress + " on port " + port);
            cResult.setStatus(Status.CONNECTED);
            in = socket.getInputStream();
            out = socket.getOutputStream();
    	} catch (SocketTimeoutException ex){
        	System.out.println("Timed out while connecting to node");
        	cResult.setStatus(Status.TIMEOUT);
        	return (null);
    	} catch (UnknownHostException ex) {
            System.out.println("Error: Failed to connect to node "+ipAddress + ":" + port);
            cResult.setStatus(Status.UNROUTABLE);
            return (null);
    	}catch (ConnectException ex) {
    		System.out.println("Error: Connection was refused by "+ipAddress+":"+port);
            cResult.setStatus(Status.REFUSED);
            return (null);
        } catch (IOException ex) {
            System.out.println("Error: Failed to connect to node "+ipAddress+":"+port);
            cResult.setStatus(Status.INTERNAL);
            return (null);
        }

        cResult.setHostname(socket.getInetAddress().getHostName());
        String response  = new String();
        StringBuffer request = new StringBuffer();
        request.append("GNUTELLA CONNECT/0.6\r\n" +
                        "User-Agent: UBCECE (crawl)\r\n" +
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
            cResult.setStatus(Status.MUTE);
            return null;
        }

        String responseLine = new String();
        while (true){
            try {
                responseLine = ByteOrder.readLine(in);
            } catch (IOException ex) {
                System.out.println("Error: Failed to recieve the response from the node " + ipAddress + ":" + port);
                cResult.setStatus(Status.NOREPLY);
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
            return null;
        }

        return response;
    }
    
    private static void parsePeers(String topoCrawlResult){
        int beginIndex;
        int endIndex;        
        String agent = new String();
        String upeers = new String();
        String leaves = new String();
        
        beginIndex = topoCrawlResult.indexOf("User-Agent:");
        endIndex = topoCrawlResult.indexOf("\n", beginIndex);
        agent = topoCrawlResult.substring(beginIndex+11, endIndex);
        
        beginIndex = topoCrawlResult.indexOf("Peers:");
        endIndex = topoCrawlResult.indexOf("\n",beginIndex);
        upeers = topoCrawlResult.substring(beginIndex+6,endIndex);
        upeers = upeers.trim();
        
        beginIndex = topoCrawlResult.indexOf("Leaves:");
        endIndex = topoCrawlResult.indexOf("\n", beginIndex);
        leaves = topoCrawlResult.substring(beginIndex+7,endIndex);
        leaves = leaves.trim();     
        
        if (upeers.equals("LLA/0.6 503 Shielded leaf node") || leaves.equals("LA/0.6 503 Shielded leaf node")) {
        	cResult.setStatus(Status.SHIELDED);
        } else {
	        cResult.setAgent(agent);
	        cResult.setUltrapeers(upeers);
	        cResult.setLeaves(leaves);
        }
    }
   

   private static void listFiles(String ipAddress, int port, int timeout){
        InputStream in;
        OutputStream out;
        Socket socket = new Socket();
        
        try {
        	InetSocketAddress address = new InetSocketAddress(ipAddress, port);
            socket.connect(address, timeout);

            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (UnknownHostException ex) {
            System.out.println("Error: Failed to connect to node " + ipAddress + ":" + port);
            cResult.setStatus(Status.UNROUTABLE);
            return;
        } catch (IOException ex) {
            System.out.println("Error: Failed to connect to node " + ipAddress + ":" + port);
            cResult.setStatus(Status.INTERNAL);
            return;
        }
        
        StringBuffer strBuf = new StringBuffer();
        String localAddress = socket.getLocalAddress().getHostAddress();
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
        	// ignore
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
	            
	            byte[] qhit = subBuffer(buffer, 23,qHitSize+23);
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
            // ignore
        }
    }

    private static void parseFilesListHeader(byte[] flist, int headerEndIndex){
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

    private static void processQueryHit(byte[] qhit){
        int endIndex;
        int numOfFiles = qhit[0];
        int fileSize = 0;
        String filesList = new String();
        String msg = new String();

        qhit = subBuffer(qhit,11,qhit.length);
        msg = new String(qhit);

        byte[] fname = new byte[512];
        int num = 0;
        while (qhit.length > 0 && num < numOfFiles){        	
        	fileSize=ByteOrder.leb2int(subBuffer(qhit, 4, 8),0,4);
        	if(fileSize < cResult.getMinimumFileSize() || cResult.getMinimumFileSize() == -1){
        		cResult.setMinimumFileSize(fileSize);
        	}
        	if(fileSize > cResult.getMaximumFileSize()){
        		cResult.setMaximumFileSize(fileSize);
        	}
        	cResult.addtotalFileSize(fileSize);
        	
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
    
    private static int findEndOfHeader(byte[] packet){

        for (int i = 0; i <= packet.length-4; i++){
            if (packet[i] == '\r' && packet[i+1] == '\n' && packet[i+2] == '\r' && packet[i+3] == '\n')
                return i+4;
        }
        return (-1);
    }
    
    private static byte[] subBuffer(byte[] buffer,int beginIndex,int endIndex){
        
        if (beginIndex>buffer.length)
            return null;
            
        if (endIndex > buffer.length)
            endIndex = buffer.length;
        
        int bLen = endIndex-beginIndex;
        byte[] result=new byte[bLen];
     
        System.arraycopy(buffer,beginIndex,result,0,bLen);
        
        return result;
    }

    private static void trimBufferBegining(byte[] buffer, int trimBeginIndex){
        if (trimBeginIndex > buffer.length)
            return;

        for (int i=trimBeginIndex ; i<buffer.length ; i++)
            buffer[i-trimBeginIndex] = buffer[i];
    }
    
    private static int bufferIndexOf(byte[] buffer,char c){
        for (int i=0;i<buffer.length;i++){
            if (buffer[i]==c)
                return i;
        }
        return (-1);
    }
    
}
