package ch.elexis.db.connection;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import ch.elexis.db.Laboritem;
import ch.elexis.db.Laboritem.Typ;
import ch.elexis.db.Laborwert;
import ch.elexis.db.Patient;
import ch.elexis.export.Messages;
import ch.elexis.export.hl7.LastUpdate;
import ch.elexis.export.hl7.Logger;

public class ElexisDatabase extends AbstractConnection {
	private static final String PATIENT_FLD_ID = "ID"; //$NON-NLS-1$
	private static final String PATIENT_FLD_LASTUPDATE = "LASTUPDATE"; //$NON-NLS-1$
	private static final String PATIENT_FLD_PATNR = "PATIENTNR"; //$NON-NLS-1$
	private static final String PATIENT_FLD_NACHNAME = "BEZEICHNUNG1"; //$NON-NLS-1$
	private static final String PATIENT_FLD_VORNAME = "BEZEICHNUNG2"; //$NON-NLS-1$
	private static final String PATIENT_FLD_STRASSE = "STRASSE"; //$NON-NLS-1$
	private static final String PATIENT_FLD_OTHER = "BEZEICHNUNG3"; //$NON-NLS-1$
	private static final String PATIENT_FLD_PLZ = "PLZ"; //$NON-NLS-1$
	private static final String PATIENT_FLD_ORT = "ORT"; //$NON-NLS-1$
	private static final String PATIENT_FLD_TELEFON1 = "TELEFON1"; //$NON-NLS-1$
	private static final String PATIENT_FLD_TELEFON2 = "TELEFON2"; //$NON-NLS-1$
	private static final String PATIENT_FLD_NATELNR = "NATELNR"; //$NON-NLS-1$
	private static final String PATIENT_FLD_FAX = "FAX"; //$NON-NLS-1$
	private static final String PATIENT_FLD_EMAIL = "EMAIL"; //$NON-NLS-1$
	private static final String PATIENT_FLD_GESCHLECHT = "GESCHLECHT"; //$NON-NLS-1$
	private static final String PATIENT_FLD_GEBDATUM = "GEBURTSDATUM"; //$NON-NLS-1$
	private static final String PATIENT_FLD_LAND = "LAND"; //$NON-NLS-1$
	private static final String PATIENT_FLD_TITEL = "TITEL"; //$NON-NLS-1$
	
	private static final String LABORWERTE_FLD_ID = "ID"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_LASTUPDATE = "LASTUPDATE"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_PATIENT_ID = "PATIENTID"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_LABITEM_ID = "ITEMID"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_RESULTAT = "RESULTAT"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_DATUM = "DATUM"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_ORIGIN = "ORIGIN"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_FLAGS = "FLAGS"; //$NON-NLS-1$
	private static final String LABORWERTE_FLD_KOMMENTAR = "KOMMENTAR"; //$NON-NLS-1$
	
	private static final String LABORITEMS_FLD_ID = "ID"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_LASTUPDATE = "LASTUPDATE"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_KUERZEL = "KUERZEL"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_TITEL = "TITEL"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_REFMANN = "REFMANN"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_REFFRAUORTX = "REFFRAUORTX"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_EINHEIT = "EINHEIT"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_GRUPPE = "GRUPPE"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_PRIO = "PRIO"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_TYP = "TYP"; //$NON-NLS-1$
	private static final String LABORITEMS_FLD_EXPORT = "EXPORT"; //$NON-NLS-1$
	
	private static final String DATE_FORMAT_ELEXIS = "yyyyMMdd"; //$NON-NLS-1$
	private static final String FEMALE_ELEXIS = "w"; //$NON-NLS-1$
	private static final String MALE_ELEXIS = "m"; //$NON-NLS-1$
	
	// XID content
	public static final String DOMAIN_CSV_IMPORT_PATID = "medshare.ch/csv_import/PatID"; //$NON-NLS-1$
	public static final String DOMAIN_CSV_IMPORT_AHV = "www.ahv.ch/xid"; //$NON-NLS-1$
	public static final String TYPE_CSV_IMPORT_PATIENT = "ch.elexis.data.Patient"; //$NON-NLS-1$
	
	// XID Field names
	public static final String FLD_XID_DOMAIN_ID = "DOMAIN_ID"; //$NON-NLS-1$
	public static final String FLD_XID_DOMAIN = "DOMAIN"; //$NON-NLS-1$
	public static final String FLD_XID_OBJECT = "OBJECT"; //$NON-NLS-1$
	public static final String FLD_XID_QUALITY = "QUALITY"; //$NON-NLS-1$
	public static final String FLD_XID_TYPE = "TYPE"; //$NON-NLS-1$
	
