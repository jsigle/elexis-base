package ch.elexis.core.databinding;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	Test_observables.class
})
public class AllTests {
	
	public static Test suite(){
		TestSuite suite = new TestSuite("elexis databinding tests");
		return suite;
	}
	
}
