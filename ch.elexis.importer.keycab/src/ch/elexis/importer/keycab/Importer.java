/*******************************************************************************
 * Copyright (c) 2010, Niklaus Giger niklaus.giger@member.fsf.org
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    N. Giger - used praxistar to borrow some ideas for Keycab import
 *    
 *******************************************************************************/

package ch.elexis.importer.keycab;

import java.io.File;
import java.util.*;
import java.text.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import com.healthmarketscience.jackcess.*;

import ch.elexis.data.Fall;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Xid;
import ch.elexis.exchange.KontaktMatcher;
import ch.elexis.tarmedprefs.TarmedRequirements;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;

import com.healthmarketscience.jackcess.Database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Importer extends ImporterPage {
	private static final float TOTALWORK = 100000;
	private static final float WORK_PORTIONS = 5;
	
	public static final String PLUGINID = "ch.elexis.importer.Keycab";
	
	// we'll use these local XID's to reference the external data
	private final static String IMPORT_XID = "elexis.ch/Keycab";
	private final static String PATID = IMPORT_XID + "/PatID";
	private final static String GARANTID = IMPORT_XID + "/garantID";
	private final static String ARZTID = IMPORT_XID + "/arztID";
	private final static String USERID = IMPORT_XID + "/userID";
	private static Database db = null;
	private static int maxRecordsToImport = 5000;
	private static int maxRecordsToDisplay = 5;
	static {
		Fall.getAbrechnungsSysteme(); // make sure billing systems are
		// initialized
		Xid.localRegisterXIDDomainIfNotExists(PATID, "Alte Patientennummer", Xid.ASSIGNMENT_LOCAL);
		Xid.localRegisterXIDDomainIfNotExists(GARANTID, "Alte Garant-ID", Xid.ASSIGNMENT_LOCAL);
		Xid.localRegisterXIDDomainIfNotExists(ARZTID, "Alte Arzt-ID", Xid.ASSIGNMENT_LOCAL);
		Xid.localRegisterXIDDomainIfNotExists(USERID, "Alte Anwender-ID", Xid.ASSIGNMENT_LOCAL);
		Xid.localRegisterXIDDomainIfNotExists(TarmedRequirements.DOMAIN_KSK, "KSK",
			Xid.ASSIGNMENT_REGIONAL);
		Xid.localRegisterXIDDomainIfNotExists(TarmedRequirements.DOMAIN_NIF, "NIF",
			Xid.ASSIGNMENT_REGIONAL);
	}
	
	public Importer(){
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Composite createPage(final Composite parent){
		Composite ret = new ImporterPage.FileBasedImporter(parent, this);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return ret;
	}
	
	private class CommentaireAlarme {
		String id;
		String commentaire;
		String date;
		String actif;
		
		CommentaireAlarme(String vId, String vCommentaire, String vDate, String vActif){
			id = vId;
			commentaire = vCommentaire;
			date = vDate;
			actif = vActif;
		}
		
		public String toString(){
			if (actif.equalsIgnoreCase("false"))
				return new String(date + ": " + commentaire);
			else
				return actif;
		}
	}
	
	static Map<String, CommentaireAlarme> id2Alarme = new HashMap<String, CommentaireAlarme>();
	
	static final String T_Id2Alarme_Name = "PATIENT_COMMENTAIRE_ALARME_T074";
	
	private CommentaireAlarme getAlarme(String aId){
		return id2Alarme.get(aId);
	}
	
	private void initAlarme(){
		int num = 0;
		try {
			Table t = db.getTable(T_Id2Alarme_Name);
			// Patient_ID_Nr Commentaire date actif
			if (t != null) {
				t.display(10);
				num = t.getRowCount();
				Iterator<Map<String, Object>> it = t.iterator();
				while (it.hasNext()) {
					Map<String, Object> row = it.next();
					String id = getCol(row, "Patient_ID_Nr");
					String alarme = getCol(row, "Commentaire");
					String date = getCol(row, "date");
					String actif = getCol(row, "actif");
					CommentaireAlarme data = new CommentaireAlarme(id, alarme, date, actif);
					id2Alarme.put(new String(id), data);
				}
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		System.out.println("Added id2Alarme " + id2Alarme.size() + " of " + num);
	}
	
	private class PlzData {
		String id;
		String plz;
		String ort;
		
		PlzData(String vId, String vPlz, String vOrt){
			id = vId;
			plz = vPlz;
			ort = vOrt;
		}
	}
	
	static Map<String, PlzData> id2Data = new HashMap<String, PlzData>();
	
	static final String T_Id2Plz_Name = "T_LOCALITE_T023";
	
	// Localite_ID CPost Localite
	
	private PlzData getPlzOrt(String plzId){
		return id2Data.get(plzId);
	}
	
	private void initPlzData(){
		int num = 0;
		try {
			Table t = db.getTable(T_Id2Plz_Name);
			// Localite_ID CPost Localite
			if (t != null) {
				t.display(10);
				num = t.getRowCount();
				Iterator<Map<String, Object>> it = t.iterator();
				while (it.hasNext()) {
					Map<String, Object> row = it.next();
					String id = getCol(row, "Localite_ID");
					String plz = getCol(row, "CPost");
					String ort = getCol(row, "Localite");
					PlzData data = new PlzData(id, plz, ort);
					id2Data.put(new String(id), data);
				}
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		System.out.println("Added nr local_id " + id2Data.size() + " of " + num);
	}
	
	private void appendIfNotEmpty(final StringBuilder sb, final String title, final String value){
		if (!StringTool.isNothing(value)) {
			sb.append(title).append(value).append("\n");
		}
	}
	
	private void showInfo(String tablename){
		System.out.println("keycab: showInfo");
		try {
			Table t = db.getTable(tablename);
			if (t != null) {
				
				System.out.println("table " + tablename + " has " + t.getRowCount() + " rows");
				System.out.println(t.display(maxRecordsToDisplay));
				// System.out.println(t.getColumns().toString());
			} else {
				System.out.println(tablename + " not found");
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return;
		}
	}
	
	private String getCol(Map<String, Object> map, String colname){
		Object o = map.get(colname);
		if (o == null)
			return "";
		else
			return o.toString();
	}
	
	/*
	 * @param eingabe. String as reported by jackcess e.g.Sat Jun 16 00:00:00 CET 1956
	 */
	private Date string2Date(String eingabe){
		// return java.Date date ;
		// return new SimpleDateFormat("dd-MMM-yy");
		// Sat Jun 16 00:00:00 CET 1956
		Pattern pattern = Pattern.compile(".* (.*) (.*) .* .* (.*)");
		Matcher m = pattern.matcher(eingabe);
		String simpler = null;
		if (m.matches()) {
			try {
				simpler = new String(m.group(1) + " " + m.group(2) + " " + m.group(3));
				// System.out.println("simpler?: " + simpler);
				Date date = new SimpleDateFormat("MMM dd yyyy", new Locale("en")).parse(simpler);
				// Sat Jun 16 00:00:00 CET 1956
				return date;
			} catch (ParseException e) {
				System.out.println("Failed parsing :" + eingabe);
				return null;
				
			}
		}
		System.out.println("Parsing :" + eingabe + " gives null");
		return null;
	}
	
	static Map<String, String> country2code = new HashMap<String, String>();
	
	private void initCountry2code(){
		country2code.put("Suisse", "CH");
		country2code.put("Allemagne", "D");
		country2code.put("Italie", "I");
		country2code.put("France", "F");
		country2code.put("Autriche", "A");
		country2code.put("Portugal", "P");
		country2code.put("Espagne", "E");
	}
	
	// Patient_ID_Nr Patient_NrDossier Patient_Medecin_ID Patient_Titre Patient_Nom
	// Patient_NomJFille
	// Patient_Prenom Patient_Adres_CO Patient_Adresse Patient_Localite_ID Patient_TelPriv
	// Patient_Natel
	// Patient_Fax Patient_Email Patient_TelProf Patient_Profession Patient_Employeur
	// Patient_DateNaiss
	// Patient_CodeSecuSocial Patient_Sexe Patient_Parente Patient_EtatCivil Patient_Origine
	// Patient_Comment Patient_Nationalite Patient_Garant Patient_Remise_% Patient_Medecin_Traitant
	// Patient_Medecin_Adresse Patient_Commentaire_ALARME_T074 Patient_Nr_Covercard Patient_Inactif
	private boolean importPatients(IProgressMonitor monitor, String tableName){
		monitor.subTask("Importations des patients");
		int counter = 0, nr = 0, nrDone = 0;
		try {
			Table t = db.getTable(tableName);
			if (t != null) {
				int num = t.getRowCount();
				final int PORTION = Math.round(TOTALWORK / 2 / num);
				System.out.println("importing " + num + " rows");
				System.out.println(t.display(maxRecordsToDisplay));
				Iterator<Map<String, Object>> it = t.iterator();
				while (it.hasNext()) {
					monitor.worked(1);
					Map<String, Object> row = it.next();
					// System.out.println("import " + tableName + " " + nr + "/" + num + " " +
					// counter
					// + "/" + nrDone + " " + row);
					String ID = getCol(row, ("Patient_NrDossier"));
					if (ID.equals(""))
						continue; // wir können keine Schrott importieren
					if (!ID.toUpperCase().startsWith("B"))
						continue; // Nur die Dossiers von Bruno !!
					// Patient_ID_Nr ist fortlaufende Nummer von KeyCab
					if (Xid.findObject(PATID, ID) != null) {
						log.log("Skipped " + ID, Log.DEBUGMSG);
						continue; // avoid multiple imports
					}
					// String EAN = getCol(row, ("Patient_NrDossier"));
					String titel = getCol(row, "Patient_Titre");
					String vorname = getCol(row, "Patient_Prenom");
					if (vorname.equals(""))
						continue; // wir können keine Schrott importieren
					String name = getCol(row, "Patient_Nom");
					if (name.equals(""))
						continue; // wir können keine Schrott importieren
					String bez2 = getCol(row, "Patient_NomJFille");
					String strasse = getCol(row, "Patient_Adresse");
					String lId = getCol(row, "Patient_Localite_ID");
					if (lId.equals(""))
						continue; // wir können keine Schrott importieren
					String plz = getPlzOrt(lId).plz;
					String ort = getPlzOrt(lId).ort;
					String email = getCol(row, "Patient_Email");
					String tel1 = getCol(row, "Patient_TelPriv");
					String tel2 = getCol(row, "Patient_Fax"); // ou Patient_TelProf
					String natel = getCol(row, "Patient_Natel");
					String sexe = getCol(row, "Patient_Sexe");
					// if (sexe.equals(""))
					// continue; // wir können keine Schrott importieren
					sexe = sexe.toUpperCase().equals("M") ? Patient.MALE : Patient.FEMALE;
					if (getCol(row, "Patient_DateNaiss").equals(""))
						continue; // wir können keine Schrott importieren
					Date birthDate = string2Date(getCol(row, "Patient_DateNaiss"));
					if (birthDate == null)
						continue;
					SimpleDateFormat xx = new SimpleDateFormat("dd.MM.yyyy");
					String german = xx.format(birthDate);
					String zusatz = getCol(row, "Patient_Adres_CO");
					nr++;
					plz = plz.replaceAll("\\D", ""); // we only want the digits
					Patient pat = new Patient(name, vorname, german, sexe);
					pat.set("PatientNr", ID);
					monitor.subTask("Patient: " + ID + " " + pat.getLabel());
					pat.set(new String[] {
						"Strasse", "Plz", "Ort", "Telefon1", "Telefon2", "Natel"
					}, getCol(row, "Patient_Adresse"), plz, ort, getCol(row, "Patient_TelPriv"),
						getCol(row, "Patient_TelProf"), getCol(row, "Patient_Natel"));
					String land = getCol(row, "Patient_Nationalite");
					StringBuilder sb = new StringBuilder();
					if (country2code.get(land) != null) {
						pat.set("Land", country2code.get(land));
					} else {
						appendIfNotEmpty(sb, "nationalité:", land);
					}
					// 		String risks = p.get(Patient.FLD_RISKS); //$NON-NLS-1$
					appendIfNotEmpty(sb, "dossier: ", ID);
					appendIfNotEmpty(sb, "profession: ", getCol(row, "Patient_Profession"));
					appendIfNotEmpty(sb, "remarque: ", getCol(row, "Patient_Comment"));
					String alarm = getCol(row, "Patient_Commentaire_ALARME_T074");
					if (!alarm.contains("false") && !alarm.equals("")) {
						if (getAlarme(alarm) != null) {
							appendIfNotEmpty(sb, "alarme: ", getAlarme(alarm).toString());
							System.out.println("alarme: " + getAlarme(alarm).toString());
						}
					}
					appendIfNotEmpty(sb, "employeur: ", getCol(row, "Patient_Employeur"));
					appendIfNotEmpty(sb, "medecin traitant: ",
						getCol(row, "Patient_Medecin_Traitant"));
					if (sb.length() > 0) {
						pat.setBemerkung(sb.toString());
					}
					if (email.length() > 80)
						email = email.substring(0, 79);
					pat.set(new String[] {
						"ID", "E-Mail", "Telefon1", "Telefon2", "Natel", "Anschrift"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
						ID, email, tel1, tel2, natel, zusatz);
					pat.set("Zusatz", bez2); //$NON-NLS-1$
					if (titel.length() > 20)
						titel = titel.substring(0, 19);
					pat.set("Titel", titel); //$NON-NLS-1$
					if (monitor.isCanceled()) {
						return false;
					}
					if (counter++ > 200) {
						PersistentObject.clearCache();
						System.gc();
						try {
							Thread.sleep(100);
						} catch (Exception ex) {
							// no worries
						}
						counter = 0;
					}
					pat.addXid(PATID, ID, false);
					nrDone++;
					if (nrDone > maxRecordsToImport)
						return true;
				}
			} else {
				System.out.println("ADRES_MED_T009 nicht gefunden");
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
		return false; //$NON-NLS-1$		
	}
	
	private boolean importDoctors(IProgressMonitor monitor, String tableName){
		monitor.subTask("Importations des médecins");
		int counter = 0;
		try {
			Table t = db.getTable(tableName);
			if (t != null) {
				int num = t.getRowCount();
				final int PORTION = Math.round(TOTALWORK / num);
				System.out.println("importing " + num + " rows");
				System.out.println(t.display(maxRecordsToDisplay));
				Iterator<Map<String, Object>> it = t.iterator();
				while (it.hasNext()) {
					monitor.worked(1);
					Map<String, Object> row = it.next();
					counter++;
					// System.out.println("importDoctors " + counter + " " + row);
					String ID = getCol(row, ("AdresMed_ID"));
					if (ID.equals(""))
						continue; // wir können keine Schrott importieren
					ID = "ARZT_" + ID;
					String titel = getCol(row, ("AdresMed_Titre"));
					String vorname = getCol(row, ("AdresMed_Prenom"));
					String name = getCol(row, ("AdresMed_Nom"));
					String bez2 = getCol(row, ("AdresMed_Specialisation"));
					String EAN = getCol(row, ("AdresMed_Code_EAN"));
					String strasse = getCol(row, ("AdresMed_Adresse"));
					String plz = getPlzOrt(getCol(row, ("AdresMed_Localite_ID"))).plz;
					String ort = getPlzOrt(getCol(row, ("AdresMed_Localite_ID"))).ort;
					String email = getCol(row, ("AdresMed_Email"));
					String tel1 = getCol(row, ("AdresMed_Tel"));
					String tel2 = getCol(row, ("AdresMed_TelConfid"));
					String natel = getCol(row, ("AdresMed_Natel"));
					String zusatz = getCol(row, ("AdresMed_Case_Postale"));
					Kontakt k = null;
					monitor.subTask("Médecin: " + name + " " + vorname);
					k =
						KontaktMatcher.findPerson(name, vorname, "", "m", strasse, plz, ort, natel,
							KontaktMatcher.CreateMode.CREATE);
					if (k == null) {
						continue; // Error creating contact
					}
					k.set("Titel", titel); //$NON-NLS-1$
					k.set("Zusatz", bez2); //$NON-NLS-1$
					k.set(new String[] {
						"ID", "E-Mail", "Telefon1", "Telefon2", "Natel", "Anschrift"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
						ID, email, tel1, tel2, natel, zusatz);
					if (EAN.matches("[0-9]{13,13}")) { //$NON-NLS-1$
						k.addXid(Xid.DOMAIN_EAN, EAN, true);
					}
					if (monitor.isCanceled()) {
						return false;
					}
					if (counter++ > 200) {
						PersistentObject.clearCache();
						System.gc();
						try {
							Thread.sleep(100);
						} catch (Exception ex) {
							// no worries
						}
						counter = 0;
					}
					
					if (Xid.findObject(ARZTID, ID) != null) {
						continue;
					}
					monitor.worked(PORTION);
					if (counter > maxRecordsToImport)
						return true;
				}
			} else {
				System.out.println("ADRES_MED_T009 nicht gefunden");
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
		return false; //$NON-NLS-1$		
	}
	
	/*
	 * public Prescription(Prescription other){ String[] fields=new
	 * String[]{ARTICLE,PATIENT_ID,DOSAGE,REMARK,ARTICLE_ID}; String[] vals=new
	 * String[fields.length]; if(other.get(fields, vals)){ create(null); set(fields,vals);
	 * addTerm(new TimeTool(), vals[2]); } }
	 */
	private boolean importPresciption(IProgressMonitor monitor, String tableName){
		monitor.subTask("importations des ordonnances");
		int counter = 0;
		try {
			Table t = db.getTable(tableName);
			if (t != null) {
				int num = t.getRowCount();
				final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
				System.out.println("importing " + num + " rows");
				System.out.println(t.display(maxRecordsToDisplay));
				return true;
				/*
				 * Iterator<Map<String, Object>> it = t.iterator(); while (it.hasNext()) {
				 * Map<String, Object> row = it.next(); counter++; System.out.println("import " +
				 * tableName + " " + counter + " " + row); String ID = getCol(row,
				 * ("Patient_NrDossier")); // Patient_ID_Nr ist fortlaufende Nummer von KeyCab if
				 * (Xid.findObject(PATID, ID) != null) { log.log("Skipped " + ID, Log.DEBUGMSG);
				 * continue; // avoid multiple imports } monitor.worked(1); if
				 * (monitor.isCanceled()) { return false; } if (counter++ > 200) {
				 * PersistentObject.clearCache(); System.gc(); try { Thread.sleep(100); } catch
				 * (Exception ex) { // no worries } counter = 0; }
				 * 
				 * if (Xid.findObject(ARZTID, ID) != null) { continue; } monitor.worked(PORTION); if
				 * (counter > maxRecordsToImport) return true; }
				 */
			} else {
				System.out.println("ADRES_MED_T009 nicht gefunden");
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return false;
		}
		return false; //$NON-NLS-1$		
	}
	
	@Override
	public IStatus doImport(final IProgressMonitor monitor) throws Exception{
		String dateiName = results[0];
		String[] tableNames =
			{
				"_____Prestation", "T_DIAGNOSTIC_T051", "T_DIAGNOSTIC_T054", "T_LOCALITE_T023",
				"T_MEDICAMENT_T027", "T_NATIONALITE_T046", "T_POSOLOGIE_T070", "T_PROFESSION_T026",
				"T_SPECIALISATION_T033", "T_TITRE_T024", "AGENDA_CALENDRIER_T050", "AGENDA_T048",
				"ASSURANCE_T015", "BANKING_Payement_SAVE", "BANKING_TMP_SAVE", "CERTIFICAT_T018",
				"DOSSIER_MED_01_T022", "FACTURE_DATE_EDITION_FACTURE_RAPPEL_T047", "FACTURE_T003",
				"FACTURE_T073_Histo_Acquittements_Partiels", "FICHIER_EXT_T014",
				"HISTO_AGENDA_T048", "HSCI_EAN_Assurances", "ORDONNANCE_Fiche_T040",
				"ORDONNANCE_Journal_T019", "PATIENT_ASSURANCE_T016",
				"PATIENT_COMMENTAIRE_ALARME_T074", "PATIENT_T001", "PRESTATIONS_T005",
				"RH_VACANCES_CALENDRIER_T035", "TRAITEMENT_MEDICAMENTAUX",
			};
		db = Database.open(new File(dateiName));
		
		if (false)
			for (String tName : tableNames) {
				showInfo(tName);
			}
		
		monitor
			.beginTask("Importations des donneés de Keycab: " + dateiName, Math.round(TOTALWORK));
		initPlzData();
		initAlarme();
		importDoctors(monitor, "ADRES_MED_T009");
		importPatients(monitor, "PATIENT_T001");
		// importPresciption(monitor, "ORDONNANCE_Journal_T019");
		db.close();
		
		return Status.OK_STATUS;
	}
	
	@Override
	public String getDescription(){
		return "Importation des données Keycab";
	}
	
	@Override
	public String getTitle(){
		return "Keycab";
	}
}
