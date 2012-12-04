/*******************************************************************************
 * Copyright (c) 2006-2007, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Lutz - initial implementation
 *    Gerry Weirich - Programmverzeichnis von OpenOffice auch hier definieren
 *    
 * $Id: OOPrefs.java 2397 2007-05-18 16:17:50Z rgw_ch $
 *******************************************************************************/
package ch.rgw.oowrapper;

import java.io.*;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.preferences.SettingsPreferenceStore;

public class OOPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String SHOW_MENU = "oowrapper/showMenu";
	public static final String SHOW_TOOLBAR = "oowrapper/showToolbar";
	DirectoryFieldEditor dfe;
	
	public OOPrefs(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
		setDescription("Einstellungen für OpenOffice");
		// noDefaultAndApplyButton();
	}
	
	public OOPrefs(String title, int style){
		super(title, style);
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new BooleanFieldEditor(SHOW_MENU, "Menü anzeigen", getFieldEditorParent()));
		addField(new BooleanFieldEditor(SHOW_TOOLBAR, "Toolbar anzeigen", getFieldEditorParent()));
		dfe =
			new DirectoryFieldEditor(PreferenceConstants.P_OOBASEDIR, "OOo-Programmverzeichnis",
				getFieldEditorParent());
		addField(dfe);
	}
	
	public void init(IWorkbench workbench){
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	@Override
	public boolean performOk(){
		File base = new File(Hub.getBasePath());
		String root = base.getParentFile().getParent();
		try {
			File infile = new File(root + File.separator + "elexis.ini");
			File outfile = new File(root + File.separator + "elexis.ini.new");
			InputStreamReader ir = new InputStreamReader(new FileInputStream(infile), "iso-8859-1");
			BufferedReader br = new BufferedReader(ir);
			OutputStreamWriter or =
				new OutputStreamWriter(new FileOutputStream(outfile), "iso-8859-1");
			String in;
			while ((in = br.readLine()) != null) {
				if (in.startsWith("-Djava.library.path=")) {
					continue;
				} else {
					or.write(in + "\n");
				}
			}
			or.write("-Djava.library.path=" + dfe.getStringValue() + "\n");
			or.close();
			br.close();
			infile.delete();
			outfile.renameTo(infile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return super.performOk();
	}
	
}
