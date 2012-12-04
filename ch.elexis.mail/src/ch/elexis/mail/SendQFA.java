/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: SendQFA.java 4742 2008-12-04 21:35:32Z rgw_ch $
 *******************************************************************************/

package ch.elexis.mail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Message;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.util.SWTHelper;
import ch.rgw.io.FileTool;
import ch.rgw.tools.Result;

/**
 * Qualitiy Feedback Action. Dies ist der Standard-delegate für den Menüpunkt Hilfe-Fehlermeldung
 * senden. Es werden das Elexis log und das Eclipse log gezippt und an eine Mail angehängt, die an
 * die in den Preferences angegebene Adresse gesandt wird.
 * 
 * @author gerry
 * 
 */
public class SendQFA implements IWorkbenchWindowActionDelegate {
	
	public void dispose(){
		// TODO Auto-generated method stub
		
	}
	
	public void init(IWorkbenchWindow window){
		
	}
	
	public void run(IAction action){
		
		QFADialog qfa = new QFADialog(Desk.getTopShell());
		if (qfa.open() == Dialog.OK) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ZipOutputStream zos = new ZipOutputStream(baos);
				zos.putNextEntry(new ZipEntry("elexis.log")); //$NON-NLS-1$
				FileInputStream fis =
					new FileInputStream(Hub.localCfg.get(PreferenceConstants.MAIL_ELEXIS_LOG,
						"elexis.log")); //$NON-NLS-1$
				FileTool.copyStreams(fis, zos);
				fis.close();
				zos.putNextEntry(new ZipEntry("eclipse.log")); //$NON-NLS-1$
				fis =
					new FileInputStream(Hub.localCfg.get(PreferenceConstants.MAIL_ECLIPSE_LOG,
						".log")); //$NON-NLS-1$
				FileTool.copyStreams(fis, zos);
				fis.close();
				zos.finish();
			} catch (Exception ex) {
				SWTHelper.showError(Messages.SendQFA_couldtZip, ex.getMessage());
				return;
			}
			Mailer mailer = new Mailer();
			Message msg =
				mailer.createMultipartMessage(qfa.subject,
					Hub.localCfg.get(PreferenceConstants.MAIL_SENDER, "elexisuser")); //$NON-NLS-1$
			mailer.addTextPart(msg, qfa.text);
			mailer.addBinaryPart(msg, "logs.zip", baos.toByteArray()); //$NON-NLS-1$
			Result res =
				mailer.send(msg,
					Hub.localCfg.get(PreferenceConstants.MAIL_QFA_ADDRESS, "errors@elexis.ch")); //$NON-NLS-1$
			if (res.isOK()) {
				MessageDialog.openInformation(Desk.getTopShell(), Messages.SendQFA_MessageSent,
					Messages.SendQFA_YourMessageIsSent);
			} else {
				SWTHelper.showError(Messages.SendQFA_ErrorWhileSending, res.toString());
			}
		}
		
	}
	
	public void selectionChanged(IAction action, ISelection selection){
		// TODO Auto-generated method stub
		
	}
	
}
