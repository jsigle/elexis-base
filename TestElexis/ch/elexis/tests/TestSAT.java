package ch.elexis.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;
import ch.rgw.crypt.Cryptologist;
import ch.rgw.crypt.JCECrypter;
import ch.rgw.crypt.SAT;
import ch.rgw.tools.Result;

public class TestSAT extends TestCase {
	static String homedir;
	static Cryptologist crypt;
	static byte[] encrypted;
	private static final byte[] plain = {
		1, 3, 5, 7, 6, 4, 3, 2, 4, 10, 100, (byte) 254, (byte) 129
	};
	
	private static final String adminname = "admin@elexistest.ch";
	private static final String alicename = "alice@elexistest.ch";
	private static final String bobname = "bob@elexistest.ch";
	private static final String adminpwd = "adminpwd";
	private static final String alicepwd = "alicepwd";
	private static final String bobpwd = "bobpwd";
	
	private static final String datafile = System.getProperty("java.io.tmpdir") + File.separator
		+ "data.ttx";
	private static final String keyfile = System.getProperty("java.io.tmpdir") + File.separator
		+ "key.ttx";
	
	public void testModule() throws Exception{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); // Add
		
		// Create the secret/symmetric key
		KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "Blowfish");
		
		// Create the cipher for encrypting
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		
		// Encrypt the data
		byte[] encrypted = cipher.doFinal(plain);
		
		// Save the encrypted data
		FileOutputStream fos = new FileOutputStream(datafile);
		fos.write(encrypted);
		fos.close();
		
		// Save the cipher settings
		byte[] encodedKeySpec = skeySpec.getEncoded();
		FileOutputStream eksos = new FileOutputStream(keyfile);
		eksos.write(encodedKeySpec);
		eksos.close();
		
		// Read the encrypted data
		FileInputStream fis = new FileInputStream(datafile);
		byte[] temp = new byte[8192];
		int bytesRead = fis.read(temp);
		byte[] data = new byte[bytesRead];
		System.arraycopy(temp, 0, data, 0, bytesRead);
		
		// Read the cipher settings
		FileInputStream eksis = new FileInputStream(keyfile);
		bytesRead = eksis.read(temp);
		encodedKeySpec = new byte[bytesRead];
		System.arraycopy(temp, 0, encodedKeySpec, 0, bytesRead);
		
		// Recreate the secret/symmetric key
		skeySpec = new SecretKeySpec(encodedKeySpec, "Blowfish");
		
		// Create the cipher for encrypting
		cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		
		// Decrypt the data
		byte[] decrypted = cipher.doFinal(data);
		
		assertTrue(Arrays.equals(decrypted, plain));
		
	}
	
	public void testCreate() throws Exception{
		crypt = new JCECrypter(null, null, adminname, adminpwd.toCharArray());
		assertTrue(crypt.hasKeyOf(adminname));
	}
	
	public void testCreateKeys() throws Exception{
		if (!crypt.hasKeyOf("alice")) {
			/* KeyPair kp= */crypt.generateKeys(alicename, alicepwd.toCharArray(), null, null);
			
		}
		if (!crypt.hasKeyOf("bob")) {
			/* KeyPair kp= */crypt.generateKeys(bobname, bobpwd.toCharArray(), null, null);
		}
	}
	
	public void testEncrypt() throws Exception{
		JCECrypter crypter = new JCECrypter(null, null, alicename, alicepwd.toCharArray());
		byte[] encrypted = crypter.encrypt(plain, bobname);
		
		crypter = new JCECrypter(null, null, bobname, bobpwd.toCharArray());
		Result<byte[]> check = crypter.decrypt(encrypted);
		assertTrue(check.isOK());
		assertTrue(Arrays.equals(check.get(), plain));
		
	}
	
	public void testWrap() throws Exception{
		crypt = new JCECrypter(null, null, alicename, alicepwd.toCharArray());
		SAT sat = new SAT(crypt);
		HashMap<String, Serializable> hash = new HashMap<String, Serializable>();
		hash.put("test", "Ein Testtext");
		byte[] result = sat.wrap(hash, bobname);
		assertNotNull(result);
		System.out.println(new String(result));
		encrypted = result;
	}
	
	public void testUnwrap() throws Exception{
		crypt = new JCECrypter(null, null, bobname, bobpwd.toCharArray());
		SAT sat = new SAT(crypt);
		Map<String, Serializable> res = sat.unwrap(encrypted, true);
		assertNull(res.get("error"));
		String val = (String) res.get("test");
		assertEquals(val, "Ein Testtext");
		assertEquals(alicename, res.get(SAT.ADM_SIGNED_BY));
	}
}
