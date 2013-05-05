/*******************************************************************************
 * Copyright (c) 2006, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    D. Lutz - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.importers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.Desk;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;

/**
 * Dieser Importer konvertiert Daten von Mediwin CBpro der Aerztekasse nach Elexis.
 * 
 * In Mediwin CBpro können die Patienten mittels "Module/Export" exportiert werden. Es sollten alle
 * Felder ausgewählt und unter "Optionen" die Kolonnen-Titel aktiviert werden. Die Datei kann dann
 * mittels dieses Importers importiert werden. Importierte Patienten werden speziell markiert, so
 * dass der Importer diese wieder erkennen kann. Die Patienten können zu jeder Zeit erneut
 * importiert werden. Falls ein Patient nicht mehr importiert werden soll, kann in Elexis im Feld
 * "Bemerkungen" der Text "AEK:elexis" eingegeben werden, dann wird er ignoriert. Falls ein Patient
 * in Elexis erstellt wurde, und in Mediwin CBpro ein Patient mit der gleichen Nummer exisitert,
 * gibt es eine Warnung, und der Patient wird nicht importiert. Soll er trotzdem importiert werden,
 * kann der Patient mit der gleichen Nummer in Elexis im Feld "Bemerkungen" mit dem Text "AEK:aek"
 * markiert werden. Der in Elexis erfasste Patient wird dann mit den Daten aus Mediwin CBpro
 * ersetzt.
 * 
 * 
 * @author Daniel Lutz <danlutz@watz.ch>
 * 
 */
public class ImportAerztekasse extends ImporterPage {
	private static Log log = Log.get("Aerztekasse Import");
	
	// indices for AEK_COLUMN_NAMES and corresponding array variables
	private static final int NUMMER = 0;
	private static final int NAME = 1;
	private static final int VORNAME = 2;
	private static final int GEBURTSDATUM = 3;
	private static final int SEX = 4;
	private static final int TELEFON1 = 5;
	private static final int TELEFON2 = 6;
	private static final int STRASSE = 7;
	private static final int PLZ = 8;
	private static final int ORT = 9;
	private static final int LAND = 10;
	
	// column names in AEK export file
	private static final String[] AEK_COLUMNS_NAMES = {
		"Nummer", // NUMMER
		"Name", // NAME
		"Vorname", // VORNAME
		"Geburtsdatum", // GEBURTSDATUM
		"Sex", // SEX
		"Tel. Platz 1", // TELEFON1
		"Tel. Platz 2", // TELEFON2
		"Strasse", // STRASSE
		"PLZ", // PLZ
		"Ort", // ORT
		"Land" // LAND
	};
	
	// Elexis fields
	private static final String[] ELEXIS_FIELDS = {
		"PatientNr", "Name", "Vorname", "Geburtsdatum", "Geschlecht", "Telefon1", "Telefon2",
		"Strasse", "Plz", "Ort", "Land"
	};
	
	private static final int[] MAX_FIELD_LENGTHS = {
		10, // PatientNr
		80, // Name
		80, // Vorname
		-1, // Geburtsdatum, no lenght limitation here
		1, // Geschlecht
		15, // Telefon1
		15, // Telefon2
		40, // Strasse
		6, // Plz
		40, // Ort
		3
	// Land
		};
	
	/**
	 * Patient Numer as of Mediwin CBpro, stored in InfoStore. This helps us recognize an already
	 * imported patient.
	 */
	private static final String AEK_PATIENT_NR = "AEKPatientNr";
	
	/**
	 * Preference values indicating which patient should be preferred (AKE or Elexis), given in the
	 * field Bemerkungen. At initial import, PREFER_AEK is used. Can be changed by the user, so that
	 * a patient is no longer imported.
	 */
	private static final String PREFER_AEK = "AEK:aek";
	private static final String PREFER_ELEXIS = "AEK:elexis";
	
	/**
	 * Conversion map from long country names to short country names
	 */
	private static final HashMap<String, String> countryCodes = new HashMap<String, String>();
	
	static {
		countryCodes.put("Schweiz", "CH");
	}
	
	public ImportAerztekasse(){}
	
	@Override
	public String getTitle(){
		return "Aerztekasse Import";
	}
	
