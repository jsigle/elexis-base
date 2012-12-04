/**
 * (c) 2007 by G. Weirich
 * All rights reserved
 * $Id: PreferencePage.java 116 2007-06-07 07:06:44Z gerry $
 * 
 * Adapted to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 */

package ch.elexis.laborimport.bioanalytica;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String JAR_PATH = "bioanalytica/jar_path";
	public static final String INI_PATH = "bioanalytica/ini_path";
	public static final String DL_DIR = "bioanalytica/downloaddir";
	
	public PreferencePage(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new FileFieldEditor(JAR_PATH, "OpenMedical Bibliothek (JMedTransferO.jar)",
			getFieldEditorParent()));
		addField(new FileFieldEditor(INI_PATH, "OpenMedical Konfiguration (MedTransfer.ini)",
			getFieldEditorParent()));
		addField(new DirectoryFieldEditor(DL_DIR, "Download Verzeichnis", getFieldEditorParent()));
	}
	
	public void init(IWorkbench workbench){
		// TODO Auto-generated method stub
	}
}
