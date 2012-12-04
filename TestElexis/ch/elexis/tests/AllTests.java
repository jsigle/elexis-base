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

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	public static Test suite(){
		TestSuite suite = new TestSuite("Test for ch.elexis.tests");
		// $JUnit-BEGIN$
		suite.addTestSuite(TestHL7.class);
		suite.addTestSuite(TestStringtool.class);
		suite.addTestSuite(FtpServerTest.class);
		suite.addTestSuite(TestCompress.class);
		// suite.addTestSuite(TestSAT.class);
		// $JUnit-END$
		return suite;
	}
	
}
