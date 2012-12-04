package ch.elexis.export.hl7;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ch.elexis.db.Laborwert;
import ch.elexis.db.Patient;
import ch.elexis.db.connection.ElexisDatabase;
import ch.elexis.export.Messages;
import ch.elexis.export.tools.StringTool;
import ch.elexis.export.tools.UtilFile;
import ch.elexis.hl7.data.HL7LaborItem;
import ch.elexis.hl7.data.HL7LaborWert;
import ch.elexis.hl7.data.HL7Mandant;
import ch.elexis.hl7.data.HL7Patient;
import ch.elexis.hl7.model.ObservationMessage;
import ch.elexis.hl7.v26.HL7_ORU_R01;

public class Start {
	public static final DateFormat fileDateFormat = new SimpleDateFormat("ddMMyyyy"); //$NON-NLS-1$
	
	private String exportPath = "";
	private String exportTag = "";
	
	public Start(String[] args){
		checkArguments(args);
		Settings.getCurrent().showProperties(exportPath, exportTag);
	}
	
	/**
	 * Arguments: <export-Dir> <export-Tag>
	 * 
	 * @param args
	 */
	private void checkArguments(String[] args){
		exportPath = Settings.getCurrent().getProperty(Settings.EXPORT_PATH);
		exportTag = Settings.getCurrent().getProperty(Settings.EXPORT_TAG);
		if (args.length > 0) {
			exportPath = args[0];
		}
		if (args.length > 1) {
			exportTag = args[1];
		}
	}
	
