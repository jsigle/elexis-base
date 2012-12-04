package ch.elexis.connect.jivex;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	static final String PREFERENCE_BASE = "connect/jivex";
	public static final String EXCHANGE_DIR = PREFERENCE_BASE + "/gdt_dir";
	
	public Preferences(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
	}
	
	@Override
	public void init(IWorkbench workbench){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new DirectoryFieldEditor(EXCHANGE_DIR, "GDT-Verzeichnis", getFieldEditorParent()));
	}
	
	@Override
	protected void performApply(){
		Hub.localCfg.flush();
	}
	
}
