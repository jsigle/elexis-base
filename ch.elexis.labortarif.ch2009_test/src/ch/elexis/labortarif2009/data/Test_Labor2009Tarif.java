package ch.elexis.labortarif2009.data;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.JdbcLinkException;
import ch.rgw.tools.TimeTool;

public class Test_Labor2009Tarif {
	@BeforeClass
	public static void initData2011u2012() throws Exception{
		TimeTool time2011 = new TimeTool();
		time2011.set(2011, 0, 1);
		TimeTool time2012 = new TimeTool();
		time2012.set(2012, 0, 1);
		
		Labor2009Tarif.createTable();
		
		Importer importer = new Importer();
		importer.validFrom = time2011;
		URL testTarifFile = Test_Importer.class.getResource("/rsc/EAL_2011.xls");
		importer.tarifInputStream = testTarifFile.openStream();
		importer.doImport(null);
		
		importer = new Importer();
		importer.validFrom = time2012;
		testTarifFile = Test_Importer.class.getResource("/rsc/EAL_2012.xls");
		importer.tarifInputStream = testTarifFile.openStream();
		importer.doImport(null);
	}
	
	@AfterClass
	public static void tearDown(){
		// just make sure the table is dropped so creation will be successful
		JdbcLink link = PersistentObject.getConnection();
		Stm statement = link.getStatement();
		try {
			statement.exec("DROP TABLE " + Labor2009Tarif.load("1").getTableName() + ";");
		} catch (JdbcLinkException e) {
			// ignore exceptions ...
		} finally {
			if (link != null && statement != null)
				link.releaseStatement(statement);
		}
	}
	
	@Test
	public void testIsValidOn(){
		TimeTool time2012 = new TimeTool();
		time2012.set(2012, 1, 1);
		TimeTool time2011 = new TimeTool();
		time2011.set(2011, 1, 1);
		
		// get all entries with matching code
		Query<Labor2009Tarif> qEntries = new Query<Labor2009Tarif>(Labor2009Tarif.class);
		qEntries.add(Labor2009Tarif.FLD_CODE, "=", "4707.10");
		List<Labor2009Tarif> entries = qEntries.execute();
		
		Labor2009Tarif lt2012 = null;
		Labor2009Tarif lt2011 = null;
		
		for (Labor2009Tarif labor2009Tarif : entries) {
			if (labor2009Tarif.isValidOn(time2012))
				lt2012 = labor2009Tarif;
			if (labor2009Tarif.isValidOn(time2011))
				lt2011 = labor2009Tarif;
		}
		
		assertEquals("4.1", lt2011.get(Labor2009Tarif.FLD_CHAPTER));
		assertEquals("4,2", lt2012.get(Labor2009Tarif.FLD_CHAPTER));
	}
}
