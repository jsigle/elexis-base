package ch.elexis.trustx;

import com4j.*;

/**
 * ITrustx Interface
 */
@IID("{0B3B047B-B477-4DEF-8D0A-ECD7616DAD36}")
public interface ITrustx extends Com4jObject {
	/**
	 * property trustxVersion
	 */
	@VTID(7)
	java.lang.String trustxVersion();
	
	/**
	 * property trustCenters
	 */
	@VTID(8)
	ch.elexis.trustx.ITrustxCollection trustCenters();
	
	@VTID(8)
	@ReturnValue(defaultPropertyThrough = {
		ch.elexis.trustx.ITrustxCollection.class
	})
	java.lang.String trustCenters(int index);
	
	/**
	 * property trustCenter
	 */
	@VTID(9)
	java.lang.String trustCenter();
	
	/**
	 * property trustCenter
	 */
	@VTID(10)
	void trustCenter(java.lang.String pVal);
	
	/**
	 * property inputDirectory
	 */
	@VTID(11)
	java.lang.String inputDirectory();
	
	/**
	 * property inputDirectory
	 */
	@VTID(12)
	void inputDirectory(java.lang.String pVal);
	
	/**
	 * property workDirectory
	 */
	@VTID(13)
	java.lang.String workDirectory();
	
	/**
	 * property workDirectory
	 */
	@VTID(14)
	void workDirectory(java.lang.String pVal);
	
	/**
	 * property withPDF
	 */
	@VTID(15)
	boolean withPDF();
	
	/**
	 * property withPDF
	 */
	@VTID(16)
	void withPDF(boolean pVal);
	
	/**
	 * property withArchive
	 */
	@VTID(17)
	boolean withArchive();
	
	/**
	 * property withArchive
	 */
	@VTID(18)
	void withArchive(boolean pVal);
	
	/**
	 * property ean
	 */
	@VTID(19)
	java.lang.String ean();
	
	/**
	 * property esr
	 */
	@VTID(20)
	java.lang.String esr();
	
	/**
	 * property esrPDF
	 */
	@VTID(21)
	java.lang.String esrPDF();
	
	/**
	 * property tcTel
	 */
	@VTID(22)
	java.lang.String tcTel();
	
	/**
	 * property session
	 */
	@VTID(23)
	java.lang.String session();
	
	/**
	 * property asasVersion
	 */
	@VTID(24)
	java.lang.String asasVersion();
	
	/**
	 * property asasLogins
	 */
	@VTID(25)
	ch.elexis.trustx.IAsasCollection asasLogins();
	
	@VTID(25)
	@ReturnValue(type = NativeType.VARIANT, defaultPropertyThrough = {
		ch.elexis.trustx.IAsasCollection.class
	})
	java.lang.Object asasLogins(int idx);
	
	/**
	 * property asasLogin
	 */
	@VTID(26)
	java.lang.String asasLogin();
	
	/**
	 * property asasLogin
	 */
	@VTID(27)
	void asasLogin(java.lang.String pVal);
	
	/**
	 * property activateGUI
	 */
	@VTID(28)
	boolean activateGUI();
	
	/**
	 * property activateGUI
	 */
	@VTID(29)
	void activateGUI(boolean pVal);
	
	/**
	 * property debug
	 */
	@VTID(30)
	boolean debug();
	
	/**
	 * property debug
	 */
	@VTID(31)
	void debug(boolean pVal);
	
	/**
	 * method Auto
	 */
	@VTID(32)
	void auto();
	
	/**
	 * method Read
	 */
	@VTID(33)
	void read();
	
	/**
	 * method Check
	 */
	@VTID(34)
	void check();
	
	/**
	 * method Anonymize
	 */
	@VTID(35)
	void anonymize();
	
	/**
	 * method Send
	 */
	@VTID(36)
	void send();
	
	/**
	 * method Cancel
	 */
	@VTID(37)
	void cancel();
	
}
