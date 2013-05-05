/*******************************************************************************
 * Copyright (c) 2007-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.eigendiagnosen.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import ch.elexis.Desk;
import ch.elexis.eigendiagnosen.data.Eigendiagnose;
import ch.elexis.util.LabeledInputField;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.LabeledInputField.InputData;
import ch.elexis.views.IDetailDisplay;

public class DetailDisplay implements IDetailDisplay {
	Form form;
	LabeledInputField.AutoForm tblPls;
	InputData[] data = new InputData[] {
		new InputData("Kuerzel"), //$NON-NLS-1$
		new InputData("Text"),
	};
	Text tComment;
	
	public Composite createDisplay(Composite parent, IViewSite site){
		form = Desk.getToolkit().createForm(parent);
		TableWrapLayout twl = new TableWrapLayout();
		form.getBody().setLayout(twl);
		
		tblPls = new LabeledInputField.AutoForm(form.getBody(), data);
		
		TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.grabHorizontal = true;
		tblPls.setLayoutData(twd);
		TableWrapData twd2 = new TableWrapData(TableWrapData.FILL_GRAB);
		tComment = Desk.getToolkit().createText(form.getBody(), "", SWT.BORDER);
		tComment.setLayoutData(twd2);
		return form.getBody();
		
	}
	
	public void display(Object obj){
		if (obj instanceof Eigendiagnose) { // should always be true...
			Eigendiagnose ls = (Eigendiagnose) obj;
			form.setText(ls.getLabel());
			tblPls.reload(ls);
			tComment.setText(ls.get("Kommentar"));
		}
	}
	
	public Class getElementClass(){
		return Eigendiagnose.class;
	}
	
	public String getTitle(){
		return Eigendiagnose.CODESYSTEM_NAME;
	}
	
}
