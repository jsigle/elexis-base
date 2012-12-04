package ch.elexis.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ngiger.comm.ftp.FtpSemaException;
import ch.ngiger.comm.ftp.FtpServer;

public class FtpServerTest extends TestCase {
	FtpServer ftp = null;
	/*
	 * This JUnit test works against a real FTP-Server!
	 */
	String ftpHost = Preferences.getFtpServer();
	String ftpUser = Preferences.getElexisUsername(1);
	String ftpPwd = Preferences.getElexisPwd(1);
	String ftpPwdInvalid = "NoSuchPassword";
	String downloadDir = System.getProperty("java.io.tmpdir")
		+ System.getProperty("file.separator") + "FtpServerTest";
	
	@Before
	public void setUp() throws Exception{
		boolean res = new File(downloadDir).mkdir();
		assert (res);
		File f = new File(downloadDir);
		assert (f.isDirectory());
		String[] files = f.list();
		for (int i = 0; i < files.length; i++) {
			res = new File(files[i]).delete();
			assert (res);
		}
		if (ftp != null && ftp.isConnected())
			ftp.disconnect();
	}
	
	@After
	public void tearDown() throws Exception{
		if (ftp != null)
			ftp.disconnect();
	}
	
	@Test
	public void testConnection(){
		ftp = new FtpServer();
		int step = 0;
		try {
			ftp.openConnection(ftpHost, ftpUser, ftpPwdInvalid);
			assertFalse(ftp.isConnected());
			step = 10;
		} catch (IOException e) {
			step += 100;
			assert (true);
		}
		try {
			ftp = new FtpServer();
			ftp.openConnection(ftpHost, ftpPwdInvalid, ftpPwd);
			assertFalse(ftp.isConnected());
			step = 20;
		} catch (IOException e) {
			step += 100;
			assert (true);
		}
		try {
			ftp = new FtpServer();
			ftp.openConnection(ftpPwdInvalid, ftpUser, ftpPwd);
			assertFalse(ftp.isConnected());
			step = 30;
		} catch (IOException e) {
			step += 100;
			assert (true);
		}
		try {
			ftp = new FtpServer();
			ftp.openConnection(ftpHost, ftpUser, ftpPwd);
			assert (ftp.isConnected());
			step = 40;
		} catch (IOException e) {
			fail();
		} finally {
			assert (step == 40);
		}
	}
	
	@Test
	public void testListFiles(){
		int step = -1;
		System.out.println("My passwd is " + ftpPwd);
		ftp = new FtpServer();
		try {
			step = 10;
			ftp.openConnection(ftpHost, ftpUser, ftpPwd);
			assert (ftp.isConnected());
			step = 20;
			String[] filenameList = ftp.listNames();
			step = 30;
			assert (filenameList.length > 0);
			for (String filename : filenameList) {
				ftp.deleteFile(filename);
			}
			step = 40;
			assert (ftp.isConnected());
			step = 50;
		} catch (IOException e) {
			fail();
		} finally {
			assertEquals(step, 50);
		}
		assertEquals(step, 50);
	}
	
	@Test
	public void testGetFile(){
		String PRAXIS_SEMAPHORE = "Praxis.Sema";
		String LABO_SEMAPHORE = "Labo.Sema";
		String RemoteName = "remoteName";
		String Content = "First and \nsecond line\n";
		String localName = downloadDir + System.getProperty("file.separator") + "TestDatei.txt";
		String rcvName = localName + ".rcv";
		int step = -1;
		ftp = new FtpServer();
		try {
			ftp.openConnection(ftpHost, ftpUser, ftpPwd);
			assert (ftp.isConnected());
			FileOutputStream fos = new FileOutputStream(localName);
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
			out.write(Content);
			out.close();
			File f = new File(localName);
			assert (f.exists());
			step = 1;
			
			ftp.uploadFile("RemoteName", localName);
			step = 10;
			ftp.addSemaphore(downloadDir, PRAXIS_SEMAPHORE, LABO_SEMAPHORE);
			step = 20;
			assert (ftp.isConnected());
			boolean res = f.delete();
			assert (res);
			assertFalse(f.exists());
			step = 30;
			
			String[] filenameList = ftp.listNames();
			assert (filenameList.length > 0);
			for (String filename : filenameList) {
				System.out.println("filename list " + filename);
				ftp.deleteFile(filename);
			}
			step = 40;
			assert (ftp.isConnected());
			step = 50;
			
		} catch (IOException e) {
			fail();
		} catch (FtpSemaException e) {
			fail();
		} finally {
			assertEquals(step, 50);
		}
		assertEquals(step, 50);
	}
	
}
