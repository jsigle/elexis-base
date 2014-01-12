/*******************************************************************************
 * Copyright (c) 2006-2007, G. Weirich and Elexis; Portions Copyright (c) 2013 Joerg Sigle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle - Added trim() after editing meta information
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.omnivore.views;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import ch.elexis.Hub;
import ch.elexis.omnivore.data.DocHandle;
import ch.elexis.util.SWTHelper;

public class FileImportDialog extends TitleAreaDialog {
	String file;
	DocHandle dh;
	Text tTitle;
	Text tKeywords;
	public String title;
	public String keywords;
	
	public FileImportDialog(DocHandle dh){
		super(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell());
		this.dh = dh;
		file = dh.get("Titel"); //$NON-NLS-1$
	}
	
	public FileImportDialog(String name){
		super(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell());
		file = name;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		new Label(ret, SWT.NONE).setText(Messages.FileImportDialog_titleLabel);
		tTitle = SWTHelper.createText(ret, 1, SWT.NONE);
		new Label(ret, SWT.NONE).setText(Messages.FileImportDialog_keywordsLabel);
		tKeywords = SWTHelper.createText(ret, 4, SWT.NONE);
		tTitle.setText(file);
		if (dh != null) {
			tKeywords.setText(dh.get("Keywords")); //$NON-NLS-1$
		}
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		setTitle(file);
		getShell().setText(Messages.FileImportDialog_importFileCaption);
		setMessage(Messages.FileImportDialog_importFileText);
	}
	
	@Override
	protected void okPressed(){
		//20130530js - adding trim() here twice. This may make my previous improvement to DocHandle.java assimilate()
		//unnecessary, where I wrote fid.title.trim() and fid.keyword.trim(); and including the trim() here, has the
		//advantage that it not only works on original importing a file, but also when editing the meta info later on.
		//To test this functionality, enter additional leading or trailing spaces in the title or keywords fields,
		//either during initial file import, or during later editing of the meta information.
		//They should be automatically removed before the information is recorded; the document list remains clearer. 
		keywords = tKeywords.getText().trim();
		title = tTitle.getText().trim();
		if (dh != null) {
			dh.set("Titel", title); //$NON-NLS-1$
			dh.set("Keywords", keywords); //$NON-NLS-1$
		}
		super.okPressed();
	}
	
}
