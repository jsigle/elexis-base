/*******************************************************************************
 * Copyright (c) 2006-2010, Gerry Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gerry Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.tests;

import java.util.Arrays;
import java.util.Hashtable;

import junit.framework.TestCase;
import ch.rgw.tools.StringTool;

public class TestStringtool extends TestCase {
	
	protected void setUp() throws Exception{
		
	}
	
	public void testMatchMail() throws Exception{
		String mailaddr = "psiska@students.unibe.ch";
		assertTrue(StringTool.isMailAddress(mailaddr));
	}
	
	public void testAmbiguify() throws Exception{
		String n1 = "abcädefüghiöjklè";
		String n2 = "abcaedefÜghiOejklé";
		String n3 = StringTool.unambiguify(n1);
		String n4 = StringTool.unambiguify(n2);
		assertEquals(n3, n4);
	}
	
	/**
	 * Statdard-Vefrahren?
	 * 
	 * @throws Exception
	 */
	public void testFold() throws Exception{
		Hashtable<String, String> hash = new Hashtable<String, String>();
		hash.put("first", "thisisfirst");
		hash.put("second", "thisissecond");
		assertNotNull(hash);
		String flat = StringTool.flattenStrings(hash);
		assertNotNull(flat);
		Hashtable check = StringTool.foldStrings(flat);
		assertNotNull(check);
	}
	
	public void testEnPrintable() throws Exception{
		byte[] check = new byte[256];
		for (int i = 0; i < 255; i++) {
			check[i] = (byte) i;
		}
		String print = StringTool.enPrintableStrict(check);
		byte[] dec = StringTool.dePrintableStrict(print);
		assertTrue(Arrays.equals(check, dec));
	}
	
}
