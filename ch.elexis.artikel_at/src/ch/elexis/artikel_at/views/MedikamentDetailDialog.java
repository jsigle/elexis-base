/*******************************************************************************
 * Copyright (c) 2007, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *  $Id: MedikamentDetailDialog.java 6333 2010-05-04 15:02:59Z marcode79 $
 *******************************************************************************/
package ch.elexis.artikel_at.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.artikel_at.data.Medikament;

public class MedikamentDetailDialog extends Dialog {
	Medikament med;
	
	public MedikamentDetailDialog(Shell parent, Medikament medi){
		super(parent);
		med = medi;
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Medikament-Detail");
		getShell().setSize(800, SWT.DEFAULT);
		getShell().pack();
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		MedikamentDetailBlatt mdb = new MedikamentDetailBlatt(parent);
		mdb.display(med);
		return mdb;
	}
	
}
