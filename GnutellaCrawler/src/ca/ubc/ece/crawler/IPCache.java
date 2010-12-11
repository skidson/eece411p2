package ca.ubc.ece.crawler;

public class IPCache {
	/* IP: a.b.c.d */
	private byte[] a;
	private byte[] b;
	private byte[] c;
	
	public IPCache() {
		a = new byte[32];
		b = new byte[128];
		c = new byte[128];
	}
	
	public IPCache(int aSize, int bSize, int cSize) {
		a = new byte[aSize];
		b = new byte[bSize];
		c = new byte[cSize];
	}
	
	public void cache(String address) {
		int[] domains = toDomains(address);
		
		a[domains[0]%a.length] = (byte) domains[1];
		b[domains[1]%b.length] = (byte) domains[2];
		c[domains[2]%c.length] = (byte) domains[3];
	}
	
	public boolean isCached(String address) {
		int[] domains = toDomains(address);
		if (a[domains[0]] == domains[1] &&
			b[domains[1]] == domains[2] &&
			c[domains[2]] == domains[3]) {
			return true;
		}
		return false;
	}
	
	private int[] toDomains(String address) {
		String[] stringDomains = address.split(".");
		int[] domains = new int[4];
		
		// Encode unsigned value for use with signed bytes
		int i = 0;
		for(String domain : stringDomains) {
			Integer temp = Integer.parseInt(domain);
			if (temp > 127)
				temp -= 256;
			domains[i] = temp;
			i++;
		}
		return domains;
	}
	
}
