/*******************************************************************************
 * Copyright (c) 2005-2009, D. Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    D. Lutz - initial implementation
 *    
 * $Id: AgendaAnzeige.java 5283 2009-05-09 16:45:09Z rgw_ch $
 *******************************************************************************/

package ch.elexis.agenda.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.agenda.Messages;
import ch.elexis.preferences.SettingsPreferenceStore;

public class AgendaAnzeige extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	SettingsPreferenceStore prefs = new SettingsPreferenceStore(Hub.userCfg);
	
	public AgendaAnzeige(){
		super(GRID);
		
		prefs.setDefault(PreferenceConstants.AG_SHOW_REASON, false);
		
		setPreferenceStore(prefs);
		
		setDescription(Messages.AgendaAnzeige_options);
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new BooleanFieldEditor(PreferenceConstants.AG_SHOW_REASON,
			Messages.AgendaAnzeige_showReason, getFieldEditorParent()));
	}
	
	@Override
	public boolean performOk(){
		prefs.flush();
		return super.performOk();
	}
	
	public void init(IWorkbench workbench){
		// TODO Auto-generated method stub
		
	}
	
}
