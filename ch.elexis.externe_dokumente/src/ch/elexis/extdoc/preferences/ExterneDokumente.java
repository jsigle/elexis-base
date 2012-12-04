/*******************************************************************************
 * Copyright (c) 2006-2011, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Lutz - initial implementation
 *    Niklaus Giger - new layout with subdirectories
 *    
 *******************************************************************************/
package ch.elexis.extdoc.preferences;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.extdoc.Messages;

/**
 * Einstellungen zur Verknüpfung externen Dokumenten
 * 
 * @author Daniel Lutz
 */
public class ExterneDokumente extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public ExterneDokumente(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
		setDescription(Messages.ExterneDokumente_externe_dokumente);
	}
	
	@Override
	protected void createFieldEditors(){
		DirectoryFieldEditor dfe;
		StringFieldEditor sfe;
		PreferenceConstants.PathElement[] prefElems = PreferenceConstants.getPrefenceElements();
		for (int j = 0; j < prefElems.length; j++) {
			sfe =
				new StringFieldEditor(prefElems[j].prefName, String.format(
					Messages.ExterneDokumente_shorthand_for_path, j), getFieldEditorParent());
			sfe.setTextLimit(8);
			addField(sfe);
			dfe =
				new DirectoryFieldEditor(prefElems[j].prefBaseDir,
					Messages.ExterneDokumente_path_name_preference, getFieldEditorParent());
			addField(dfe);
		}
	}
	
	public void init(IWorkbench workbench){}
}
