/*******************************************************************************
 * Copyright (c) 2007-2009, Daniel Lutz and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Lutz - initial implementation
 *    
 *  $Id: UserCasePreferences.java 5320 2009-05-27 16:51:14Z rgw_ch $
 *******************************************************************************/
package ch.elexis.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.data.Fall;
import ch.elexis.data.PersistentObject;
import ch.elexis.dialogs.DiagnoseSelektor;
import ch.rgw.io.InMemorySettings;

/**
 * User specific settings: Case defaults
 */
public class UserCasePreferences extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	
	public static final String ID = "ch.elexis.preferences.UserCasePreferences"; //$NON-NLS-1$
	
	Text diagnoseTxt;
	
	public UserCasePreferences(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(new InMemorySettings()));
		setDescription(Messages.UserCasePreferences_Cases);
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFCASELABEL,
			Messages.UserCasePreferences_DefaultName, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFCASEREASON,
			Messages.UserCasePreferences_DefaultReason, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.USR_DEFLAW,
			Messages.UserCasePreferences_DefaultBillingSystem, getFieldEditorParent()));
	}
	
	public void init(IWorkbench workbench){
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFCASELABEL,
			Fall.getDefaultCaseLabel());
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFCASEREASON,
			Fall.getDefaultCaseReason());
		getPreferenceStore().setValue(PreferenceConstants.USR_DEFLAW, Fall.getDefaultCaseLaw());
	}
	
	@Override
	public boolean performOk(){
		super.performOk();
		
		Hub.userCfg.set(PreferenceConstants.USR_DEFCASELABEL,
			getPreferenceStore().getString(PreferenceConstants.USR_DEFCASELABEL));
		Hub.userCfg.set(PreferenceConstants.USR_DEFCASEREASON,
			getPreferenceStore().getString(PreferenceConstants.USR_DEFCASEREASON));
		Hub.userCfg.set(PreferenceConstants.USR_DEFLAW,
			getPreferenceStore().getString(PreferenceConstants.USR_DEFLAW));
		return true;
	}
	
	@Override
	protected Control createContents(Composite parent){
		// create the field editors by calling super
		Control suParent = super.createContents(parent);
		// create a composite for selecting the default diagnose
		Composite diagnoseParent = new Composite((Composite) suParent, SWT.NULL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		diagnoseParent.setLayoutData(gd);
		
		diagnoseParent.setLayout(new FormLayout());
		Label diagnoseLbl = new Label(diagnoseParent, SWT.NONE);
		diagnoseLbl.setText(Messages.UserCasePreferences_DefaultDiagnose);
		diagnoseTxt = new Text(diagnoseParent, SWT.BORDER);
		diagnoseTxt.setEditable(false);
		String diagnoseId = Hub.userCfg.get(PreferenceConstants.USR_DEFDIAGNOSE, "");
		if (diagnoseId.length() > 1) {
			PersistentObject diagnose = Hub.poFactory.createFromString(diagnoseId);
			if (diagnose != null)
				diagnoseTxt.setText(diagnose.getLabel());
		}
		Button diagnoseBtn = new Button(diagnoseParent, SWT.PUSH);
		diagnoseBtn.setText("Diagnose"); //$NON-NLS-1$
		diagnoseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				DiagnoseSelektor dsl = new DiagnoseSelektor(getShell());
				if (dsl.open() == Dialog.OK) {
					Object[] sel = dsl.getResult();
					if (sel != null && sel.length > 0) {
						PersistentObject diagnose = (PersistentObject) sel[0];
						Hub.userCfg.set(PreferenceConstants.USR_DEFDIAGNOSE,
							diagnose.storeToString());
						diagnoseTxt.setText(diagnose.getLabel());
					} else {
						Hub.userCfg.set(PreferenceConstants.USR_DEFDIAGNOSE, "");
						diagnoseTxt.setText("");
					}
				}
			}
		});
		
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 0);
		diagnoseLbl.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(diagnoseLbl, 0, SWT.CENTER);
		fd.left = new FormAttachment(diagnoseLbl, 5);
		fd.right = new FormAttachment(diagnoseBtn, -5);
		diagnoseTxt.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(diagnoseLbl, 0, SWT.CENTER);
		fd.right = new FormAttachment(100, -5);
		diagnoseBtn.setLayoutData(fd);
		
		return suParent;
	}
	
	@Override
	protected void performDefaults(){
		this.initialize();
	}
}
