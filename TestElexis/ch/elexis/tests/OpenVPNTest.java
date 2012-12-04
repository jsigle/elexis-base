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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ngiger.comm.ftp.FtpServer;
import ch.ngiger.comm.vpn.OpenVPN;
import ch.elexis.tests.Preferences;

public class OpenVPNTest {
	static OpenVPN ovpn;
	static String srvName = ch.elexis.tests.Preferences.getOvpnPublic();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		ovpn = new OpenVPN();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception{}
	
	@Test
	public void testOpenConnection(){
		int step = 0;
		boolean res = false;
		String content = "Test-File to upload\n";
		
		try {
			try {
				InetAddress addr = InetAddress.getLocalHost(); // Get IP Address
				byte[] ipAddr = addr.getAddress(); // Get hostname
				String hostname = addr.getHostName();
				content += hostname + " ip " + InetAddress.getLocalHost().toString() + "\n";
			} catch (UnknownHostException e) {}
			java.util.Date today = new java.util.Date();
			String tStamp = new java.sql.Timestamp(today.getTime()) + "";
			content += tStamp;
			System.out.println("testOpenConnection: starting " + tStamp + " -> " + srvName);
			step = 1;
			String ftpSrv = ch.elexis.tests.Preferences.getOvpnFtp();
			String testUser = ch.elexis.tests.Preferences.getElexisUsername(1);
			step = 2;
			assert (ovpn.ping("172.222.222.222") == false);
			step = 3;
			assert (ovpn.ping(srvName));
			step = 4;
			res = ovpn.openConnection(ch.elexis.tests.Preferences.getOvpnConfig(), ftpSrv, 20);
			assert (res);
			step = 10;
			assert (ovpn.ping(srvName));
			step = 20;
			FtpServer ftp = new FtpServer();
			step = 21;
			String ftpPwd = Preferences.getElexisPwd(1);
			System.out.println("test to " + ftpSrv + " " + testUser + " " + ftpPwd);
			step = 24;
			ftp.openConnection(ftpSrv, testUser, ftpPwd);
			step = 25;
			File temp = File.createTempFile("OpenVPNTest", ".snd");
			FileWriter fos = new FileWriter(temp);
			fos.write(content);
			fos.close();
			String remoteName = "Remote";
			ftp.uploadFile(remoteName, temp.getAbsolutePath());
			step = 30;
			File tempRcv = File.createTempFile("OpenVPNTest", ".rcv");
			step = 31;
			ftp.downloadFile(remoteName, tempRcv.getAbsolutePath());
			step = 50;
		} catch (IOException e) {
			step += 100;
		} finally {
			java.util.Date today = new java.util.Date();
			String tStamp = new java.sql.Timestamp(today.getTime()) + "";
			System.out.println("testOpenConnection: finished " + tStamp);
			if (step != 50)
				System.out.println("could not reach step 50. I am  at " + step);
			else
				System.out.println("up/download successful: using\n" + content);
			assertEquals(step, 50);
		}
	}
	
	@Test
	public void testCloseConnection(){
		ovpn.closeConnection();
	}
	
}
