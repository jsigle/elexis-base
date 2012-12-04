/**
 * (c) 2007-2008 by G. Weirich
 * All rights reserved
 * 
 * From: Laborimport Viollier
 * 
 * Adapted to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 * Adapted to Risch by Gerry Weirich
 * Adapted to Viollier by Gerry Weirich
 * 
 * $Id: PreferencePage.java 3516 2008-01-11 10:32:42Z rgw_ch $
 */

package ch.elexis.laborimport.viollier;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String JAR_PATH = "viollier/jar_path";
	public static final String INI_PATH = "viollier/ini_path";
	public static final String DL_DIR = "viollier/downloaddir";
	
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
	
	public void init(final IWorkbench workbench){
		// TODO Auto-generated method stub
	}
}