	@Override
	public String getDescription(){
		return "Bitte geben Sie den Pfad einer Datei an, die die Patientendaten enthält. Die Datei muss"
			+ " von Mediwin CBpro aus mittels \"Module/Export\" exportiert worden sein. Sie sollten"
			+ " alle Felder ausgewählt und unter \"Optionen\" die Kolonnen-Titel aktiviert haben.";
	}
	
	@Override
	public Composite createPage(final Composite parent){
		Composite ret = new ImporterPage.FileBasedImporter(parent, this);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return ret;
	}
	
	/**
	 * 
	 * @param columns
	 *            the column names in the header
	 * @param columnName
	 *            the column name to find
	 * @return the index of the column (starting with 0), or -1 if not found
	 */
	private int findColumnIndex(String[] columns, String columnName){
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(columnName)) {
				return i;
			}
		}
		
		return -1;
	}
	
	// parametrized Runnable showing an alert box.
	// used in Desk.theDisplay.asyncExec() call in the method doImport().
	private class Alert implements Runnable {
		private String title;
		private String message;
		
		Alert(String title, String message){
			this.title = title;
			this.message = message;
		}
		
		public void run(){
			SWTHelper.alert(title, message);
		}
	}
	
	private void alertConflict(String nummer, String existingLabel, String importedLabel){
		String title = "Patient existiert bereits";
		String message =
			"Ein Patient mit der Nummer " + nummer + " existiert bereits. Bitte geben Sie dem "
				+ "existierenden (" + existingLabel + ") oder dem zu importierenden ("
				+ importedLabel + ") Patienten eine andere Nummer. "
				+ "Versuchen Sie es dann erneut. (Der Patient " + "wurde nicht importiert.)";
		Desk.asyncExec(new Alert(title, message));
		
	}
	
	private void alertInvalidFormat(){
		String title = "Datei ungültig";
		String message =
			"Die Datei konnte nicht korrekt gelesen werden. "
				+ "Bitte überprüfen Sie den Inhalt der Datei.";
		
		Desk.asyncExec(new Alert(title, message));
	}
	
	private void alertStorageError(String label){
		String title = "Speicherfehler";
		String message =
			"Fehler beim Speichern von Patient \"" + label
				+ "\": Einige Felder konnten nicht gespeichert werden.";
		
		Desk.asyncExec(new Alert(title, message));
	}
	
	private void alert(String title, String message){
		Desk.asyncExec(new Alert(title, message));
	}
	
	/**
	 * 
	 * @param fields
	 *            the single fields of a line
	 * @param index
	 *            the index of the field to be returned
	 * @return the field with the requested index
	 */
	String getField(String[] fields, int index){
		String field = fields[index];
		return field;
	}
	
	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		File file = new File(results[0]);
		long l = file.length();
		InputStreamReader ir = new InputStreamReader(new FileInputStream(file), "iso-8859-1"); // TODO
																								// always
																								// this
																								// charset?
		BufferedReader br = new BufferedReader(ir);
		
		monitor.beginTask("Aerztekasse Import", (int) (l / 100));
		Query qbe = new Query(Patient.class);
		
		// header
		String header = br.readLine();
		if (header == null) {
			// error message
			alertInvalidFormat();
			monitor.done();
			return Status.CANCEL_STATUS;
		}
		String[] columns = header.split(";", -1);
		int[] aekIndices = new int[AEK_COLUMNS_NAMES.length];
		for (int i = 0; i < aekIndices.length; i++) {
			aekIndices[i] = findColumnIndex(columns, AEK_COLUMNS_NAMES[i]);
			if (aekIndices[i] == -1) {
				// at least one column not found
				// error message
				
				alertInvalidFormat();
				monitor.done();
				return Status.CANCEL_STATUS;
			}
		}
		
		String line;
		while ((line = br.readLine()) != null) {
			String[] fields = line.split(";", -1);
			if (fields.length < columns.length) {
				// not enough data
				String title = "Daten ungültig";
				String message =
					"Der aktuelle Patient konnte nicht korrekt gelesen werden. "
						+ "Bitte überprüfen Sie den Inhalt der Datei. " + "Zeile: " + line;
				Desk.asyncExec(new Alert(title, message));
				
				continue;
			}
			
			String[] values = new String[AEK_COLUMNS_NAMES.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = getField(fields, aekIndices[i]);
			}
			
			// remove leading 0 from number
			values[NUMMER] = values[NUMMER].replaceFirst("^0+", "");
			
			if (values[SEX].equals("1")) {
				values[SEX] = Person.MALE;
			} else if (values[SEX].equals("2")) {
				values[SEX] = Person.FEMALE;
			} else {
				// unknown
				values[SEX] = "";
			}
			
			// convert country
			if (countryCodes.containsKey(values[LAND])) {
				values[LAND] = countryCodes.get(values[LAND]);
			}
			
			// check field lengths
			for (int i = 0; i < values.length; i++) {
				if (MAX_FIELD_LENGTHS[i] > 0) {
					if (values[i].length() > MAX_FIELD_LENGTHS[i]) {
						// shorten field
						values[i] = values[i].substring(0, MAX_FIELD_LENGTHS[i]);
						
						log.log("Patient " + values[NUMMER] + ": Feld " + ELEXIS_FIELDS[i]
							+ " ist zu lang, wurde gekürzt.", Log.WARNINGS);
					}
				}
			}
			
			// original patient number, for recognizing an already imported patient
			String aekNumber = "AEK" + values[NUMMER];
			
			// find existing patient
			String id = qbe.findSingle("PatientNr", "=", values[NUMMER]);
			if (id == null) {
				// create new patient
				Patient patient =
					new Patient(values[NAME], values[VORNAME], values[GEBURTSDATUM], values[SEX]);
				log.log("Created patient " + values[NUMMER], Log.DEBUGMSG);
				if (!patient.set(ELEXIS_FIELDS, values)) {
					// warn the user (but continue work)
					String label = values[NAME] + " " + values[VORNAME];
					alertStorageError(label);
				}
				
				// set Bemerkung (informell)
				patient.set("Bemerkung", PREFER_AEK + " " + "Importiert von Mediwin CBpro");
				
				// set import hint
				patient.setInfoElement(AEK_PATIENT_NR, aekNumber);
			} else {
				// update existing patient
				Patient patient = Patient.load(id);
				
				// recongize already imported patient
				String oldAekNumber = patient.getInfoString(AEK_PATIENT_NR);
				if (!oldAekNumber.equals(aekNumber)) {
					// patient with same number already exists, but has not been
					// imported from the AEK program
					
					// import the patient if it is marked to be imported
					String bemerkung = patient.get("Bemerkung");
					if (bemerkung.startsWith(PREFER_AEK)) {
						// update and warn user
						patient.set(ELEXIS_FIELDS, values);
						// mark to be imported from AEK
						patient.setInfoElement(AEK_PATIENT_NR, aekNumber);
						
						// notify user
						String title = "Patient wurde importiert";
						String message =
							"Der Patient " + patient.getLabel()
								+ " wurde importiert. Die bereits existierenden "
								+ "alten Daten wurden überschrieben.";
						alert(title, message);
						
						log.log("Updated patient " + values[NUMMER], Log.DEBUGMSG);
					} else {
						// don't update and warn the user
						String existingLabel = patient.getLabel();
						String importedLabel = values[NAME] + " " + values[VORNAME];
						alertConflict(values[NUMMER], existingLabel, importedLabel);
						
						log.log("Conflicting patient " + values[NUMMER], Log.WARNINGS);
					}
					
				} else {
					// existing patient has been imported from the AEK program.
					// we can safely update unless PREFER_ELEXIS is given.
					String bemerkung = patient.get("Bemerkung");
					if (!bemerkung.startsWith(PREFER_ELEXIS)) {
						// update
						patient.set(ELEXIS_FIELDS, values);
						log.log("Updated patient " + values[NUMMER], Log.DEBUGMSG);
					} else {
						// notify user
						String title = "Patient nicht importiert";
						String message =
							"Der Patient " + patient.getLabel()
								+ " wurde nicht importiert, da er in Elexis "
								+ "geändert und entsprechend markiert wurde.";
						alert(title, message);
						log.log("Ignoring patient " + values[NUMMER], Log.WARNINGS);
					}
				}
			}
			
			if (monitor.isCanceled()) {
				monitor.done();
				return Status.CANCEL_STATUS;
			}
			
			monitor.worked(1);
		}
		
		monitor.done();
		return Status.OK_STATUS;
	}
}