	public ElexisDatabase(final String driverName, final String url, final String user,
		final String pwd) throws ClassNotFoundException, SQLException{
		super();
		openConnection(driverName, url, user, pwd);
	}
	
	/**
	 * Liest XID Eintrag (Domain Id) für einen Patient
	 * 
	 * @param id
	 *            Technische Id des Patienten
	 * @param domain
	 *            Name des XID Domains
	 * @return Domanin-Id oder null falls kein XID Eintrag vorhanden ist
	 * @throws SQLException
	 */
	private String getXIDDomainIdForPatient(final String id, final String domain)
		throws SQLException{
		
		// XID Tabelle
		// TYPE = ch.elexis.data.Patient
		// DOMAIN = medshare.ch/csv_import/PatID
		// OBJECT = id (technische)
		// DOMAIN_ID = patientNr
		// QUALITY = je höher umso besser
		List<DBRow> rowList =
			select(MessageFormat.format("SELECT * FROM XID WHERE {0}=''{1}'' AND {2}=''{3}''", //$NON-NLS-1$
				FLD_XID_DOMAIN, domain, FLD_XID_OBJECT, id));
		
		if (rowList == null || rowList.size() == 0) {
			// Keine XID Eintrag.
			return null;
		}
		
		int lastQuality = 0;
		String domainId = null;
		for (DBRow row : rowList) {
			String qualityStr = (String) row.getString(FLD_XID_QUALITY);
			int quality = Integer.parseInt(qualityStr);
			if (quality > lastQuality) {
				lastQuality = quality;
				domainId = (String) row.getString(FLD_XID_DOMAIN_ID);
			}
		}
		
		return domainId;
	}
	
	/**
	 * Id für Patient wird gelesen. Falls kein Eintrag in der Tabelle XID gefunden wird, dann wird
	 * die Patienten-Nr retourniert.
	 * 
	 * @return Id des Patienten
	 */
	private String getIdForPatient(final String id, final String patNr) throws SQLException{
		
		String domainId = getXIDDomainIdForPatient(id, DOMAIN_CSV_IMPORT_PATID);
		
		if (domainId == null) {
			return patNr;
		}
		
		return domainId;
	}
	
	/**
	 * AHV für Patient wird gelesen aus Tabelle XID gelesen.
	 * 
	 * @return AHV des Patienten
	 */
	private String getAHVForPatient(final String id) throws SQLException{
		return getXIDDomainIdForPatient(id, DOMAIN_CSV_IMPORT_AHV);
	}
	
