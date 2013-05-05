package ch.elexis.db.connection;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import ch.elexis.export.Messages;
import ch.elexis.export.hl7.Logger;

public abstract class AbstractConnection {
	
	private Connection connection = null;
	
	protected class DBRow {
		List<String> columns = new Vector<String>();
		List<Object> rowData = new Vector<Object>();
		
		private DBRow(final List<String> columnNames, final ResultSet rst) throws SQLException{
			for (String colName : columnNames) {
				this.columns.add(colName.toUpperCase().trim());
			}
			// Zeileninhalt ermitteln
			for (int i = 1; i <= columns.size(); i++) {
				Object rowData = null;
				if (rst.getMetaData().getColumnType(i) == Types.CLOB) {
					Clob clob = rst.getClob(i);
					if (clob != null) {
						Reader reader = clob.getCharacterStream();
						StringWriter writer = new StringWriter();
						char[] buffer = new char[1];
						try {
							while (reader.read(buffer) > 0) {
								writer.write(buffer);
							}
							rowData = writer.getBuffer().toString();
						} catch (IOException e) {
							rowData = clob.toString();
						}
					}
				} else {
					rowData = rst.getObject(i);
				}
				this.rowData.add(rowData);
			}
		}
		
		public String getString(final String colName){
			Object value = getValue(colName);
			if (value != null) {
				if (value instanceof String) {
					return (String) value;
				}
				return value.toString();
			}
			return null;
		}
		
		public Integer getInteger(final String colName){
			Object value = getValue(colName);
			if (value != null) {
				if (value instanceof Integer) {
					return (Integer) value;
				}
				if (value.toString().trim().length() > 0) {
					try {
						return Integer.parseInt(value.toString());
					} catch (NumberFormatException e) {
						Logger.logError(
							Messages.getString("AbstractConnection.errorParseInt") + value, e); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
		
		public Long getLong(final String colName){
			Object value = getValue(colName);
			if (value != null) {
				if (value instanceof Long) {
					return (Long) value;
				}
				if (value.toString().trim().length() > 0) {
					try {
						return Long.parseLong(value.toString());
					} catch (NumberFormatException e) {
						Logger.logError(
							Messages.getString("AbstractConnection.errorParseLong") + value, e); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
		
		public Date getDate(final String colName, final String pattern){
			Object value = getValue(colName);
			if (value != null) {
				if (value instanceof Date) {
					return (Date) value;
				}
				if (value.toString().trim().length() > 0) {
					try {
						return new SimpleDateFormat(pattern).parse(value.toString());
					} catch (ParseException e) {
						Logger.logError(
							Messages.getString("AbstractConnection.errorParseDate") + value, e); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
		
		private Object getValue(final String colName){
			return this.rowData.get(getColumnIndex(colName));
		}
		
		public List<Object> getValues(){
			return this.rowData;
		}
		
		private int getColumnIndex(final String colName){
			return this.columns.indexOf(colName.toUpperCase().trim());
		}
	}
	
	/**
	 * Opens a connection. If connection failed, a log output is written
	 * 
	 * @param path
	 *            Path of the database
	 * @param driverName
	 *            Driver name
	 * @param user
	 *            User or null
	 * @param pwd
	 *            Password or null
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	protected void openConnection(final String driverName, final String url, final String user,
		final String pwd) throws ClassNotFoundException, SQLException{
		// Sicherstellen, dass Jdbc-Odbc Driver in classpath
		Class.forName(driverName);
		Properties info = new Properties();
		info.put("charSet", "ISO-8859-1"); //$NON-NLS-1$ //$NON-NLS-2$
		if (user != null) {
			info.put("user", user); //$NON-NLS-1$
		}
		if (pwd != null) {
			info.put("password", pwd); //$NON-NLS-1$
		}
		
		this.connection = DriverManager.getConnection(url, info);
		Logger.logInfo(Messages.getString("AbstractConnection.infoConnectionSuccessful")); //$NON-NLS-1$
	}
	
	/**
	 * Close connection
	 */
	public void close() throws SQLException{
		if (this.connection != null && !this.connection.isClosed()) {
			this.connection.close();
		}
	}
	
	/**
	 * Getter for connection
	 * 
	 * @return
	 */
	protected Connection getConnection(){
		return this.connection;
	}
	
	/**
	 * Execute select statement and return result
	 * 
	 * @param sqlStatement
	 * @return DBResult
	 * @throws SQLException
	 */
	protected List<DBRow> select(final String sqlStatement) throws SQLException{
		Logger.logDebug(sqlStatement);
		Statement stmt = null;
		ResultSet rst = null;
		try {
			stmt = getConnection().createStatement();
			rst = stmt.executeQuery(sqlStatement);
			ResultSetMetaData md = rst.getMetaData();
			List<String> columnNames = new Vector<String>();
			int columnCount = md.getColumnCount();
			// Spaltennamen ermitteln
			for (int i = 1; i <= columnCount; i++) {
				String colname = md.getColumnName(i);
				columnNames.add(colname);
			}
			// Zeileninhalt ermitteln
			List<DBRow> rowList = new Vector<DBRow>();
			while (rst.next()) {
				rowList.add(new DBRow(columnNames, rst));
			}
			return rowList;
		} finally {
			if (rst != null) {
				rst.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
	}
	
	/**
	 * Executes SQL Statement
	 * 
	 * @param sqlStatement
	 * @return
	 * @throws SQLException
	 */
	protected boolean execute(final String sqlStatement) throws SQLException{
		Logger.logDebug(sqlStatement);
		Statement stmt = null;
		try {
			stmt = getConnection().createStatement();
			int updateCount = stmt.executeUpdate(sqlStatement);
			return updateCount != 0;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}
}
