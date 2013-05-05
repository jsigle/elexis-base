/*******************************************************************************
 * Copyright (c) 2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.importer.praxistar;

import java.sql.ResultSet;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.data.Anwender;
import ch.elexis.data.Fall;
import ch.elexis.data.Mandant;
import ch.elexis.data.Organisation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Xid;
import ch.elexis.icpc.Episode;
import ch.elexis.tarmedprefs.TarmedRequirements;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.JdbcLink.Stm;

public class Importer extends ImporterPage {
	private static final float TOTALWORK = 100000;
	private static final float WORK_PORTIONS = 5;
	
	public static final String PLUGINID = "ch.elexis.importer.praxistar";
	
	// we'll use these local XID's to reference the external data
	private final static String IMPORT_XID = "elexis.ch/praxistar_import";
	private final static String PATID = IMPORT_XID + "/PatID";
	private final static String GARANTID = IMPORT_XID + "/garantID";
	private final static String ARZTID = IMPORT_XID + "/arztID";
	private final static String USERID = IMPORT_XID + "/userID";
	
	private JdbcLink j;
	private Stm stm;
	
	static {
		Fall.getAbrechnungsSysteme(); // make sure billing systems are initialized
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
		DBBasedImporter dbi = new DBBasedImporter(parent, this);
		dbi.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return dbi;
	}
	
	@Override
	public IStatus doImport(final IProgressMonitor monitor) throws Exception{
		if (!connect()) {
			return new Status(Log.ERRORS, PLUGINID, "Verbindung nicht möglich");
		}
		monitor.beginTask("Importiere PraxiStar", Math.round(TOTALWORK));
		stm = j.getStatement();
		try {
			importMandanten(monitor);
			importAerzte(monitor);
			importGaranten(monitor);
			importPatienten(monitor);
			importDiagnosen(monitor);
		} catch (Exception ex) {
			return new Status(Log.ERRORS, PLUGINID, ex.getMessage());
		} finally {
			j.releaseStatement(stm);
		}
		return Status.OK_STATUS;
	}
	
	@Override
	public String getDescription(){
		return "Import PraxiStar Stammdaten";
	}
	
	@Override
	public String getTitle(){
		return "PraxiStar";
	}
	
	private void importMandanten(final IProgressMonitor moni) throws Exception{
		moni.subTask("importiere Mandanten");
		int num = stm.queryInt("SELECT COUNT(*) FROM Adressen_Mandanten");
		num += stm.queryInt("SELECT COUNT(*) FROM ADRESSEN_PERSONAL");
		final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
		HashMap<String, Anwender> users = new HashMap<String, Anwender>();
		ResultSet res = stm.query("SELECT * FROM ADRESSEN_PERSONAL");
		String userid;
		while ((res != null) && res.next()) {
			HashMap<String, String> row =
				fetchRow(res, new String[] {
					"ID_Personal", "tx_Anrede", "tx_Titel", "tx_Name", "tx_Vorname",
					"dt_Geburtsdatum", "Geschlecht_ID", "tx_Strasse", "tx_PLZ", "tx_Ort",
					"tx_User", "tx_Kennwort"
				});
			userid = row.get("ID_Personal");
			if (Xid.findObject(USERID, userid) != null) {
				continue;
			}
			Anwender an = new Anwender(row.get("tx_User"), row.get("tx_Kennwort"));
			an.addXid(USERID, userid, false);
			an.set(new String[] {
				"Titel", "Name", "Vorname", "Geburtsdatum", "Geschlecht", "Strasse", "Plz", "Ort"
			}, row.get("tx_Titel"), row.get("tx_Name"), row.get("tx_Vorname"), new TimeTool(row
				.get("dt_Geburtsdatum").split(" ")[0]).toString(TimeTool.DATE_GER),
				row.get("Geschlecht_ID").equals("1") ? "m" : "w", row.get("tx_Strasse"), row
					.get("tx_PLZ"), row.get("tx_Ort"));
			users.put(row.get("ID_Personal"), an);
		}
		
		res = stm.query("SELECT * FROM Adressen_Mandanten");
		while ((res != null) && res.next()) {
			HashMap<String, String> row =
				fetchRow(res, new String[] {
					"ID_Mandant", "tx_Mandant", "tx_Name", "tx_Vorname", "tx_Anrede", "tx_Titel",
					"tx_Strasse", "tx_PLZ", "tx_Ort", "tx_Fachgebiet", "Telefon_1", "Telefon_2",
					"Telefon_N", "Telefax", "Konkordats_Nr", "tx_NIFNr", "tx_EANNr"
				});
			if (Xid.findObject(Xid.DOMAIN_EAN, row.get("tx_EANNr")) != null) {
				continue;
			}
			userid = row.get("ID_Mandant");
			Anwender an = users.get(userid);
			if (an != null) {
				an.set("istMandant", "1");
				Mandant m = Mandant.load(an.getId());
				m.set(new String[] {
					"Telefon1", "Telefon2", "Natel", "Fax"
				}, row.get("Telefon_1"), row.get("Telefon_2"), row.get("Telefon_N"),
					row.get("Telefax"));
				m.addXid(Xid.DOMAIN_EAN, row.get("tx_EANNr"), false);
				m.addXid(TarmedRequirements.DOMAIN_KSK, row.get("Konkordats_Nr"), false);
				m.addXid(TarmedRequirements.DOMAIN_NIF, row.get("tx_NIFNr"), false);
			}
			moni.worked(PORTION);
		}
	}
	
	private void importGaranten(final IProgressMonitor moni) throws Exception{
		moni.subTask("importiere Garanten");
		int num = stm.queryInt("SELECT COUNT(*) FROM Adressen_Versicherungen");
		final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
		ResultSet res = stm.query("SELECT * FROM Adressen_Versicherungen");
		while ((res != null) && res.next()) {
			String id = res.getString("ID_Versicherung");
			String name = res.getString("tx_Name");
			if (Xid.findObject(GARANTID, id) != null) {
				continue;
			}
			Organisation o = new Organisation(name, "Versicherung");
			o.set(new String[] {
				"Strasse", "Plz", "Ort", "Telefon1", "Fax"
			}, StringTool.unNull(res.getString("tx_Strasse")),
				StringTool.unNull(res.getString("tx_PLZ")),
				StringTool.unNull(res.getString("tx_Ort")),
				StringTool.unNull(res.getString("tx_Telefon")),
				StringTool.unNull(res.getString("tx_Fax")));
			moni.subTask(name);
			o.addXid(GARANTID, id, false);
			String ean = res.getString("tx_EANNr");
			if (!StringTool.isNothing(ean)) {
				o.setInfoElement("EAN", ean);
				// o.addXid(Xid.DOMAIN_EAN, ean, false); Insurances do not have unique EAN's ->
				// revise XID
			}
			o.set("Ansprechperson", StringTool.unNull(res.getString("tx_ZuHanden")));
			o.set("Kuerzel", StringTool.limitLength("KK" + name, 39));
			moni.worked(PORTION);
		}
	}
	
	private void importAerzte(final IProgressMonitor moni) throws Exception{
		
		moni.subTask("importiere Ärzte");
		int num = stm.queryInt("SELECT COUNT(*) FROM Adressen_Ärzte");
		final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
		ResultSet res = stm.query("SELECT * FROM Adressen_Ärzte");
		while ((res != null) && res.next()) {
			// fetch all columns in given order to avoid funny error messages from
			// odbc driver
			String[] row = new String[36];
			for (int i = 0; i < 36; i++) {
				row[i] = StringTool.unNull(res.getString(i + 1));
			}
			String anrede = row[3];
			String name = row[4].length() > 0 ? row[4] : "??";
			String vorname = row[5].length() > 0 ? row[5] : " ";
			
			String geschlecht = StringTool.isFemale(vorname) ? "w" : "m";
			if (!StringTool.isNothing(anrede)) {
				geschlecht = anrede.startsWith("Her") ? "m" : "w";
			}
			
			String id = row[0];
			if (Xid.findObject(ARZTID, id) != null) {
				continue;
			}
			Person p = new Person(name, vorname, "", geschlecht);
			moni.subTask(new StringBuilder().append("Arzt: ").append(p.getLabel()).toString());
			p.set(new String[] {
				"Zusatz", "Titel", "Strasse", "Plz", "Ort", "Telefon1", "Telefon2", "Natel", "Fax"
			}, row[7], row[6], row[9], row[10], StringTool.normalizeCase(row[11]), row[12],
				row[13], row[15], row[16]);
			p.set("Anschrift", createAnschrift(p));
			p.set("Kuerzel", "Az" + name.substring(0, 1) + vorname.substring(0, 1));
			p.addXid(ARZTID, id, false);
			moni.worked(PORTION);
		}
	}
	
	private String createAnschrift(final Person p){
		StringBuilder sb = new StringBuilder();
		String salutation;
		if (p.getGeschlecht().equals("m")) {
			salutation = "Herr";
		} else {
			salutation = "Frau";
		}
		sb.append(salutation);
		sb.append("\n");
		
		String titel = p.get("Titel");
		if (!StringTool.isNothing(titel)) {
			sb.append(titel).append(" ");
		}
		sb.append(p.getVorname()).append(" ").append(p.getName()).append("\n")
			.append(p.get("Zusatz")).append("\n");
		sb.append(p.getAnschrift().getEtikette(false, true));
		return sb.toString();
		
	}
	
	private void importPatienten(final IProgressMonitor moni) throws Exception{
		moni.subTask("Importiere Patienten");
		int num = stm.queryInt("SELECT COUNT(*) FROM Patienten_Personalien");
		final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
		ResultSet res = stm.query("SELECT * FROM Patienten_Personalien");
		int count = 0;
		
		while ((res != null) && res.next()) {
			HashMap<String, String> row =
				fetchRow(res, new String[] {
					"ID_Patient", "Mandant_ID", "tx_Name", "tx_Vorname", "tx_Geburtsdatum",
					"tx_Anrede", "tx_Strasse", "tx_PLZ", "tx_Ort", "tx_TelefonP", "tx_TelefonN",
					"Geschlecht_ID", "Zivilstand_ID", "tx_Titel", "tx_Arbeitgeber", "tx_Beruf",
					"tx_TelefonG", "tx_ZuwArzt", "tx_Hausarzt", "mo_Bemerkung", "KK_Garant_ID",
					"tx_KK_MitgliedNr", "UVG_Garant_ID", "tx_UVG_MitgliedNr", "tx_AHV_Nr",
					"tx_fakt_Anrede", "tx_fakt_Name", "tx_fakt_Vorname", "tx_fakt_Strasse",
					"tx_fakt_PLZ", "tx_fakt_Ort"
				});
			
			String name = StringTool.normalizeCase(row.get("tx_Name"));
			String vorname = row.get("tx_Vorname");
			String gebdat = row.get("tx_Geburtsdatum").split(" ")[0];
			Anwender an = (Anwender) Xid.findObject(USERID, row.get("Mandant_ID"));
			String patid = row.get("ID_Patient");
			log.log(name, Log.DEBUGMSG);
			if (Xid.findObject(PATID, patid) != null) {
				log.log("Skipped", Log.DEBUGMSG);
				continue; // avoid multiple imports
			}
			String[] land_plz = row.get("tx_PLZ").split("[ -]+");
			String plz = land_plz[0];
			String land = "";
			if (land_plz.length > 1) {
				plz = land_plz[1];
				land = land_plz[0];
			}
			
			Patient pat =
				new Patient(name, vorname, new TimeTool(gebdat).toString(TimeTool.DATE_GER), row
					.get("Geschlecht_ID").equals("1") ? "m" : "w");
			pat.set("PatientNr", patid);
			moni.subTask(new StringBuilder().append("Patient: ").append(pat.getLabel()).append(" ")
				.append(pat.getPatCode()).toString());
			pat.set(new String[] {
				"Strasse", "Land", "Plz", "Ort", "Telefon1", "Telefon2", "Natel", "Titel"
			}, row.get("tx_Strasse"), StringTool.limitLength(land, 3),
				StringTool.limitLength(plz, 5), row.get("tx_Ort"), row.get("tx_TelefonP"),
				row.get("tx_TelefonG"), row.get("tx_TelefonN"), row.get("tx_Titel"));
			StringBuilder sb = new StringBuilder();
			appendIfNotEmpty(sb, "Beruf: ", row.get("tx_Beruf"));
			appendIfNotEmpty(sb, "Bemerkung: ", row.get("mo_Bemerkung"));
			appendIfNotEmpty(sb, "Hausarzt: ", row.get("tx_Hausarzt"));
			appendIfNotEmpty(sb, "Arbeitgeber", row.get("tx_Arbeitgeber"));
			appendIfNotEmpty(sb, "Zuweisender Arzt: ", row.get("tx_ZuwArzt"));
			if (sb.length() > 0) {
				pat.setBemerkung(sb.toString());
			}
			if ((an != null) && an.isValid()) {
				pat.set("Gruppe", an.get("Kuerzel"));
			}
			if (count++ > 500) {
				PersistentObject.clearCache();
				System.gc();
				count = 0;
			}
			pat.addXid(PATID, row.get("ID_Patient"), false);
			String kkid = row.get("KK_Garant_ID");
			if (!StringTool.isNothing(kkid)) {
				Organisation kk = (Organisation) Xid.findObject(GARANTID, kkid);
				if ((kk != null) && kk.isValid()) {
					Fall fall = pat.neuerFall(Fall.getDefaultCaseLabel(), Fall.TYPE_DISEASE, "KVG");
					fall.setRequiredContact(TarmedRequirements.INSURANCE, kk);
					fall.setRequiredString(TarmedRequirements.INSURANCE_NUMBER,
						row.get("tx_KK_MitgliedNr"));
				}
				
			}
			String uvgid = row.get("UVG_Garant_ID");
			if (!StringTool.isNothing(uvgid)) {
				Organisation uvg = (Organisation) Xid.findObject(GARANTID, uvgid);
				if ((uvg != null) && uvg.isValid()) {
					Fall fall =
						pat.neuerFall(Fall.getDefaultCaseLabel(), Fall.TYPE_ACCIDENT, "UVG");
					fall.setRequiredContact(TarmedRequirements.INSURANCE, uvg);
					fall.setRequiredString(TarmedRequirements.ACCIDENT_NUMBER,
						row.get("tx_UVG_MitgliedNr"));
				}
			}
			moni.worked(PORTION);
		}
		
	}
	
	private void importDiagnosen(final IProgressMonitor moni) throws Exception{
		moni.subTask("Importiere Stammdiagnosen");
		int num = stm.queryInt("SELECT COUNT(*) FROM Patienten_Stammdiagnose");
		final int PORTION = Math.round((TOTALWORK / WORK_PORTIONS) / num);
		ResultSet res = stm.query("SELECT * FROM Patienten_Stammdiagnose");
		
		while ((res != null) && res.next()) {
			HashMap<String, String> row =
				fetchRow(res, new String[] {
					"Patient_ID", "tx_Diagnosentext", "tx_Rechnungstext", "dt_DiagnosenStartdatum",
					"tx_ICD"
				});
			Patient pat = (Patient) Xid.findObject(PATID, row.get("Patient_ID"));
			if (pat != null) {
				Episode problem = new Episode(pat, row.get("tx_Diagnosentext"));
				pat.set("Diagnosen", problem.getTitle());
				problem.setStartDate(new TimeTool(row.get("dt_DiagnosenStartdatum").split(" ")[0])
					.toString(TimeTool.DATE_GER));
				String dgString = "ch.elexis.data.TICode::" + row.get("tx_Rechnungstext");
				problem.setExtField("Diagnosen", dgString);
			}
			moni.worked(PORTION);
		}
		res.close();
		
	}
	
	private void appendIfNotEmpty(final StringBuilder sb, final String title, final String value){
		if (!StringTool.isNothing(value)) {
			sb.append(title).append(value).append("\n");
		}
	}
	
	public boolean connect(){
		String type = results[0];
		if (type != null) {
			String server = results[1];
			String db = results[2];
			String user = results[3];
			String password = results[4];
			
			if (type.equals("MySQL")) { //$NON-NLS-1$
				j = JdbcLink.createMySqlLink(server, db);
				return j.connect(user, password);
			} else if (type.equals("PostgreSQL")) { //$NON-NLS-1$
				j = JdbcLink.createPostgreSQLLink(server, db);
				return j.connect(user, password);
			} else if (type.equals("ODBC")) { //$NON-NLS-1$
				j = JdbcLink.createODBCLink(db);
				return j.connect(user, password);
			}
		}
		
		return false;
	}
	
	/**
	 * The ODBC driver sometimes fires funny exceptions if columns are not fetched in the native
	 * order. We circumvent this by converting the row into a hashmap.
	 * 
	 * @param res
	 *            A ResultSet pointing to the interesting row
	 * @param columns
	 *            the names of the columns
	 * @return a hashmap of ol columne values with the column name as key
	 */
	public static HashMap<String, String> fetchRow(final ResultSet res, final String[] columns)
		throws Exception{
		HashMap<String, String> ret = new HashMap<String, String>();
		for (String col : columns) {
			// System.out.println(col);
			ret.put(col, StringTool.unNull(res.getString(col)));
		}
		return ret;
	}
}
