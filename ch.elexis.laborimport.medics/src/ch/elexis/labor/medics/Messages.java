package ch.elexis.labor.medics;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.labor.medics.messages"; //$NON-NLS-1$
	public static String KontaktOrderManagement_messageErrorCreateDB;
	public static String KontaktOrderManagement_titleErrorCreateDB;
	public static String LabOrderAction_errorMessageNoFallSelected;
	public static String LabOrderAction_errorMessageNoPatientSelected;
	public static String LabOrderAction_errorTitleCannotCreateHL7;
	public static String LabOrderAction_errorTitleCannotShowURL;
	public static String LabOrderAction_errorTitleNoFallSelected;
	public static String LabOrderAction_errorTitleNoPatientSelected;
	public static String LabOrderAction_infoMessageLabOrderFinshed;
	public static String LabOrderAction_infoNoMatchingPdfFile;
	public static String LabOrderAction_infoTitleLabOrderFinshed;
	public static String LabOrderAction_nameAction;
	public static String LabOrderAction_receivingApplication;
	public static String LabOrderAction_receivingFacility;
	public static String LabOrderImport_descriptionImport;
	public static String LabOrderImport_errorMsgVerarbeitung;
	public static String LabOrderImport_errorTitle;
	public static String LabOrderImport_labelDocumentCategory;
	public static String LabOrderImport_labelDownloadDir;
	public static String LabOrderImport_LabResult;
	public static String LabOrderImport_monitorImportiereHL7;
	public static String LabOrderImport_titleImport;
	public static String MedicsBrowserView_errorOpeningBrowserURL;
	public static String MedicsPreferencePage_defaultMedicsUrl;
	public static String MedicsPreferencePage_documentCategoryName;
	public static String MedicsPreferencePage_labelArchivDir;
	public static String MedicsPreferencePage_labelDocumentCategory;
	public static String MedicsPreferencePage_labelDownloadDir;
	public static String MedicsPreferencePage_labelUploadDir;
	public static String MedicsPreferencePage_labelErrorDir;
	public static String PatientLabor_errorAddingDocument;
	public static String PatientLabor_errorCreatingTmpDir;
	public static String PatientLabor_errorKeineDokumentablage;
	public static String PatientLabor_InfoDocSavedToOmnivore;
	public static String PatientLabor_InfoExistingValueIsValid;
	public static String PatientLabor_InfoOverwriteValue;
	public static String PatientLabor_kuerzelMedics;
	public static String PatientLabor_nameDokumentLaborParameter;
	public static String PatientLabor_nameMedicsLabor;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
