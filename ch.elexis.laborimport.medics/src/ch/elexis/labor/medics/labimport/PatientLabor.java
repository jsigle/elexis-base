package ch.elexis.labor.medics.labimport;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ch.elexis.ElexisException;
import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.hl7.model.EncapsulatedData;
import ch.elexis.hl7.model.StringData;
import ch.elexis.hl7.model.TextData;
import ch.elexis.labor.medics.MedicsPreferencePage;
import ch.elexis.labor.medics.Messages;
import ch.elexis.laborimport.medics.util.MedicsLogger;
import ch.elexis.services.GlobalServiceDescriptors;
import ch.elexis.services.IDocumentManager;
import ch.elexis.text.GenericDocument;
import ch.elexis.text.IOpaqueDocument;
import ch.elexis.util.Extensions;
import ch.rgw.io.FileTool;
import ch.rgw.tools.TimeSpan;
import ch.rgw.tools.TimeTool;

public class PatientLabor {
	
	private static String LABOR_NAME = Messages.PatientLabor_nameMedicsLabor;
	public static String DEFAULT_PRIO = "50"; //$NON-NLS-1$
	public static String FORMAT_TIME = "HHmmss"; //$NON-NLS-1$
	
	private static String KUERZEL = Messages.PatientLabor_kuerzelMedics;
	private static String FIELD_ORGIN = "Quelle"; //$NON-NLS-1$
	private static int MAX_LEN_RESULT = 80; // Spaltenlänge LABORWERTE.Result
	
	private Labor myLab = null;
	
	private final Patient patient;
	
	private IDocumentManager docManager;
	
	private boolean overwriteResults = false;
	
	public PatientLabor(Patient patient){
		this.patient = patient;
		initLabor();
		initDocumentManager();
	}
	
	/**
	 * Setting zum Überschreiben von bestehenden Laborresultaten.
	 * 
	 * @param value
	 *            true, wenn Laborwerte überschrieben werden sollen, auch wenn bereits ein neuerer
	 *            der DB vorhanden ist. Sonst false (false ist Normalfall!)
	 */
	public void setOverwriteResults(boolean value){
		overwriteResults = value;
	}
	
	/**
	 * Initialisiert document manager (omnivore) falls vorhanden
	 */
	private void initDocumentManager(){
		Object os = Extensions.findBestService(GlobalServiceDescriptors.DOCUMENT_MANAGEMENT);
		if (os != null) {
			this.docManager = (IDocumentManager) os;
		}
	}
	
	/**
	 * Check if category exists. If not, the category is created
	 */
	private void checkCreateCategory(final String category){
		if (category != null) {
			boolean catExists = false;
			for (String cat : this.docManager.getCategories()) {
				if (category.equals(cat)) {
					catExists = true;
				}
			}
			if (!catExists) {
				this.docManager.addCategorie(category);
			}
		}
	}
	
	/**
	 * Adds a document to omnivore (if it not already exists)
	 * 
	 * @return boolean. True if added false if not
	 * @throws ElexisException
	 * @throws IOException
	 */
	private boolean addDocument(final String title, final String category, final String dateStr,
		final File file) throws IOException, ElexisException{
		checkCreateCategory(category);
		
		List<IOpaqueDocument> documentList =
			this.docManager.listDocuments(this.patient, category, title, null, new TimeSpan(dateStr
				+ "-" + dateStr), null);
		
		if (documentList == null || documentList.size() == 0) {
			this.docManager.addDocument(new GenericDocument(this.patient, title, category, file,
				dateStr, null, null));
			return true;
		}
		return false;
	}
	
