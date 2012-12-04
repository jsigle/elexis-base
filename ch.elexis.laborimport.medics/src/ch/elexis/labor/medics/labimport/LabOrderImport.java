package ch.elexis.labor.medics.labimport;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.hl7.model.EncapsulatedData;
import ch.elexis.hl7.model.IValueType;
import ch.elexis.hl7.model.ObservationMessage;
import ch.elexis.hl7.model.StringData;
import ch.elexis.hl7.model.TextData;
import ch.elexis.hl7.v26.HL7_ORU_R01;
import ch.elexis.labor.medics.MedicsActivator;
import ch.elexis.labor.medics.MedicsPreferencePage;
import ch.elexis.labor.medics.Messages;
import ch.elexis.labor.medics.data.KontaktOrderManagement;
import ch.elexis.laborimport.medics.util.MedicsLogger;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.SWTHelper;
import ch.rgw.io.FileTool;

public class LabOrderImport extends ImporterPage {
	
	// Als Domain für die Filler-Auftragsnummer die GLN von Medics verwenden
	public static final String ORDER_NR_DOMAIN_FILLER =
		KontaktOrderManagement.ORDER_DOMAIN_LAB_ORDER_FILLER_MEDICS;
	
	protected final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$
	
	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		MedicsLogger.getLogger().println(
			MessageFormat.format("{0}: Medics Laborimport gestartet", df.format(new Date()))); //$NON-NLS-1$
		MedicsLogger.getLogger().println(
			"=============================================================="); //$NON-NLS-1$
		
		int errorCount = 0;
		int errorMovedCount = 0;
		
