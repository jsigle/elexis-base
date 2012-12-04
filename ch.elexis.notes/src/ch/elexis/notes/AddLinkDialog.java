/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: AddLinkDialog.java 5056 2009-01-27 13:04:37Z rgw_ch $
 *******************************************************************************/
package ch.elexis.notes;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.Desk;
import ch.elexis.util.SWTHelper;

public class AddLinkDialog extends TitleAreaDialog {
	private Note note;
	Text tXref;
	
	AddLinkDialog(Shell shell, Note note){
		super(shell);
		this.note = note;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout(2, false));
		tXref = new Text(ret, SWT.BORDER);
		tXref.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		Button bChoose = new Button(ret, SWT.PUSH);
		bChoose.setText(Messages.AddLinkDialog_searchCaption);
		bChoose.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e){
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				String file = fd.open();
				if (file != null) {
					tXref.setText(file);
				}
			}
		});
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		setTitle(Messages.AddLinkDialog_addLinkDialogTitle);
		setMessage(Messages.AddLinkDialog_addLinkDialogMessage);
		setTitleImage(Desk.getImage(Desk.IMG_LOGO48));
	}
	
	@Override
	protected void okPressed(){
		note.addRef(tXref.getText());
		super.okPressed();
	}
	
}
