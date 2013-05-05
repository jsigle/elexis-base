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

import junit.framework.TestCase;
import ch.elexis.importers.HL7Parser;

public class TestHL7 extends TestCase {
	public void testLabImport() throws Exception{
		HL7Parser hlp = new HL7Parser("TestLabor");
	//	fail();
		
	}
}