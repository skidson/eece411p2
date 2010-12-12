package ca.ubc.ece.crawler;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SteveDriver {

	public static void main(String[] args) {
		IPCache ipCache = new IPCache();
		ipCache.cache(("128.189.169.165\n").trim());
		ipCache.cache("68.149.231.212");
		System.out.println(ipCache.isCached("128.189.169.165") + " should be true");
		System.out.println(ipCache.isCached("68.149.231.212") + " should be true");
		System.out.println(ipCache.isCached("69.149.231.212") + " should be false");
		System.out.println(ipCache.toString());
		
		try {
			System.out.println(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {}
	}

}
