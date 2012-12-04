/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    R. Zweifel - SMTP-Authentifizierung
 * 
 *  $Id: Preferences.java 5952 2010-01-22 17:17:59Z rgw_ch $
 *******************************************************************************/

package ch.elexis.mail;

import java.io.File;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.data.Mandant;
import ch.elexis.preferences.SettingsPreferenceStore;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public Preferences(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.localCfg));
		setDescription(Messages.Preferences_MailSendSettings);
	}
	
	public void init(IWorkbench workbench){
		IPreferenceStore p = getPreferenceStore();
		String elexislog = p.getString(PreferenceConstants.MAIL_ELEXIS_LOG);
		if (elexislog.length() == 0) {
			elexislog = new File(Hub.getWritableUserDir(), "elexis.log").getAbsolutePath(); //$NON-NLS-1$
			p.setDefault(PreferenceConstants.MAIL_ELEXIS_LOG, elexislog);
		}
		String eclipselog = p.getString(PreferenceConstants.MAIL_ECLIPSE_LOG);
		if (eclipselog.length() == 0) {
			
		}
		String mode = p.getString(PreferenceConstants.MAIL_MODE);
		if (mode.length() == 0) {
			mode = "pop"; //$NON-NLS-1$
			p.setDefault(PreferenceConstants.MAIL_MODE, mode);
		}
		String sender = p.getString(PreferenceConstants.MAIL_SENDER);
		if (sender.length() == 0) {
			List<Mandant> ml = Hub.getMandantenList();
			if (ml.size() > 0) {
				sender = ml.get(0).getMailAddress();
				p.setDefault(PreferenceConstants.MAIL_SENDER, sender);
			}
		}
		String adressee = p.getString(PreferenceConstants.MAIL_QFA_ADDRESS);
		if (adressee.length() == 0) {
			adressee = "admin@elexis.ch"; //$NON-NLS-1$
			p.setDefault(PreferenceConstants.MAIL_QFA_ADDRESS, adressee);
		}
		String smport = p.getString(PreferenceConstants.MAIL_SMTPPORT);
		if (smport.length() == 0) {
			p.setDefault(PreferenceConstants.MAIL_SMTPPORT, "25"); //$NON-NLS-1$
		}
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new RadioGroupFieldEditor(PreferenceConstants.MAIL_MODE,
			Messages.Preferences_method, 2, new String[][] {
				{
					"IMAP", "imap" //$NON-NLS-1$ //$NON-NLS-2$
				}, {
					"POP", "pop" //$NON-NLS-1$ //$NON-NLS-2$
				}
			}, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.MAIL_SERVER,
			Messages.Preferences_mailServer, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.MAIL_SMTP,
			Messages.Preferences_SMTPServer, getFieldEditorParent()));
		
		addField(new StringFieldEditor(PreferenceConstants.MAIL_SMTPPORT,
			Messages.Preferences_SMTPServerPort, getFieldEditorParent()));
		
		addField(new StringFieldEditor(PreferenceConstants.MAIL_SENDER,
			Messages.Preferences_SenderEMail, getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(PreferenceConstants.MAIL_AUTH,
			Messages.Preferences_SMTPServerAuth, getFieldEditorParent()));
		
		addField(new StringFieldEditor(PreferenceConstants.MAIL_USER,
			Messages.Preferences_SenderEMailUser, getFieldEditorParent()));
		
		addField(new StringFieldEditor(PreferenceConstants.MAIL_PASS,
			Messages.Preferences_SenderEMailPass, getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(PreferenceConstants.MAIL_SEND_QFA,
			Messages.Preferences_SendErrorMsg, getFieldEditorParent()));
		
		addField(new StringFieldEditor(PreferenceConstants.MAIL_QFA_ADDRESS,
			Messages.Preferences_ErrorMailAdress, getFieldEditorParent()));
		addField(new FileFieldEditor(PreferenceConstants.MAIL_ELEXIS_LOG,
			Messages.Preferences_ElexisLogfile, getFieldEditorParent()));
		
		addField(new FileFieldEditor(PreferenceConstants.MAIL_ECLIPSE_LOG,
			Messages.Preferences_EclipseLogfile, getFieldEditorParent()));
		
	}
	
}
