/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *
 *******************************************************************************/
package ch.elexis;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ch.elexis.data.Test_PersistentObject.class, ch.elexis.data.Test_LabItem.class,
	ch.elexis.data.Test_DBImage.class, ch.elexis.data.Test_Query.class,
	ch.elexis.util.Test_DBUpdate.class
})
public class AllTests {
	public static Test suite() throws ClassNotFoundException{
		TestSuite suite = new TestSuite("Elexis core tests");
		return suite;
	}
}
