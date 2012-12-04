package ch.elexis.data;

import static org.junit.Assert.fail;

import org.junit.Test;

import ch.elexis.core.PersistenceException;
import ch.rgw.tools.JdbcLink;

public class Test_DBImage extends AbstractPersistentObjectTest {
	
	@Test
	public void testConstructorFail(){
		JdbcLink link = initDB();
		try {
			DBImage img = new DBImage("", "test", null);
			fail("Expected Exception not thrown!");
		} catch (PersistenceException pe) {
			
		}
		link.disconnect();
	}
}
