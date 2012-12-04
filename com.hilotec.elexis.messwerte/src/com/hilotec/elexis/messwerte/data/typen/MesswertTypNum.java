/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: MesswertTypNum.java 5766 2009-10-04 13:21:21Z freakypenguin $
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data.typen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Text;

import com.hilotec.elexis.messwerte.data.Messwert;
import com.hilotec.elexis.messwerte.data.MesswertBase;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.TextField;
import ch.elexis.util.SWTHelper;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypNum extends MesswertBase implements IMesswertTyp {
	double defVal = 0.0;
	
	public MesswertTypNum(String n, String t, String u){
		super(n, t, u);
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		return messwert.getWert();
	}
	
	public String getDefault(){
		return Double.toString(defVal);
	}
	
	public void setDefault(String str){
		defVal = Double.parseDouble(str);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		Text text = SWTHelper.createText(parent, 1, SWT.NONE);
		text.setText(messwert.getWert());
		return text;
	}
	
	public void saveInput(Widget widget, Messwert messwert){
		Text text = (Text) widget;
		messwert.setWert(text.getText());
	}
	
	@Override
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		int flags = 0;
		if (!bEditable) {
			flags |= TextField.READONLY;
		}
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		if (labelText.length() == 0) {
			flags |= TextField.HIDE_LABEL;
		}
		TextField tf = new TextField(parent, flags, labelText);
		tf.setText(messwert.getDarstellungswert());
		return tf;
	}
}
