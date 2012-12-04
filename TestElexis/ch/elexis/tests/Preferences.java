/*******************************************************************************
 * Copyright (c) 2010, Niklaus Giger and Medelexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Niklaus Giger - initial implementation
 *    
 *  $Id: NumberInput.java 5321 2009-05-28 12:06:28Z rgw_ch $
 *******************************************************************************/
package ch.elexis.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Preferences {
	
	static Properties testProperties = initProperties();
	
	private static Properties initProperties(){
		Properties props = new Properties();
		String propName = "../BuildElexis/rsc/build/local.properties";
		try {
			props.load(new FileInputStream(propName));
			System.out.println("loaded " + propName);
		} catch (IOException e) {
			System.out.println("Unable to load " + propName);
		}
		return props;
	}
	
	/*
	 * 
	 * We assume that we can have up to 10 elexis-test users
	 */
	static String getElexisUsername(int whichOne){
		return testProperties.getProperty("elexis.test.user." + whichOne, "elexis-" + whichOne);
	}
	
	/*
	 * Password for FTP,OpenVPN,DB to be used. Assume the same for all services.
	 * 
	 * @returns password
	 */
	static String getElexisPwd(int whichOne){
		return testProperties.getProperty("elexis.test.password." + whichOne, "1234");
	}
	
	/*
	 * Server which offers OpenVPN connections to elexis-test users
	 * 
	 * @returns IP or URL
	 */
	static String getOvpnPublic(){
		return testProperties.getProperty("elexis.test.ovpn.public", "173.25.1.40");
	}
	
	/*
	 * Config-file for the OpenVPN. The exe is assumed to be ../bin/openvnp
	 * 
	 * @returns IP or URL
	 */
	static String getOvpnConfig(){
		return testProperties.getProperty("elexis.test.ovpn.config", "/etc/openvpn/elexis-1.ovpn");
	}
	
	/*
	 * Server which FTP-services via OpenVPN
	 * 
	 * @returns IP or URL
	 */
	static String getOvpnFtp(){
		return testProperties.getProperty("elexis.test.ovpn.ftp", "173.23.45.1");
	}
	
	/*
	 * Server which offers Ftp to elexis-test users
	 */
	static String getFtpServer(){
		return testProperties.getProperty("elexis.test.ftp.server", "172.25.1.40");
	}
	
}
