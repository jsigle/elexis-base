// $Id$

package ch.elexis.export.tools;

import java.util.Iterator;

/**
 * Kopie des Elexis StringTool
 * 
 * @author Gerry Weirich
 */

public class StringTool {
	
	private static final char[] salties = {
		'q', 'w', 'e', 'r', 't', 'z', 'u', 'o', 'i', 'p', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k',
		'l', 'y', 'x', 'c', 'v', 'b', 'n', 'm', 'Q', 'A', 'Y', 'W', 'E', 'D', 'C', 'R', 'F', 'V',
		'T', 'G', 'B', 'Z', 'H', 'N', 'U', 'J', 'M', 'I', 'K', 'O', 'L', 'P'
	};
	
	public static final String ipv4address = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
	public static final String ipv6address =
		"((([0-9a-f]{1,4}+:){7}+[0-9a-f]{1,4}+)|(:(:[0-9a-f]{1,4}+){1,6}+)|(([0-9a-f]{1,4}+:){1,6}+:)|(::)|(([0-9a-f]{1,4}+:)(:[0-9a-f]{1,4}+){1,5}+)|(([0-9a-f]{1,4}+:){1,2}+(:[0-9a-f]{1,4}+){1,4}+)|(([0-9a-f]{1,4}+:){1,3}+(:[0-9a-f]{1,4}+){1,3}+)|(([0-9a-f]{1,4}+:){1,4}+(:[0-9a-f]{1,4}+){1,2}+)|(([0-9a-f]{1,4}+:){1,5}+(:[0-9a-f]{1,4}+))|(((([0-9a-f]{1,4}+:)?([0-9a-f]{1,4}+:)?([0-9a-f]{1,4}+:)?([0-9a-f]{1,4}+:)?)|:)(:(([0-9]{1,3}+\\.){3}+[0-9]{1,3}+)))|(:(:[0-9a-f]{1,4}+)*:([0-9]{1,3}+\\.){3}+[0-9]{1,3}+))(/[0-9]+)?";
	
	private static int ipHash;
	private static long sequence;
	
	/**
	 * Gibt eine zufällige und eindeutige Zeichenfolge zurück
	 * 
	 * @param salt
	 *            Ein beliebiger String oder null
	 */
	public static String unique(final String salt){
		if (ipHash == 0) {
			Iterator<String> it = NetTool.IPs.iterator();
			while (it.hasNext()) {
				ipHash += (it.next()).hashCode();
			}
		}
		
		long t = System.currentTimeMillis();
		int t1 = System.getProperty("user.name").hashCode();
		long t2 = ((long) ipHash) << 32;
		long t3 = Math.round(Math.random() * Long.MAX_VALUE);
		long t4 = t + t1 + t2 + t3;
		if (salt != null) {
			long t0 = salt.hashCode();
			t4 ^= t0;
		}
		t4 += sequence++;
		if (sequence > 99999) {
			sequence = 0;
		}
		long idx = sequence % salties.length;
		char start = salties[(int) idx];
		return new StringBuilder().append(start).append(Long.toHexString(t4))
			.append(Long.toHexString((long) Math.random() * 1000)).append(sequence).toString();
	}
	
	/**
	 * Test whether a String is an IPV4 or IPV6-Address
	 * 
	 * @param in
	 *            a String that is possibly an ipv4 or ipv6-Address
	 * @return true if ir seems to be an IP-Address
	 */
	public static boolean isIPAddress(final String in){
		if (in.matches(ipv4address)) {
			return true;
		}
		if (in.matches(ipv6address)) {
			return true;
		}
		return false;
	}
	
	/** gibt true zurück, wenn das Objekt kein String oder null oder "" ist */
	static public boolean isNothing(final Object n){
		if (n == null) {
			return true;
		}
		if (n instanceof String) { // if(((String)n).equals("")) return true;
			if (((String) n).trim().equals("")) {
				return true;
			}
			return false;
		}
		return true;
	}
}