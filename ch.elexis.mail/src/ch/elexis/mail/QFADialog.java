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
 *  $Id: QFADialog.java 4718 2008-12-04 10:10:16Z rgw_ch $
 *******************************************************************************/

package ch.elexis.mail;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.util.SWTHelper;

public class QFADialog extends TitleAreaDialog {
	String subject, text;
	Text tSubject, tMessage;
	
	QFADialog(Shell shell){
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		new Label(ret, SWT.NONE).setText(Messages.QFADialog_MessageTitle);
		tSubject = new Text(ret, SWT.BORDER | SWT.SINGLE);
		tSubject.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.QFADialog_MessageText);
		tMessage = SWTHelper.createText(ret, 6, SWT.BORDER);
		tSubject.setText(Messages.QFADialog_ErrorMessage + Hub.getId() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("E-Mail"); //$NON-NLS-1$
		setTitle(Messages.QFADialog_SendErrorMail);
		setMessage(Messages.QFADialog_ExplanationErrorMail);
		setTitleImage(Desk.getImage(Desk.IMG_LOGO48));
	}
	
	@Override
	protected void okPressed(){
		subject = tSubject.getText();
		text = tMessage.getText();
		super.okPressed();
	}
	
}