	/**
	 * Starts export
	 * 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	private void run() throws ClassNotFoundException, SQLException, IOException{
		Settings settings = Settings.getCurrent();
		ElexisDatabase elexisDb = null;
		long lastupdate = 0;
		
		try {
			// Elexis Verbindung aufbauen
			Logger.logInfo(Messages.getString("Start.elexisDbConnect")); //$NON-NLS-1$
			String elexisDriver = settings.getString(Settings.ELEXIS_DB_DRIVER);
			String elexisUrl = settings.getString(Settings.ELEXIS_DB_URL);
			String elexisUser = settings.getString(Settings.ELEXIS_DB_USER);
			String elexisPwd = settings.getString(Settings.ELEXIS_DB_PWD);
			Logger.logDebug(MessageFormat.format(
				Messages.getString("Start.elexisConnectString"), elexisUrl, elexisUser, //$NON-NLS-1$
				elexisPwd));
			Logger.logInfo(MessageFormat.format(
				Messages.getString("Start.elexisDbConnection"), elexisUrl)); //$NON-NLS-1$
			elexisDb = new ElexisDatabase(elexisDriver, elexisUrl, elexisUser, elexisPwd);
			
			// Laboritems lesen
			Logger.logInfo(Messages.getString("Start.readLaborwerte")); //$NON-NLS-1$
			List<Laborwert> laborWerteList = elexisDb.readLaborWerte(exportTag);
			Logger.logInfo(MessageFormat.format(
				Messages.getString("Start.readLaborwerteResult"), laborWerteList.size())); //$NON-NLS-1$
			
			// HL7 Export starten
			HL7Mandant mandant = new HL7Mandant();
			mandant.setLabel(settings.getString(Settings.HL7_MANDANT_KUERZEL));
			mandant.setEan(settings.getString(Settings.HL7_MANDANT_EAN));
			
			// Receiving Application
			String receivingApp = settings.getString(Settings.HL7_RECEIVING_APPLICATION);
			String receivingApp1 = null;
			String receivingApp3 = null;
			if (receivingApp != null) {
				String[] parts = receivingApp.split("\\^");
				if (parts.length > 0) {
					receivingApp1 = parts[0];
				}
				if (parts.length > 2) {
					receivingApp3 = parts[2];
				}
			}
			
			// Receiving Facility
			String receivingFacility = settings.getString(Settings.HL7_RECEIVING_FACILITY);
			if (receivingFacility != null) {
				String[] parts = receivingFacility.split("\\^");
				if (parts.length > 1) {
					receivingFacility = parts[0];
				}
			}
			
			// Unique ID's
			String uniqueMessageControlID = StringTool.unique("MessageControlID"); //$NON-NLS-1$
			String uniqueProcessingID = StringTool.unique("ProcessingID"); //$NON-NLS-1$
			
			// Temp-Directory
			String tempDirectoryPathname = exportPath + "\\tmp\\";
			File tempDirectory = new File(tempDirectoryPathname);
			if (!tempDirectory.exists()) {
				tempDirectory.mkdirs();
			} else {
				File[] tmpInhalt = tempDirectory.listFiles();
				if (tmpInhalt.length > 0) {
					Logger
						.logInfo("WARN: Alte Dateien aus Tmp-Verzeichnis ins Export Verzeichnis bereitstellen ..");
					// Nachtrag 9.5.2011
					// backup ist keine gute Idee, weil damit Messwerte "verloren" gehen, bis diese
					// jemand persönlich anschaut. Wir schieben diese Dateien demzufolge in den
					// Exportpfad (Teilresultat).
					String backupDirectoryPathname = exportPath; // + "\\backup\\"
					File backupDirectory = new File(backupDirectoryPathname);
					if (!backupDirectory.exists()) {
						backupDirectory.mkdirs();
					}
					for (File tmpFile : tmpInhalt) {
						Logger.logInfo("- " + tmpFile.getName());
						// Eigentlich dürfte ja gar keine Files da sein
						// Wenn doch Files da sind, dann diese in ein backup Dir verschieben
						String baseFilename = UtilFile.getNakedFilename(tmpFile);
						// File speichern
						String filename = baseFilename + ".hl7";
						int count = 1;
						while (new File(exportPath + File.separator + filename).exists()) {
							filename = baseFilename + "_" + new Integer(count).toString() + ".hl7";
							count++;
						}
						
						String content = UtilFile.readTextFile(tmpFile);
						UtilFile.writeTextFile(new File(backupDirectoryPathname + File.separator
							+ filename), content);
						tmpFile.delete();
					}
					Logger
						.logInfo("WARN: Alte Dateien aus Tmp-Verzeichnis ins Export Verzeichnis bereitstellen: abgeschlossen");
				}
			}
			
			// Exportieren
			for (Laborwert laborwert : laborWerteList) {
				HL7_ORU_R01 oruR01 =
					new HL7_ORU_R01("CHELEXIS", "LABRESULTS", receivingApp1, receivingApp3,
						receivingFacility, uniqueMessageControlID, uniqueProcessingID, mandant);
				
				// Check if exists
				String baseFilename =
					laborwert.getPatient().getPatId() + "_" + fileDateFormat.format(new Date());
				Logger.logInfo(MessageFormat.format("Ueberpruefe, ob Datei {0} existiert..",
					baseFilename));
				String tempFilenamePath = tempDirectoryPathname + baseFilename + ".hl7";
				File hl7File = new File(tempFilenamePath);
				String encodedMessage = null;
				Integer itemNo = 1;
				if (hl7File.exists()) {
					Logger.logInfo(MessageFormat.format("Lese bestehende HL7-Datei {0} ..",
						baseFilename));
					String text = UtilFile.readTextFile(hl7File);
					ORU_R01 existingOruR01 = oruR01.read(text);
					ObservationMessage observation = oruR01.readObservation(text);
					itemNo = observation.getObservations().size() + 1;
					encodedMessage =
						oruR01.addResult(existingOruR01, getHL7Patient(laborwert),
							getHL7LaborItem(laborwert, itemNo), getHL7LaborWert(laborwert));
				} else {
					HL7Patient patient = getHL7Patient(laborwert);
					Logger.logInfo(MessageFormat.format("Erstelle neue HL7-Datei für Patient {0}",
						patient.getPatCode()));
					encodedMessage =
						oruR01.createText(patient, getHL7LaborItem(laborwert, itemNo),
							getHL7LaborWert(laborwert));
				}
				
				// File speichern
				UtilFile.writeTextFile(hl7File, encodedMessage);
				
				// Last-Update aktualisieren
				lastupdate = laborwert.getLastUpdate();
				
			}
			
			// Move from tmp-Verzeichnis
			Logger.logInfo("Dateien aus Tmp-Verzeichnis verschieben..");
			for (File tmpFile : tempDirectory.listFiles()) {
				String baseFilename = UtilFile.getNakedFilename(tmpFile);
				// File speichern
				String filename = baseFilename + ".hl7";
				int count = 1;
				while (new File(exportPath + File.separator + filename).exists()) {
					filename = baseFilename + "_" + new Integer(count).toString() + ".hl7";
					count++;
				}
				
				String content = UtilFile.readTextFile(tmpFile);
				UtilFile.writeTextFile(new File(exportPath + File.separator + filename), content);
				tmpFile.deleteOnExit();
			}
			
			Logger.logInfo(MessageFormat.format("Last-Update wird aktualisiert: ", lastupdate));
			LastUpdate.writeLastUpdate(lastupdate);
		} catch (Exception e) {
			Logger
				.logError("Unbekannter Fehler beim Export der HL7-Datei. Export abgebrochen!!", e);
		} finally {
			if (elexisDb != null) {
				elexisDb.close();
			}
		}
	}
	
	/**
	 * Transformiert ein Laborwert Objekt in ein HL7 Patient Objekt
	 * 
	 * @param patient
	 * @return
	 */
	private HL7Patient getHL7Patient(Laborwert laborwert){
		HL7Patient hl7Patient = new HL7Patient();
		hl7Patient.setAddress1(laborwert.getPatient().getStrasse());
		hl7Patient.setAddress2(laborwert.getPatient().getOther());
		hl7Patient.setBirthdate(laborwert.getPatient().getGebDatum());
		hl7Patient.setCity(laborwert.getPatient().getOrt());
		hl7Patient.setCountry(laborwert.getPatient().getLand());
		hl7Patient.setEmail(laborwert.getPatient().getEmail());
		hl7Patient.setFax(laborwert.getPatient().getFax());
		hl7Patient.setFirstname(laborwert.getPatient().getVorname());
		hl7Patient.setIsMale(laborwert.getPatient().getSex().equals(Patient.MALE));
		hl7Patient.setName(laborwert.getPatient().getNachname());
		hl7Patient.setPatCode(laborwert.getPatient().getPatId());
		hl7Patient.setPhone1(laborwert.getPatient().getTelefon1());
		hl7Patient.setPhone2(laborwert.getPatient().getTelefon2());
		hl7Patient.setTitle(laborwert.getPatient().getTitel());
		hl7Patient.setZip(laborwert.getPatient().getPlz());
		
		return hl7Patient;
	}
	
