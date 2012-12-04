package ch.elexis.tests;

import ch.elexis.importers.HL7Parser;
import junit.framework.TestCase;

public class TestHL7 extends TestCase {
	public void testLabImport() throws Exception{
		HL7Parser hlp = new HL7Parser("TestLabor");
		fail();
		
	}
}