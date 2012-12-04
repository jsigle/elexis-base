package ch.elexis.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ch.elexis.data.PersistentObject;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.JdbcLinkException;

public class Test_SqlWithUiRunner {
	
	String[] successSqls = {
		"CREATE TABLE TEST_SQLWITHUIRUNNER(" + "column1 VARCHAR(80)," + "column2 VARCHAR(25));"
			+ "CREATE INDEX eol1 on TEST_SQLWITHUIRUNNER(column1);",
		
		"ALTER TABLE TEST_SQLWITHUIRUNNER ADD column3 VARCHAR(20);",
		"INSERT INTO TEST_SQLWITHUIRUNNER " + "VALUES ('ROW1','row1','1234567890123456789');",
		
		"ALTER TABLE TEST_SQLWITHUIRUNNER MODIFY column3 VARCHAR(255);",
		
		"DROP INDEX eol1;" + "DROP TABLE TEST_SQLWITHUIRUNNER;"
	};
	
	String[] failSqls = {
		"CREATE TABLE TEST_SQLWITHUIRUNNER(" + "column1 VARCHAR(80)," + "column2 VARCHAR(25));"
			+ "CREATE INDEX eol1 on TEST_SQLWITHUIRUNNER(column1);",
		
		"ALTER TABLE TEST_SQLWITHUIRUNNER ADD column3 VARCHAR(20);",
		"INSERT INTO TEST_SQLWITHUIRUNNER " + "VALUES ('ROW1','row1','1234567890123456789');",
		
		"ALTER TABLE TEST_SQLWITHUIRUNNER MODIFY column3 VARCHAR(10);",
		
		"ALTER TABLE TEST_SQLWITHUIRUNNER MODIFY column3 VARCHAR(255);",
		
		"DROP INDEX eol1;" + "DROP TABLE TEST_SQLWITHUIRUNNER;"
	};
	
	@Before
	public void setUp(){
		// just make sure the table is dropped so creation will be successful
		JdbcLink link = PersistentObject.getConnection();
		Stm statement = link.getStatement();
		try {
			statement.exec("DROP INDEX eol1;");
			statement.exec("DROP TABLE TEST_SQLWITHUIRUNNER;");
		} catch (JdbcLinkException e) {
			// ignore exceptions ...
		} finally {
			if (link != null && statement != null)
				link.releaseStatement(statement);
		}
	}
	
	@Test
	public void testUiRunSqlStrings(){
		SqlWithUiRunner runner = new SqlWithUiRunner(successSqls, "ch.elexis_test");
		assertNotNull(runner);
		boolean success = runner.runSql();
		assertTrue(success);
	}
	
	@Test
	public void testUiRunSqlStringsFail(){
		SqlWithUiRunner runner = new SqlWithUiRunner(failSqls, "ch.elexis_test");
		assertNotNull(runner);
		boolean success = runner.runSql();
		assertFalse(success);
		// make sure table got dropped, as drop statement should still succeed
		JdbcLink link = PersistentObject.getConnection();
		Stm statement = link.getStatement();
		try {
			statement.exec("SELECT * FROM TEST_SQLWITHUIRUNNER;");
			fail("Expected Exception not thrown!");
		} catch (JdbcLinkException e) {
			// expected exception ch.rgw.tools.JdbcLinkSyntaxException: Fehler bei: SELECT * FROM
			// TEST_SQLWITHUIRUNNER; (SQLState: 42S02)
			// Caused by: org.h2.jdbc.JdbcSQLException: Tabelle "TEST_SQLWITHUIRUNNER" nicht
			// gefunden
		} finally {
			if (link != null && statement != null)
				link.releaseStatement(statement);
		}
	}
}