	/**
	 * Transformiert ein Laborwert Objekt in ein HL7 LaborItem Objekt
	 * 
	 * @param laborwert
	 * @param id
	 * @return
	 */
	private HL7LaborItem getHL7LaborItem(Laborwert laborwert, Integer id){
		if (id == null)
			id = 1;
		HL7LaborItem hl7LaborItem = new HL7LaborItem();
		hl7LaborItem.setId(id.toString());
		hl7LaborItem.setEinheit(laborwert.getLaborItem().getEinheit());
		hl7LaborItem.setGruppe(laborwert.getLaborItem().getGruppe());
		hl7LaborItem.setKuerzel(laborwert.getLaborItem().getKuerzel());
		hl7LaborItem.setPrio(laborwert.getLaborItem().getPrio());
		hl7LaborItem.setRefFrau(laborwert.getLaborItem().getRefFrau());
		hl7LaborItem.setRefMann(laborwert.getLaborItem().getRefMann());
		hl7LaborItem.setTitel(laborwert.getLaborItem().getTitel());
		switch (laborwert.getLaborItem().getTyp()) {
		case ABSOLUTE:
			hl7LaborItem.setTyp(HL7LaborItem.Typ.ABSOLUTE);
			break;
		case DOCUMENT:
			hl7LaborItem.setTyp(HL7LaborItem.Typ.DOCUMENT);
			break;
		case FORMULA:
			hl7LaborItem.setTyp(HL7LaborItem.Typ.FORMULA);
			break;
		case NUMERIC:
			hl7LaborItem.setTyp(HL7LaborItem.Typ.NUMERIC);
			break;
		case TEXT:
			hl7LaborItem.setTyp(HL7LaborItem.Typ.TEXT);
			break;
		}
		
		return hl7LaborItem;
	}
	
	/**
	 * Transformiert ein Laborwert Objekt in ein HL7 LaborWert Objekt
	 * 
	 * @param laborwert
	 * @return
	 */
	private HL7LaborWert getHL7LaborWert(Laborwert laborwert){
		HL7LaborWert hl7Laborwert = new HL7LaborWert();
		hl7Laborwert.setId(laborwert.getId());
		hl7Laborwert.setResultat(laborwert.getResultat());
		hl7Laborwert.setZeitpunkt(laborwert.getDatum());
		hl7Laborwert.setFlags(laborwert.getFlags());
		hl7Laborwert.setKommentar(laborwert.getKommentar());
		return hl7Laborwert;
	}
	
	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		try {
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$
			Logger.logInfoLn();
			Logger.logInfo("HL7 Export"); //$NON-NLS-1$
			Logger.logInfo("==============="); //$NON-NLS-1$
			Start start = new Start(args);
			Logger.logInfoLn();
			Logger.logInfo(MessageFormat.format("{0}: HL7 Export wird gestartet", //$NON-NLS-1$
				dateFormat.format(Calendar.getInstance().getTime())));
			start.run();
			Logger.logInfoLn();
			Logger.logInfo(MessageFormat.format("{0}: HL7 Export beendet", //$NON-NLS-1$
				dateFormat.format(Calendar.getInstance().getTime())));
		} catch (Throwable t) {
			Logger.logError(Messages.getString("Start.error"), t); //$NON-NLS-1$
		}
	}
}
