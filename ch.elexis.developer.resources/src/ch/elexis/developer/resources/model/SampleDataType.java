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
 *    $Id: SampleDataType.java 6119 2010-02-12 06:16:14Z rgw_ch $
 *******************************************************************************/

package ch.elexis.developer.resources.model;

import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.VersionInfo;

/**
 * This is an example on how to derive your own type from PersistentObject and make it persistent
 * 
 * @author gerry
 * 
 */
public class SampleDataType extends PersistentObject {
	public static final String FLD_PATIENT_ID = "PatientID"; //$NON-NLS-1$
	public static final String FLD_FUNNY_STUFF = "FunnyStuff"; //$NON-NLS-1$
	public static final String FLD_BOREFACTOR = "Bore"; //$NON-NLS-1$
	public static final String FLD_FUNFACTOR = "Fun"; //$NON-NLS-1$
	public static final String FLD_TITLE = "Title"; //$NON-NLS-1$
	static final String VERSION = "1.0.0"; //$NON-NLS-1$
	/**
	 * The Name of the Table objects of this class will reside in. If a plugin creates its own
	 * table, the name MUST begin with the plugin ID to avoid name clashes. Note that dots must be
	 * replaced by underscores due to naming restrictions of the database engines.
	 */
	static final String TABLENAME = "ch_elexis_developer_resources_sampletable"; //$NON-NLS-1$
	
	/** Definition of the database table */
	static final String createDB = "CREATE TABLE "
		+ TABLENAME
		+ "("
		+ "ID VARCHAR(25) primary key," // This field must always be present
		+ "lastupdate BIGINT," // This field must always be present
		+ "deleted CHAR(1) default '0'," // This field must always be
		// present
		+ "PatientID VARCHAR(25),"
		+ "Title      VARCHAR(50)," // Use VARCHAR, CHAR, TEXT and BLOB
		+ "FunFactor VARCHAR(6)," // No numeric fields
		+ "BoreFactor	VARCHAR(6)," // VARCHARS can be read as integrals
		+ "Date		CHAR(8)," // use always this for dates
		+ "Remarks	TEXT," + "FunnyStuff BLOB);" + "CREATE INDEX "
		+ TABLENAME // Create index as needed
		+ "idx1 on " + TABLENAME + " (FunFactor);"
		// Do not forget to insert some version information
		+ "insert into " + TABLENAME + " (ID,Title) VALUES ('VERSION'," + JdbcLink.wrap(VERSION)
		+ ");";
	
	/**
	 * In the static initializer we construct the table mappings, then we try to load the Version of
	 * the table. If no version is found, we assume the table has to be created. If we find a
	 * version we check if it matches our version and update the table as needed.
	 */
	static {
		addMapping(TABLENAME, FLD_TITLE, "Fun=FunFactor", "Bore=BoreFactor", //$NON-NLS-1$ //$NON-NLS-2$
			"Date=S:D:Date", "Remarks", FLD_FUNNY_STUFF, FLD_PATIENT_ID); //$NON-NLS-1$ //$NON-NLS-2$
		SampleDataType version = load("VERSION"); //$NON-NLS-1$
		if (!version.exists()) {
			createOrModifyTable(createDB);
		} else {
			VersionInfo vi = new VersionInfo(version.get(FLD_TITLE));
			
			if (vi.isOlder(VERSION)) {
				// we should update eg. with createOrModifyTable(update.sql);
				// And then set the new version
				version.set(FLD_TITLE, VERSION);
			}
		}
	}
	
	/**
	 * Create a new Object. Any constructor except the empty constructor and the constructor with a
	 * single String argument are possible
	 * 
	 * @param title
	 * @param funFactor
	 * @param boreFactor
	 */
	public SampleDataType(Patient pat, String title, int funFactor, int boreFactor){
		create(null); // Use this to have the object's UUID generated by the
		// framework. If you provide a different parameter as
		// null, you are responsible for it to be unique.
		
		set(new String[] {
			FLD_PATIENT_ID, FLD_TITLE, FLD_FUNFACTOR, FLD_BOREFACTOR
		}, pat.getId(), title, Integer.toString(funFactor), Integer.toString(boreFactor));
		
	}
	
	/**
	 * accessor methods can be created as useful
	 * 
	 * @param funny
	 */
	public void setFunnyThings(byte[] funny){
		setBinary(FLD_FUNNY_STUFF, funny);
	}
	
	public byte[] getFunnyStuff(){
		return getBinary(FLD_FUNNY_STUFF);
	}
	
	public String getTitle(){
		return checkNull(get(FLD_TITLE));
	}
	
	public int getFunFactor(){
		return getInt(FLD_FUNFACTOR);
	}
	
	/**
	 * This should return a human readable short description of this object. The getLabel Method
	 * should be fast because it is called frequently.
	 */
	@Override
	public String getLabel(){
		StringBuilder sb = new StringBuilder();
		String[] result = new String[3];
		get(new String[] {
			FLD_TITLE, FLD_FUNFACTOR, FLD_BOREFACTOR
		}, result);
		synchronized (sb) {
			sb.append(result[0]).append(Messages.SampleDataType_hasFunFactor).append(result[1])
				.append(Messages.SampleDataType_hasBoreFactor).append(result[2]);
		}
		return sb.toString();
	}
	
	/**
	 * This static method should always be defined. We need this to retrieve PersistentObjects from
	 * the Database
	 * 
	 * @param id
	 * @return
	 */
	public static SampleDataType load(String id){
		return new SampleDataType(id);
	}
	
	/**
	 * This must return the name of the Table this class will reside in. This may be an existent
	 * table or one specificallym created by this plugin.
	 */
	@Override
	protected String getTableName(){
		return TABLENAME;
	}
	
	/**
	 * The constructor with a String parameter must be present
	 * 
	 * @param id
	 */
	protected SampleDataType(String id){
		super(id);
	}
	
	/**
	 * The default constructor must be present but is only called by the framework
	 */
	SampleDataType(){ /* do nothing */
	}
}
