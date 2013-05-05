/*******************************************************************************
 * Copyright (c) 2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data;

import java.util.ArrayList;
import java.util.List;

import com.hilotec.elexis.messwerte.Activator;
import com.hilotec.elexis.messwerte.data.typen.IMesswertTyp;

import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;

import ch.rgw.tools.TimeTool;

public class Messung extends PersistentObject {
	private static final String VERSION = "1";
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_MESSWERTE_MESSUNGEN";
	
	static {
		addMapping(TABLENAME, "PatientID", "TypName", "Datum=S:D:Datum");
		checkTable();
	}
	
	private static final String create = "CREATE TABLE " + TABLENAME + " ("
		+ "  ID			VARCHAR(25) PRIMARY KEY, " + "  lastupdate 	BIGINT, "
		+ "  deleted		CHAR(1) DEFAULT '0', " + "  PatientID	VARCHAR(25), "
		+ "  TypName		VARCHAR(25), " + "  Datum		CHAR(8) " + ");" + "INSERT INTO " + TABLENAME
		+ " (ID, TypName) VALUES " + "	('VERSION', '" + VERSION + "');";
	
	/**
	 * Pruefen ob die Tabelle existiert
	 */
	private static void checkTable(){
		Messung check = load("VERSION");
		if (!check.exists()) {
			createOrModifyTable(create);
		}
	}
	
	@Override
	public String getLabel(){
		return get("TypName");
	}
	
	@Override
	public String getTableName(){
		return TABLENAME;
	}
	
	/**
	 * Dieser Konstruktor darf nicht oeffentlich sein
	 */
	protected Messung(){}
	
	/**
	 * Vorhandene Messung anhand der ID erstellen
	 * 
	 * @param id
	 */
	protected Messung(String id){
		super(id);
	}
	
	/**
	 * Neue Messung erstellen
	 * 
	 * @param patient
	 *            Patient, dem diese Messung zugeordnet werden soll
	 * @param typ
	 *            Typ der Messung
	 */
	public Messung(Patient patient, MessungTyp typ){
		create(null);
		set("PatientID", patient.getId());
		set("TypName", typ.getName());
		set("Datum", new TimeTool().toString(TimeTool.DATE_GER));
	}
	
	/**
	 * Messung anhand der ID laden
	 * 
	 * @param id
	 *            ID der Messung
	 * @return Messung
	 */
	public static Messung load(final String id){
		return new Messung(id);
	}
	
	/**
	 * Datum dieser Messung
	 */
	public String getDatum(){
		return get("Datum");
	}
	
	/**
	 * Messwert in dieser Messung anhand seines Namens holen
	 * 
	 * @param name
	 *            Name des Messwerttyps
	 * @return Messwert
	 */
	public Messwert getMesswert(String name){
		return getMesswert(name, true);
	}
	
	public Messwert getMesswert(String name, boolean create){
		Query<Messwert> query = new Query<Messwert>(Messwert.class);
		query.add("MessungID", Query.EQUALS, getId());
		query.and();
		query.add("Name", Query.EQUALS, name);
		List<Messwert> list = query.execute();
		
		if (list.size() == 0) {
			if (create) {
				// Messwert existiert noch nicht, wir legen ihn neu an
				return new Messwert(this, name);
			}
			return null;
		}
		
		return list.get(0);
	}
	
	/**
	 * @return Liste aller Messwerte
	 */
	public List<Messwert> getMesswerte(){
		List<IMesswertTyp> fields = getTyp().getMesswertTypen();
		ArrayList<Messwert> messwerte = new ArrayList<Messwert>();
		
		for (IMesswertTyp dft : fields) {
			messwerte.add(getMesswert(dft.getName()));
		}
		
		return messwerte;
	}
	
	/**
	 * @param datum
	 *            Datum neu setzen
	 */
	public void setDatum(String datum){
		set("Datum", datum);
	}
	
	/**
	 * @return Typ der Messung
	 */
	public MessungTyp getTyp(){
		MessungKonfiguration config = MessungKonfiguration.getInstance();
		return config.getTypeByName(get("TypName"));
	}
	
	/**
	 * @return Patient zu dem diese Messung gehoert
	 */
	public Patient getPatient(){
		return Patient.load(get("PatientID"));
	}
	
	/**
	 * Alle Messungen eines bestimmten Typs zu einem bestimmten Patienten zusammensuchen.
	 * 
	 * @param patient
	 *            Der Patient
	 * @param typ
	 *            Typ der zu suchenden Messungen
	 * 
	 * @return Liste mit den Messungen
	 */
	public static List<Messung> getPatientMessungen(Patient patient, MessungTyp typ){
		Query<Messung> query = new Query<Messung>(Messung.class);
		query.add("PatientID", Query.EQUALS, patient.getId());
		if (typ != null) {
			query.and();
			query.add("TypName", Query.EQUALS, typ.getName());
		}
		return query.execute();
	}
	
	/**
	 * Alle Messungen eines bestimmten Typs zusammensuchen.
	 * 
	 * @param typ
	 *            Typ der zu suchenden Messung
	 * 
	 * @return Liste mit den Messungen
	 */
	public static List<Messung> getMessungen(MessungTyp typ){
		Query<Messung> query = new Query<Messung>(Messung.class);
		query.add("TypName", Query.EQUALS, typ.getName());
		return query.execute();
	}
}
