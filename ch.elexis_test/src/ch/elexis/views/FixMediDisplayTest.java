package ch.elexis.views;

import org.junit.Assert;
import org.junit.Test;

public class FixMediDisplayTest {
	@Test
	public void getNumTest(){
		Assert.assertEquals(0.5, FixMediDisplay.getNum("1/2"), 0.0);
		Assert.assertEquals(1.5, FixMediDisplay.getNum("6/4"), 0.0);
		Assert.assertEquals(0.5, FixMediDisplay.getNum("½"), 0.0);
		Assert.assertEquals(0.25, FixMediDisplay.getNum("¼"), 0.0);
		Assert.assertEquals(1.5, FixMediDisplay.getNum("1½"), 0.0);
		Assert.assertEquals(3, FixMediDisplay.getNum("9/3"), 0.0);
		// Assert.assertEquals(0, FixMediDisplay.getNum("testName"), 0.0);
	}
}