	private void initLabor(){
		Query<Labor> qbe = new Query<Labor>(Labor.class);
		qbe.add("Kuerzel", "LIKE", "%" + KUERZEL + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		List<Labor> list = qbe.execute();
		
		if (list.size() < 1) {
			myLab = new Labor(KUERZEL, LABOR_NAME); //$NON-NLS-1$
		} else {
			myLab = list.get(0);
		}
	}
	
	/**
	 * Liest LabItem
	 * 
	 * @param kuerzel
	 * @param type
	 * @return LabItem falls exisitiert. Sonst null
	 */
	private LabItem getLabItem(String kuerzel, LabItem.typ type){
		Query<LabItem> qli = new Query<LabItem>(LabItem.class);
		qli.add(LabItem.SHORTNAME, "=", kuerzel); //$NON-NLS-1$ //$NON-NLS-2$
		qli.and();
		qli.add(LabItem.LAB_ID, "=", myLab.get("ID")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		qli.and();
		qli.add(LabItem.TYPE, "=", new Integer(type.ordinal()).toString()); //$NON-NLS-1$
		
		LabItem labItem = null;
		List<LabItem> itemList = qli.execute();
		if (itemList.size() > 0) {
			labItem = itemList.get(0);
		}
		return labItem;
	}
	
	/**
	 * Liest LabItem
	 * 
	 * @param kuerzel
	 * @param type
	 * @return LabItem falls exisitiert. Sonst null
	 */
	private LabResult getLabResult(LabItem labItem, String name, TimeTool date){
		Query<LabResult> qli = new Query<LabResult>(LabResult.class);
		qli.add(LabResult.ITEM_ID, "=", labItem.getId()); //$NON-NLS-1$
		qli.and();
		qli.add(LabResult.DATE, "=", date.toDBString(false)); //$NON-NLS-1$ //$NON-NLS-2$
		qli.and();
		qli.add(LabResult.PATIENT_ID, "=", patient.getId()); //$NON-NLS-1$
		qli.and();
		qli.add(LabResult.RESULT, "=", name); //$NON-NLS-1$ //$NON-NLS-2$
		
		LabResult labResult = null;
		List<LabResult> resultList = qli.execute();
		if (resultList.size() > 0) {
			labResult = resultList.get(0);
		}
		return labResult;
	}
	
	/**
	 * Fügt Laborwert zu Patientenlabor hinzu
	 * 
	 * @param data
	 */
	public void addLaborItem(final StringData data){
		LabItem labItem = getLabItem(data.getName(), LabItem.typ.NUMERIC);
		if (labItem == null) {
			String group = data.getGroup();
			if (group == null || group.length() == 0) {
				group = LABOR_NAME;
			}
			String sequence = data.getSequence();
			if (sequence == null || sequence.length() == 0) {
				sequence = "50";
			}
			labItem =
				new LabItem(data.getName(), data.getName(), myLab, null, null, data.getUnit(),
					LabItem.typ.NUMERIC, group, sequence);
		}
		
		// RefFrau, bzw. RefMann aktualisieren
		if (Patient.MALE.equals(patient.getGeschlecht())) {
			String labRefMann = labItem.getRefM();
			if (labRefMann == null || labRefMann.length() == 0) {
				String newRefMann = data.getRange();
				if (newRefMann != null && newRefMann.length() > 0) {
					labItem.setRefM(newRefMann);
				}
			}
		} else {
			String labRefFrau = labItem.getRefW();
			if (labRefFrau == null || labRefFrau.length() == 0) {
				String newRefFrau = data.getRange();
				if (newRefFrau != null && newRefFrau.length() > 0) {
					labItem.setRefW(newRefFrau);
				}
			}
		}
		
		TimeTool dateTime = new TimeTool();
		dateTime.setTime(data.getDate());
		LabResult lr =
			new LabResult(patient, dateTime, labItem, data.getValue(), data.getComment()); //$NON-NLS-1$
		lr.set("Quelle", LABOR_NAME); //$NON-NLS-1$
	}
	
	/**
	 * Fügt Laborwert zu Patientenlabor hinzu
	 * 
	 * @param data
	 */
	public void addLaborItem(final TextData data){
		LabItem labItem = getLabItem(data.getName(), LabItem.typ.TEXT);
		if (labItem == null) {
			String group = data.getGroup();
			if (group == null || group.length() == 0) {
				group = LABOR_NAME;
			}
			String sequence = data.getSequence();
			if (sequence == null || sequence.length() == 0) {
				sequence = "50";
			}
			labItem =
				new LabItem(data.getName(), data.getName(), myLab, null, null,
					"", LabItem.typ.TEXT, group, sequence); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		TimeTool dateTime = new TimeTool();
		dateTime.setTime(data.getDate());
		LabResult lr = getLabResult(labItem, data.getName(), dateTime);
		if (lr != null) {
			// Wenn Text noch nicht an diesem Tag für diesen Patient vorhanden, dann wird er zum
			// bestehenden Text hinzugefügt
			if (lr.getComment().indexOf(data.getText()) < 0) {
				lr.set(LabResult.COMMENT, lr.getComment() + "\n-----------\n\n" + data.getText());
			}
		} else {
			lr = new LabResult(patient, dateTime, labItem, "Text", data.getText()); //$NON-NLS-1$
		}
		lr.set("Quelle", LABOR_NAME); //$NON-NLS-1$
	}
	
	/**
	 * Fügt Dokument zu Patientenlabor hinzu
	 * 
	 * @param data
	 */
	public void addDocument(EncapsulatedData data) throws IOException{
		if (this.docManager == null) {
			throw new IOException(MessageFormat.format(
				Messages.PatientLabor_errorKeineDokumentablage, data.getName(),
				this.patient.getLabel()));
		}
		
		// Kategorie überprüfen/ erstellen
		String category = MedicsPreferencePage.getDokumentKategorie();
		checkCreateCategory(category);
		
		String downloadDir = MedicsPreferencePage.getDownloadDir();
		
		// Tmp Verzeichnis überprüfen
		File tmpDir = new File(downloadDir + File.separator + "tmp"); //$NON-NLS-1$
		if (!tmpDir.exists()) {
			if (!tmpDir.mkdirs()) {
				throw new IOException(MessageFormat.format(
					Messages.PatientLabor_errorCreatingTmpDir, tmpDir.getName()));
			}
		}
		String filename = data.getName();
		File tmpPdfFile =
			new File(downloadDir + File.separator + "tmp" + File.separator + filename); //$NON-NLS-1$
		tmpPdfFile.deleteOnExit();
		FileTool.writeFile(tmpPdfFile, data.getData());
		
		TimeTool dateTime = new TimeTool();
		dateTime.setTime(data.getDate());
		String dateTimeStr = dateTime.toString(TimeTool.DATE_GER);
		
		try {
			// Zu Dokumentablage hinzufügen
			addDocument(filename, category, dateTimeStr, tmpPdfFile);
			
			// Labor Item erstellen
			String kuerzel = "doc"; //$NON-NLS-1$
			LabItem labItem = getLabItem(kuerzel, LabItem.typ.DOCUMENT);
			if (labItem == null) {
				String group = data.getGroup();
				if (group == null || group.length() == 0) {
					group = LABOR_NAME;
				}
				String sequence = data.getSequence();
				if (sequence == null || sequence.length() == 0) {
					sequence = "50";
				}
				labItem =
					new LabItem(kuerzel, Messages.PatientLabor_nameDokumentLaborParameter, myLab,
						"", "", //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						FileTool.getExtension(filename), LabItem.typ.DOCUMENT, group, sequence); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			LabResult lr = new LabResult(patient, dateTime, labItem, filename, data.getComment()); //$NON-NLS-1$
			lr.set("Quelle", LABOR_NAME); //$NON-NLS-1$
		} catch (ElexisException e) {
			throw new IOException(MessageFormat.format(Messages.PatientLabor_errorAddingDocument,
				tmpPdfFile.getName()), e);
		}
	}
	
	/**
	 * Speichert externer Laborbefund
	 * 
	 * @param title
	 *            Titel, der gespeichert werden soll
	 * @param category
	 *            Kategorie, die verwendet werden soll
	 * @param file
	 *            File, das archiviert werden soll
	 * @param timeStamp
	 *            Timestamp, welcher gespeichert werden soll
	 * @param orderId
	 *            Fremdschlüssel auf kontakt_order_management.id
	 * @param keyword
	 *            Schlüsselwörter, welche gespeichert werden sollen
	 * @throws IOException
	 *             if the document manager is <tt>null</tt> or if, for any reason, the labor item
	 *             could not be store in <cite>Omnivore</cite>.
	 */
	public void saveLaborItem(String title, String category, File file, Date timeStamp,
		String orderId, String keyword) throws IOException{
		String filename = file.getName();
		if (this.docManager == null) {
			throw new IOException(MessageFormat.format(
				Messages.PatientLabor_errorKeineDokumentablage, filename, this.patient.getLabel()));
		}
		
		// Kategorie überprüfen/ erstellen
		checkCreateCategory(category);
		
		TimeTool dateTime = new TimeTool();
		dateTime.setTime(timeStamp);
		SimpleDateFormat sdfZeit = new SimpleDateFormat(FORMAT_TIME);
		String zeit = sdfZeit.format(timeStamp);
		
		if (title.length() > MAX_LEN_RESULT)
			title = "..." + title.substring(title.length() - MAX_LEN_RESULT + 3, title.length()); //$NON-NLS-1$
			
		// Labor Item erstellen
		String kuerzel = "doc"; //$NON-NLS-1$
		LabItem labItem = getLabItem(kuerzel, LabItem.typ.DOCUMENT);
		if (labItem == null) {
			labItem =
				new LabItem(kuerzel, Messages.PatientLabor_nameDokumentLaborParameter, myLab,
					"", "", //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					"pdf", LabItem.typ.DOCUMENT, LABOR_NAME, DEFAULT_PRIO); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (orderId == null || "".equals(orderId)) { //$NON-NLS-1$
			orderId = LABOR_NAME;
		}
		
		boolean saved = false;
		LabResult lr = getLabResult(labItem, title, dateTime);
		if (lr == null) {
			// Neues Laborresultat erstellen
			lr = new LabResult(patient, dateTime, labItem, title, null); //$NON-NLS-1$
			lr.set(FIELD_ORGIN, orderId); //$NON-NLS-1$
			lr.set(LabResult.TIME, zeit); //$NON-NLS-1$
			saved = true;
		} else {
			// bestehendes Laborresultat ändern, sofern es neuer ist als das bereits gespeicherte
			if (overwriteResults || lr.getDateTime().before(timeStamp)) {
				MedicsLogger
					.getLogger()
					.println(
						MessageFormat.format(
							Messages.PatientLabor_InfoOverwriteValue,
							labItem.getKuerzel() + "-" + labItem.getName(), lr.getDateTime().toDBString(true), dateTime.toDBString(true), lr.getResult(), title)); //$NON-NLS-2$
				lr.setResult(title);
				lr.set(LabResult.TIME, zeit); //$NON-NLS-1$
				saved = true;
			} else {
				MedicsLogger
					.getLogger()
					.println(
						MessageFormat.format(
							Messages.PatientLabor_InfoExistingValueIsValid,
							labItem.getKuerzel() + "-" + labItem.getName(), lr.getDateTime().toDBString(true), dateTime.toDBString(true), lr.getResult(), title)); //$NON-NLS-2$
			}
		}
		
		if (saved) {
			// Dokument in Omnivore archivieren
			try {
				String dateTimeStr = dateTime.toString(TimeTool.DATE_GER);
				
				// Zu Dokumentablage hinzufügen
				addDocument(title, category, dateTimeStr, file);
				MedicsLogger.getLogger().println(
					MessageFormat.format(Messages.PatientLabor_InfoDocSavedToOmnivore, title));
				
			} catch (ElexisException e) {
				throw new IOException(MessageFormat.format(
					Messages.PatientLabor_errorAddingDocument, filename), e);
			}
		}
	}
}