	/**
	 * Liest Patient anhand Primary Key aus Elexis Datenbank
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public Patient readPatient(final String id) throws SQLException{
		if (id == null) {
			return null;
		}
		
		// Patienten suchen
		List<DBRow> elexisPatList =
			select(MessageFormat.format("SELECT * FROM KONTAKT WHERE ID = ''{0}''", //$NON-NLS-1$
				id));
		
		if (elexisPatList.size() > 0) {
			// Suche über Primary Key retourniert nur 1 Datensatz
			DBRow row = elexisPatList.get(0);
			Patient patient = new Patient();
			patient.setId(row.getString(PATIENT_FLD_ID));
			patient.setLastUpdate(row.getLong(PATIENT_FLD_LASTUPDATE));
			patient.setNachname(row.getString(PATIENT_FLD_NACHNAME));
			patient.setVorname(row.getString(PATIENT_FLD_VORNAME));
			patient.setStrasse(row.getString(PATIENT_FLD_STRASSE));
			patient.setPlz(row.getString(PATIENT_FLD_PLZ));
			patient.setOrt(row.getString(PATIENT_FLD_ORT));
			patient.setTelefon1(row.getString(PATIENT_FLD_TELEFON1));
			patient.setTelefon2(row.getString(PATIENT_FLD_TELEFON2));
			patient.setNatelnr(row.getString(PATIENT_FLD_NATELNR));
			patient.setFax(row.getString(PATIENT_FLD_FAX));
			patient.setEmail(row.getString(PATIENT_FLD_EMAIL));
			patient.setTitel(row.getString(PATIENT_FLD_TITEL));
			patient.setLand(row.getString(PATIENT_FLD_LAND));
			patient.setOther(row.getString(PATIENT_FLD_OTHER));
			
			String sex = row.getString(PATIENT_FLD_GESCHLECHT);
			if (sex != null) {
				if (sex.toLowerCase().trim().equals(FEMALE_ELEXIS)) {
					patient.setSex(Patient.FEMALE);
				} else if (sex.toLowerCase().trim().equals(MALE_ELEXIS)) {
					patient.setSex(Patient.MALE);
				}
			}
			patient.setGebDatum(row.getDate(PATIENT_FLD_GEBDATUM, DATE_FORMAT_ELEXIS));
			String ahv = getAHVForPatient(row.getString(PATIENT_FLD_ID));
			patient.setAhv(ahv);
			
			String patId =
				getIdForPatient(row.getString(PATIENT_FLD_ID), row.getString(PATIENT_FLD_PATNR));
			if (patId == null) {
				Logger.logInfo(MessageFormat.format(
					Messages.getString("ElexisDatabase.errorPatientKeineNummer"), //$NON-NLS-1$
					patient.getNachname(), patient.getVorname()));
			} else {
				patient.setPatId(patId);
				return patient;
			}
		}
		
		return null;
	}
	
	/**
	 * Liest Laboritem anhand Primary Key aus Elexis Datenbank
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public Laboritem readLaborItem(final String id) throws SQLException{
		if (id == null) {
			return null;
		}
		// Patienten suchen
		List<DBRow> elexisLabItemList =
			select(MessageFormat.format("SELECT * FROM LABORITEMS WHERE ID = ''{0}''", //$NON-NLS-1$
				id));
		
		Laboritem laboritem = null;
		if (elexisLabItemList.size() > 0) {
			// Suche über Primary Key retourniert nur 1 Datensatz
			DBRow row = elexisLabItemList.get(0);
			laboritem = new Laboritem();
			laboritem.setId(row.getString(LABORITEMS_FLD_ID));
			laboritem.setLastUpdate(row.getLong(LABORITEMS_FLD_LASTUPDATE));
			laboritem.setKuerzel(row.getString(LABORITEMS_FLD_KUERZEL));
			laboritem.setEinheit(row.getString(LABORITEMS_FLD_EINHEIT));
			laboritem.setTitel(row.getString(LABORITEMS_FLD_TITEL));
			laboritem.setGruppe(row.getString(LABORITEMS_FLD_GRUPPE));
			laboritem.setPrio(row.getString(LABORITEMS_FLD_PRIO));
			laboritem.setRefMann(row.getString(LABORITEMS_FLD_REFMANN));
			laboritem.setRefFrau(row.getString(LABORITEMS_FLD_REFFRAUORTX));
			
			final String typStr = row.getString(LABORITEMS_FLD_TYP);
			try {
				int typIndex = Integer.parseInt(typStr);
				laboritem.setTyp(Typ.values()[typIndex]);
			} catch (NumberFormatException e) {
				Logger.logError("Typ ist keine Zahl. Verwende Defaulttyp = NUMERIC (0)", e);
				laboritem.setTyp(Typ.NUMERIC);
			}
		}
		
		return laboritem;
	}
	
	/**
	 * Liest Laborwerte für Export aus der Elexis Datenbank
	 * 
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public List<Laborwert> readLaborWerte(final String matchingExportTag) throws SQLException,
		IOException{
		// Patienten suchen
		List<DBRow> elexisPatList =
			select(MessageFormat.format("SELECT * FROM LABORWERTE wert, LABORITEMS item"
				+ " WHERE wert.LASTUPDATE > {0} AND wert.ITEMID = item.ID "
				+ " AND item.EXPORT IS NOT NULL ORDER BY wert.LASTUPDATE ASC", //$NON-NLS-1$
				LastUpdate.readLastUpdate()));
		
		// Patient Objekte erstellen
		List<Laborwert> laborwertList = new Vector<Laborwert>();
		for (DBRow row : elexisPatList) {
			String exportTag = row.getString(LABORITEMS_FLD_EXPORT);
			if (matchingExportTag != null
				&& exportTag.toLowerCase().contains(matchingExportTag.toLowerCase())) {
				String labItemId = row.getString(LABORWERTE_FLD_LABITEM_ID);
				String patientId = row.getString(LABORWERTE_FLD_PATIENT_ID);
				Patient patient = readPatient(patientId);
				if (patient != null) {
					Laborwert laborwert = new Laborwert();
					laborwert.setId(row.getString(LABORWERTE_FLD_ID));
					laborwert.setLastUpdate(row.getLong(LABORWERTE_FLD_LASTUPDATE));
					laborwert.setOrigin(row.getString(LABORWERTE_FLD_ORIGIN));
					laborwert.setKommentar(row.getString(LABORWERTE_FLD_KOMMENTAR));
					laborwert.setResultat(row.getString(LABORWERTE_FLD_RESULTAT));
					laborwert.setFlags(row.getInteger(LABORWERTE_FLD_FLAGS));
					
					Date datum = row.getDate(LABORWERTE_FLD_DATUM, DATE_FORMAT_ELEXIS);
					laborwert.setDatum(datum);
					
					laborwert.setLaborItem(readLaborItem(labItemId));
					laborwert.setPatient(patient);
					
					laborwertList.add(laborwert);
				}
			}
		}
		
		return laborwertList;
	}
}