		File downloadDir = new File(MedicsPreferencePage.getDownloadDir());
		MedicsLogger.getLogger().println(
			MessageFormat.format("HL7 Dateien in Verzeichnis {0} lesen..", downloadDir)); //$NON-NLS-1$
		if (downloadDir.isDirectory()) {
			File[] hl7Files = downloadDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name){
					return name.toLowerCase().endsWith(".hl7"); //$NON-NLS-1$
				}
			});
			monitor.beginTask(Messages.LabOrderImport_monitorImportiereHL7, hl7Files.length);
			if (hl7Files != null) {
				HL7_ORU_R01 hl7OruR01 = new HL7_ORU_R01();
				for (File hl7File : hl7Files) {
					if (monitor.isCanceled()) {
						break;
					}
					String msg = MessageFormat.format("Parse Datei {0}..", hl7File.getName());//$NON-NLS-1$
					monitor.subTask(msg);
					MedicsLogger.getLogger().println(msg);
					
					// HL7 Datei lesen
					boolean importOk = true;
					try {
						String text = FileTool.readTextFile(hl7File, MedicsActivator.TEXT_ENCODING);
						ObservationMessage observation = hl7OruR01.readObservation(text);
						for (String error : hl7OruR01.getErrorList()) {
							importOk = false;
							MedicsLogger.getLogger().println(
								MessageFormat.format("ERROR: {0}", error)); //$NON-NLS-1$
						}
						for (String warn : hl7OruR01.getWarningList()) {
							MedicsLogger.getLogger().println(
								MessageFormat.format("WARN: {0}", warn)); //$NON-NLS-1$
						}
						if (importOk) {
							importOk = addObservations(observation);
							addPdfToOmnivore(hl7File, observation);
						}
					} catch (ca.uhn.hl7v2.HL7Exception ex) {
						importOk = false;
						String cause = "";
						if (ex.getCause() != null) {
							if (ex.getCause().getMessage() != null) {
								cause = ex.getCause().getMessage();
							}
						}
						MedicsLogger.getLogger().println(
							MessageFormat.format(
								"ERROR: {0}\r\nHL7 Exception cause: {1}", ex.getMessage(), cause)); //$NON-NLS-1$
					} catch (Exception ex) {
						importOk = false;
						MedicsLogger.getLogger().println(
							MessageFormat.format("ERROR: {0}", ex.getMessage())); //$NON-NLS-1$
					}
					if (importOk) {
						// Archivieren
						moveToArchive(hl7File);
					} else {
						if (moveToError(hl7File)) {
							errorMovedCount++;
						}
						errorCount++;
						monitor.subTask(MessageFormat.format(
							"Fehler beim Parsen der Datei {0}!", hl7File.getName()));//$NON-NLS-1$
					}
					
					monitor.worked(1);
				}
			}
		}
		
		if (errorCount > 0) {
			String errorDir = MedicsPreferencePage.getErrorDir();
			SWTHelper.showError(Messages.LabOrderImport_errorTitle, MessageFormat
				.format(Messages.LabOrderImport_errorMsgVerarbeitung, errorCount, errorMovedCount,
					errorDir));
		}
		
		MedicsLogger.getLogger().println(
			MessageFormat.format("{0}: Medics Laborimport beendet", df.format(new Date()))); //$NON-NLS-1$
		MedicsLogger.getLogger().println(""); //$NON-NLS-1$
		
		// Bereinigung der alten Archiv Dateien
		deleteOldArchivFiles();
		
		return Status.OK_STATUS;
	}
	
	/**
	 * Anhand der Einstellungen (Default 30 Tage) werden alle Dateien im Archiv Verzeichnis gelöscht
	 * die älter als die konfigurierten Tage sind.
	 * 
	 * @return
	 */
	private void deleteOldArchivFiles(){
		int archivDeleted = 0;
		MedicsLogger.getLogger().println("Alte Archiv Dateien werden bereinigt.."); //$NON-NLS-1$
		
		int days = MedicsPreferencePage.getDeleteArchivDays();
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -days);
		long lastTime = cal.getTime().getTime();
		
		// Archiv löschen
		String archivDirName = MedicsPreferencePage.getArchivDir();
		if (archivDirName != null) {
			File archivDir = new File(archivDirName);
			if (archivDir.exists() && archivDir.isDirectory()) {
				for (File archivFile : archivDir.listFiles()) {
					if (archivFile.lastModified() < lastTime) {
						if (archivFile.delete()) {
							archivDeleted++;
						}
					}
				}
			}
			MedicsLogger.getLogger()
				.println(
					MessageFormat.format(
						"{0} Dateien aus Archiv Verzeichnis gelöscht.", archivDeleted)); //$NON-NLS-1$
		}
		
		MedicsLogger.getLogger().println(""); //$NON-NLS-1$
	}
	
	/**
	 * Datei wird ins Archiv Verzeichnis verschoben
	 * 
	 * @param file
	 */
	private boolean moveToArchive(final File file){
		String archivDir = MedicsPreferencePage.getArchivDir();
		boolean ok = false;
		if (FileTool.copyFile(file, new File(archivDir + File.separator + file.getName()),
			FileTool.REPLACE_IF_EXISTS)) {
			ok = file.delete();
		}
		return ok;
	}
	
	/**
	 * Datei wird ins Error Verzeichnis verschoben, falls ein Error Verzeichnis definiert wurde!
	 * 
	 * @param file
	 */
	private boolean moveToError(final File file){
		String errorDir = MedicsPreferencePage.getErrorDir();
		boolean ok = false;
		if (errorDir != null && errorDir.length() > 0) {
			if (FileTool.copyFile(file, new File(errorDir + File.separator + file.getName()),
				FileTool.REPLACE_IF_EXISTS)) {
				ok = file.delete();
			}
		}
		return ok;
	}
	
	/**
	 * Fügt Observations (Laboreinträge) zu Patient hinzu
	 * 
	 * @param observation
	 */
	private boolean addObservations(ObservationMessage observation){
		MedicsLogger.getLogger().println("Laboreinträge erstellen.."); //$NON-NLS-1$
		Patient patient = getPatient(observation);
		if (patient != null) {
			PatientLabor labor = new PatientLabor(patient);
			for (IValueType type : observation.getObservations()) {
				if (type.getDate() == null) {
					type.setDate(observation.getDateTimeOfMessage());
					MedicsLogger
						.getLogger()
						.println(
							MessageFormat
								.format(
									"WARN: Observation (OBX) ohne Datum (OBX-14). Verwende Datum aus MSH-7: {0}", //$NON-NLS-1$
									observation.getDateTimeOfMessage().toString()));
				}
				if (type instanceof StringData) {
					labor.addLaborItem((StringData) type);
				} else if (type instanceof EncapsulatedData) {
					try {
						labor.addDocument((EncapsulatedData) type);
					} catch (IOException e) {
						MedicsLogger.getLogger().println(
							MessageFormat.format("ERROR: Dokument hinzufügen: {0}", //$NON-NLS-1$
								e.getMessage()));
						return false;
					}
				} else if (type instanceof TextData) {
					labor.addLaborItem((TextData) type);
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Searches for a PDF file corresponding to the specified <tt>hl7File</tt> and imports it into
	 * <cite>Omnivore</cite>. If no such file is found, then this method returns immediately after
	 * having written an informational message to the logger.
	 * <p>
	 * If there is a file, in the same folder and having the same name as the specified
	 * <tt>hl7File</tt> but with the <tt>.pdf</tt> extension, then this file is imported into
	 * <cite>Omnivore</cite>.
	 * </p>
	 * <p>
	 * If the file already exists in <cite>Omnivore</cite>, then it gets overwritten only, if the
	 * file to import is newer than the existing one, or if
	 * {@link PatientLabor#setOverwriteResults(boolean)} was called with <tt>true</tt> as parameter.
	 * </p>
	 * <p>
	 * If the file import into <cite>Omnivore</cite> fails, or if, for any reason, the
	 * {@link #getPatient(ObservationMessage)} return <tt>null</tt>, the PDF file is moved to the
	 * error folder. In all other cases the file is moved to the archive folder.
	 * </p>
	 * 
	 * @param hl7File
	 *            the currently processed <cite>HL7 file</cite>.
	 * @param observation
	 *            the observation messages.
	 * @throws IOException
	 *             if, for any reason, the PDF document could not be store in <cite>Omnivore</cite>.
	 * @throws NullPointerException
	 *             if the specified HL7 file or the observation message is <tt>null</tt>.
	 */
	private void addPdfToOmnivore(File hl7File, ObservationMessage observation) throws IOException{
		if (hl7File == null) {
			throw new NullPointerException("HL7 file is null.");
		} else if (observation == null) {
			throw new NullPointerException("Observation message is null.");
		}
		// Build the PDF file name and check if it exists
		//
		String pdfFileName = hl7File.getName().replaceAll("\\.[^\\.]+$", ".pdf");
		File pdfFile = new File(hl7File.getParent(), pdfFileName);
		if (!pdfFile.exists()) {
			MedicsLogger.getLogger().println(Messages.LabOrderAction_infoNoMatchingPdfFile);
			return;
		}
		// If patient can not be retrieved move file to error and abort
		//
		Patient patient = getPatient(observation);
		if (patient == null) {
			moveToError(pdfFile);
			return;
		}
		// Parameters for the saveLaborItem() function
		//
		Date timeStamp = observation.getDateTimeOfTransaction();
		if (timeStamp == null) {
			timeStamp = observation.getDateTimeOfMessage();
		}
		String category = MedicsPreferencePage.getDokumentKategorie();
		String orderId = getAuftragsId(observation);
		
		// Import PDF file into Omnivore
		//
		PatientLabor labor = new PatientLabor(patient);
		boolean fileImported = false;
		try {
			labor.saveLaborItem(pdfFileName, category, pdfFile, timeStamp, orderId, pdfFileName);
			fileImported = true;
		}
		// Move file to archive or error directory
		//
		finally {
			if (fileImported) {
				moveToArchive(pdfFile);
			} else {
				moveToError(pdfFile);
			}
		}
	} // End of addPDFToOmnivore()
	
	/**
	 * Liest alle Patient mit einer bestimmten PatientenNr. Eigentlich sollte es nur 1 Patient
	 * geben, aber man weiss ja nie!
	 * 
	 * @param patId
	 * @return List der gefundenen Patienten
	 */
	private List<Patient> readPatienten(final String patId){
		Query<Patient> patientQuery = new Query<Patient>(Patient.class);
		patientQuery.add(Patient.FLD_PATID, Query.EQUALS, patId);
		return patientQuery.execute();
	}
	
	/**
	 * Sucht Elexis Patient. <br>
	 * Falls Auftragsnummer (ORC-2) existiert, dann wird anhand der Tabelle KONTAKT_ORDER_MANAGEMENT
	 * der zugehörige Patient gesucht. <br>
	 * Wenn keine Auftragsnummer (ORC-2) vorhanden ist, dann wird über der Patient über die
	 * PatientNr (PID-3) gesucht.
	 * 
	 * @param observation
	 * @return
	 */
	private Patient getPatient(final ObservationMessage observation){
		// Suche Patient anhand internal PID oder Auftragsnummer
		Patient patient = null;
		String patientId = observation.getPatientId();
		String auftragsNr = observation.getOrderNumberPlacer();
		// Anhand observation.getOrderNumber() den Patienten suchen
		if (auftragsNr != null && auftragsNr.length() > 0) {
			Query<KontaktOrderManagement> patientOrderNrQuery =
				new Query<KontaktOrderManagement>(KontaktOrderManagement.class);
			patientOrderNrQuery.add(KontaktOrderManagement.FLD_ORDER_NR, Query.EQUALS, auftragsNr);
			List<KontaktOrderManagement> patientOrderNrList = patientOrderNrQuery.execute();
			if (patientOrderNrList.size() == 0) {
				// Suche übereinstimmung
				String patName = observation.getPatientName();
				List<Patient> patientList = readPatienten(patientId);
				for (Patient listPat : patientList) {
					if (patName.equalsIgnoreCase(listPat.getName() + " " + listPat.getVorname())) {
						patient = listPat;
					}
				}
				if (patient == null) {
					MedicsLogger
						.getLogger()
						.println(
							MessageFormat
								.format(
									"ERROR: Kein Patient zu Auftragsnummer={0} gefunden und Patientnummer {1} existiert auch nicht mit dem Namen {2}. Import abgebrochen..!", //$NON-NLS-1$
									auftragsNr, patientId, patName));
				} else {
					MedicsLogger
						.getLogger()
						.println(
							MessageFormat
								.format(
									"WARN: Kein Patient zu Auftragsnummer={0} gefunden, aber Patientnummer {1} existiert mit dem angegebenen Namen {2}. Resultat wurde Patient {1} zugewiesen..!", //$NON-NLS-1$
									auftragsNr, patientId, patName));
				}
			} else {
				// Suche übereinstimmung
				List<Patient> patientList = readPatienten(patientId);
				for (KontaktOrderManagement kontaktOrderMgt : patientOrderNrList) {
					String kontaktId = kontaktOrderMgt.getKontakt().getId();
					for (Patient pat : patientList) {
						if (kontaktId.equals(pat.getId())) {
							patient = pat;
						}
					}
				}
				if (patient == null) {
					MedicsLogger
						.getLogger()
						.println(
							MessageFormat
								.format(
									"ERROR: Patient der Auftragsnummer ({0}) kann nicht zu Patient ID ({1}) zugeordnet werden. Import abgebrochen..!", //$NON-NLS-1$
									auftragsNr, patientId));
				}
			}
		} else {
			// Keine Auftragsnummer. Verwende PID
			MedicsLogger
				.getLogger()
				.println(
					MessageFormat
						.format(
							"WARN: Kein Auftragsnummer vorhanden. Patient wird anhand PID-2 ({0}) bestimmt !", //$NON-NLS-1$
							patientId));
			List<Patient> patientList = readPatienten(patientId);
			if (patientList.size() == 0) {
				MedicsLogger.getLogger().println(
					MessageFormat.format("ERROR: Kein Patient mit ID={0} gefunden!", patientId)); //$NON-NLS-1$
			} else if (patientList.size() > 0) {
				patient = patientList.get(0);
				if (patientList.size() > 1) {
					MedicsLogger.getLogger().println(
						MessageFormat.format(
							"WARN: Mehrere Patienten mit ID={0} gefunden! Verwende Erster: {1}", //$NON-NLS-1$
							patientId, patient.getLabel()));
				}
			}
		}
		
		return patient;
	}
	
	/**
	 * Liefert die ID des Eintrags in KontaktOrderManagement zurück, sofern ein Eintrag existiert
	 * 
	 * @param observation
	 *            Angaben aus der HL7 Nachricht
	 * @return ID des Eintrags in KontaktOrderManagement oder null
	 */
	private static String getAuftragsId(ObservationMessage observation){
		String orderId = ""; //$NON-NLS-1$
		long auftragsNrFiller = -1;
		try {
			auftragsNrFiller = Long.parseLong(observation.getOrderNumberFiller());
		} catch (Exception ex) {}
		Query<KontaktOrderManagement> patientOrderNrQuery =
			new Query<KontaktOrderManagement>(KontaktOrderManagement.class);
		patientOrderNrQuery.add(KontaktOrderManagement.FLD_ORDER_NR, Query.EQUALS,
			Long.toString(auftragsNrFiller));
		patientOrderNrQuery.add(KontaktOrderManagement.FLD_ORDER_NR_DOMAIN, Query.EQUALS,
			ORDER_NR_DOMAIN_FILLER);
		List<KontaktOrderManagement> orderNrList = patientOrderNrQuery.execute();
		if (orderNrList.size() > 0) {
			orderId = orderNrList.get(0).getId();
		}
		return orderId;
	}
	
	@Override
	public String getTitle(){
		return Messages.LabOrderImport_titleImport;
	}
	
	@Override
	public String getDescription(){
		return Messages.LabOrderImport_descriptionImport;
	}
	
	@Override
	public Composite createPage(Composite parent){
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		composite.setLayout(new GridLayout(2, false));
		
		// Rechnung Verzeichnis
		Label lblDownloadDir = new Label(composite, SWT.NONE);
		lblDownloadDir.setText(Messages.LabOrderImport_labelDownloadDir);
		
		final Text txtDownloadDir = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		txtDownloadDir.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String downloadDir = MedicsPreferencePage.getDownloadDir();
		if (downloadDir != null) {
			txtDownloadDir.setText(downloadDir);
		}
		
		// Kategorie Verzeichnis
		Label lblKategorie = new Label(composite, SWT.NONE);
		lblKategorie.setText(Messages.LabOrderImport_labelDocumentCategory);
		
		final Text txtKategorie = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		txtKategorie.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String kategorie = MedicsPreferencePage.getDokumentKategorie();
		if (kategorie != null) {
			txtKategorie.setText(kategorie);
		}
		
		return composite;
	}
	
}
