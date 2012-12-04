package ch.elexis.labortarif2009.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import ch.elexis.Hub;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.SqlWithUiRunner;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.JdbcLinkException;
import ch.rgw.tools.TimeTool;

public class Test_Importer {
	// @formatter:off
	private static final String createOldTable =
		"create table " + Labor2009Tarif.load("1").getTableName() + "(" //$NON-NLS-1$ //$NON-NLS-2$
			+ "ID		VARCHAR(25) primary key," //$NON-NLS-1$
			+ "lastupdate BIGINT," //$NON-NLS-1$
			+ "deleted	 CHAR(1) default '0'," //$NON-NLS-1$
			+ "chapter   VARCHAR(10)," //$NON-NLS-1$
			+ "code		 VARCHAR(12)," //$NON-NLS-1$
			+ "tp		 VARCHAR(10)," //$NON-NLS-1$
			+ "name		 VARCHAR(255)," //$NON-NLS-1$
			+ "limitatio TEXT," //$NON-NLS-1$
			+ "fachbereich VARCHAR(10)," //$NON-NLS-1$
			+ "praxistyp VARCHAR(2));" //$NON-NLS-1$
			+ "INSERT INTO " + Labor2009Tarif.load("1").getTableName() + "(ID,code) VALUES (1,'" + Labor2009Tarif.VERSION010 + "');"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	// @formatter:on
	
	@After
	public void tearDown(){
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
	public void testDoImport2011() throws Exception{
		Labor2009Tarif.createTable();
		
		Importer importer = new Importer();
		URL testTarifFile = Test_Importer.class.getResource("/rsc/EAL_2011.xls");
		importer.tarifInputStream = testTarifFile.openStream();
		importer.doImport(null);
		
		Query<Labor2009Tarif> query = new Query<Labor2009Tarif>(Labor2009Tarif.class);
		List<Labor2009Tarif> importedTarif = query.execute();
		assertTrue(importedTarif.size() > 0);
		
		// test imported values of FLD_CHAPTER, FLD_CODE, FLD_TP, FLD_NAME, FLD_LIMITATIO,
		// FLD_FACHBEREICH, FLD_FACHSPEC on second object as first one is the table version
		Labor2009Tarif testTarif = importedTarif.get(0);
		assertEquals("1", testTarif.get(Labor2009Tarif.FLD_CHAPTER));
		assertEquals("1000.00", testTarif.get(Labor2009Tarif.FLD_CODE));
		assertEquals("85", testTarif.get(Labor2009Tarif.FLD_TP));
		assertEquals("1,25-Dihydroxycholecalciferol", testTarif.get(Labor2009Tarif.FLD_NAME));
		assertEquals("", testTarif.get(Labor2009Tarif.FLD_LIMITATIO));
		assertEquals("C", testTarif.get(Labor2009Tarif.FLD_FACHBEREICH));
		assertEquals("-1", testTarif.get(Labor2009Tarif.FLD_FACHSPEC));
	}
	
	@Test
	public void testDoImport2012() throws Exception{
		Labor2009Tarif.createTable();
		
		Importer importer = new Importer();
		URL testTarifFile = Test_Importer.class.getResource("/rsc/EAL_2012.xls");
		importer.tarifInputStream = testTarifFile.openStream();
		importer.doImport(null);
		
		Query<Labor2009Tarif> query = new Query<Labor2009Tarif>(Labor2009Tarif.class);
		List<Labor2009Tarif> importedTarif = query.execute();
		assertTrue(importedTarif.size() > 0);
		// test imported values of FLD_CHAPTER, FLD_CODE, FLD_TP, FLD_NAME, FLD_LIMITATIO,
		// FLD_FACHBEREICH, FLD_FACHSPEC on second object as first one is the table version
		Labor2009Tarif testTarif = importedTarif.get(1);
		assertEquals("1,2", testTarif.get(Labor2009Tarif.FLD_CHAPTER));
		assertEquals("1000.00", testTarif.get(Labor2009Tarif.FLD_CODE));
		assertEquals("85", testTarif.get(Labor2009Tarif.FLD_TP));
		assertEquals("1,25-Dihydroxycholecalciferol", testTarif.get(Labor2009Tarif.FLD_NAME));
		assertEquals("", testTarif.get(Labor2009Tarif.FLD_LIMITATIO));
		assertEquals("C", testTarif.get(Labor2009Tarif.FLD_FACHBEREICH));
		assertEquals("-1", testTarif.get(Labor2009Tarif.FLD_FACHSPEC));
	}
	
	@Test
	public void testUpdateTable(){
		// loading class creates new table or updates
		// load -> drop -> create old -> update
		Labor2009Tarif version = Labor2009Tarif.load("1"); //$NON-NLS-1$
		tearDown();
		createOrModifyTable(createOldTable);
		PersistentObject.clearCache();
		version = Labor2009Tarif.load("1"); //$NON-NLS-1$
		assertEquals(Labor2009Tarif.VERSION010, version.get(Labor2009Tarif.FLD_CODE));
		// update
		Labor2009Tarif.createTable();
		PersistentObject.clearCache();
		version = Labor2009Tarif.load("1"); //$NON-NLS-1$
		assertEquals(Labor2009Tarif.VERSION, version.get(Labor2009Tarif.FLD_CODE));
	}
	
	@Test
	public void testUpdate2011To2012() throws Exception{
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
		
		Query<Labor2009Tarif> query = new Query<Labor2009Tarif>(Labor2009Tarif.class);
		List<Labor2009Tarif> imported2011Tarif = query.execute();
		assertTrue(imported2011Tarif.size() == 1655);
		
		importer = new Importer();
		importer.validFrom = time2012;
		testTarifFile = Test_Importer.class.getResource("/rsc/EAL_2012.xls");
		importer.tarifInputStream = testTarifFile.openStream();
		importer.doImport(null);
		
		Query<Labor2009Tarif> query2012 = new Query<Labor2009Tarif>(Labor2009Tarif.class);
		query2012
			.add(Labor2009Tarif.FLD_GUELTIG_VON, "=", time2012.toString(TimeTool.DATE_COMPACT));
		List<Labor2009Tarif> imported2012Tarif = query2012.execute();
		assertTrue(imported2012Tarif.size() == 1669);
	}
	
	private static void dbgPrint(Labor2009Tarif tarif){
		System.err.println("Tarif: " + tarif.get(Labor2009Tarif.FLD_CODE) + " VON "
			+ tarif.get(Labor2009Tarif.FLD_GUELTIG_VON) + " BIS "
			+ tarif.get(Labor2009Tarif.FLD_GUELTIG_BIS));
	}
	
	/**
	 * copy of method from PersistenObject as we have no access here
	 * 
	 * @param sqlScript
	 *            create string
	 */
	private static void createOrModifyTable(final String sqlScript){
		String[] sql = new String[1];
		sql[0] = sqlScript;
		SqlWithUiRunner runner = new SqlWithUiRunner(sql, Hub.PLUGIN_ID);
		runner.runSql();
	}
}
