package ch.elexis.export.hl7;

import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import ch.elexis.export.Messages;

public class Settings extends Properties {
	private static final long serialVersionUID = -8153171494120107140L;
	
	public static String ELEXIS_DB_DRIVER = "elexis.db.driver"; //$NON-NLS-1$
	public static String ELEXIS_DB_URL = "elexis.db.url"; //$NON-NLS-1$
	public static String ELEXIS_DB_USER = "elexis.db.user"; //$NON-NLS-1$
	public static String ELEXIS_DB_PWD = "elexis.db.pwd"; //$NON-NLS-1$
	public static String EXPORT_PATH = "export.path"; //$NON-NLS-1$
	public static String EXPORT_TAG = "export.tag"; //$NON-NLS-1$
	public static String LOG_DEBUG = "log.debug"; //$NON-NLS-1$
	
	public static String HL7_MANDANT_KUERZEL = "hl7.mandant.label"; //$NON-NLS-1$
	public static String HL7_MANDANT_EAN = "hl7.mandant.ean"; //$NON-NLS-1$
	public static String HL7_RECEIVING_APPLICATION = "hl7.receiving.application"; //$NON-NLS-1$
	public static String HL7_RECEIVING_FACILITY = "hl7.receiving.facility"; //$NON-NLS-1$
	
	private static Settings currentSettings = null;
	
	private final String filenamePath;
	
	public static Settings getCurrent(){
		if (currentSettings == null) {
			currentSettings = new Settings();
		}
		return currentSettings;
	}
	
	private Settings(){
		this("settings.ini"); //$NON-NLS-1$
	}
	
	private Settings(String _filenamePath){
		super(getDefaults());
		this.filenamePath = _filenamePath;
		FileReader reader = null;
		try {
			reader = new FileReader(this.filenamePath);
			load(reader);
		} catch (IOException e) {
			Logger.logError(MessageFormat.format(Messages.getString("Settings.errorReadingINI"), //$NON-NLS-1$
				this.filenamePath), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					Logger.logError(Messages.getString("Settings.errorClosingFile"), ex); //$NON-NLS-1$
				}
			}
		}
	}
	
	public void showProperties(String exportPathParam, String exportTagParam){
		Logger.logInfo(MessageFormat.format(Messages.getString("Settings.infoOutput"), //$NON-NLS-1$
			this.filenamePath));
		Logger.logInfo(Messages.getString("Settings.elexisDB")); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.elexisDriver") + getProperty(ELEXIS_DB_DRIVER)); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.elexisPath") + getProperty(ELEXIS_DB_URL)); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.elexisUser") + getProperty(ELEXIS_DB_USER)); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.elexisPwd") + getProperty(ELEXIS_DB_PWD)); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.exportPath") + exportPathParam); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.exportTag") + exportTagParam); //$NON-NLS-1$
		Logger
			.logInfo(Messages.getString("Settings.hl7sendingFacilityLabel") + getProperty(HL7_MANDANT_KUERZEL)); //$NON-NLS-1$
		Logger
			.logInfo(Messages.getString("Settings.hl7sendingFacilityID") + getProperty(HL7_MANDANT_EAN)); //$NON-NLS-1$
		Logger
			.logInfo(Messages.getString("Settings.hl7receivingApp") + getProperty(HL7_RECEIVING_APPLICATION)); //$NON-NLS-1$
		Logger
			.logInfo(Messages.getString("Settings.hl7receivingFacility") + getProperty(HL7_RECEIVING_FACILITY)); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.logging")); //$NON-NLS-1$
		Logger.logInfo(Messages.getString("Settings.debug") + getProperty(LOG_DEBUG)); //$NON-NLS-1$
	}
	
	private static Properties getDefaults(){
		Properties props = new Properties();
		props.put(ELEXIS_DB_DRIVER, ""); //$NON-NLS-1$
		props.put(ELEXIS_DB_URL, ""); //$NON-NLS-1$
		props.put(ELEXIS_DB_USER, "sa"); //$NON-NLS-1$
		props.put(ELEXIS_DB_PWD, ""); //$NON-NLS-1$
		props.put(EXPORT_PATH, ""); //$NON-NLS-1$
		props.put(EXPORT_TAG, ""); //$NON-NLS-1$
		props.put(LOG_DEBUG, "false"); //$NON-NLS-1$
		return props;
	}
	
	public String getString(String key){
		String value = getProperty(key);
		if (value != null) {
			value = value.trim();
		}
		return value;
	}
	
	public boolean isDebug(){
		String debug = getString(Settings.LOG_DEBUG);
		if (debug != null) {
			return debug.toUpperCase().equals("TRUE"); //$NON-NLS-1$
		}
		return false;
	}
}
