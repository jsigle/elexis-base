// Copyright 2010 (c) Niklaus Giger <niklaus.giger@member.fsf.org>
/**
 * (c) 2007-2010 by G. Weirich
 * All rights reserved
 * 
 * This plug-in provides only a importer for one laboratory. 
 * All the rest is done generically. See plug-in elexis-importer.
 * 
 */

package ch.elexis.laborimport.RischBern;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String JAR_PATH = "rischbern/jar_path"; //$NON-NLS-1$
	public static final String INI_PATH = "rischbern/ini_path"; //$NON-NLS-1$
	public static final String DL_DIR = "rischbern/downloaddir"; //$NON-NLS-1$
	
	public PreferencePage(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new FileFieldEditor(JAR_PATH, Messages.PreferencePage_JMedTrasferJar,
			getFieldEditorParent()));
		addField(new FileFieldEditor(INI_PATH, Messages.PreferencePage_JMedTrasferJni,
			getFieldEditorParent()));
		addField(new DirectoryFieldEditor(DL_DIR, Messages.PreferencePage_DownloadDir,
			getFieldEditorParent()));
	}
	
	public void init(final IWorkbench workbench){
		// TODO Auto-generated method stub
	}
}
