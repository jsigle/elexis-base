package com.hilotec.elexis.messwerte.views;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.preferences.inputs.InexistingFileOKFileFieldEditor;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String CONFIG_FILE = "findings/hilotec/configfile";
	
	public void init(IWorkbench workbench){
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new InexistingFileOKFileFieldEditor(CONFIG_FILE, "Konfigurationsdatei",
			getFieldEditorParent()));
		
	}
	
	@Override
	public void performApply(){
		Hub.localCfg.flush();
	}
	
}
