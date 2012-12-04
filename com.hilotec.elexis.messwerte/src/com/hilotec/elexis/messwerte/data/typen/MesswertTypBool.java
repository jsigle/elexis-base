/*******************************************************************************
 * Copyright (c) 2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: MesswertTypBool.java 5766 2009-10-04 13:21:21Z freakypenguin $
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data.typen;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.BooleanField;
import ch.elexis.selectors.TextField;

import com.hilotec.elexis.messwerte.data.Messwert;
import com.hilotec.elexis.messwerte.data.MesswertBase;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypBool extends MesswertBase implements IMesswertTyp {
	boolean defVal;
	
	public MesswertTypBool(String n, String t, String u){
		super(n, t, u);
		defVal = false;
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		if (messwert.getWert().equals("1")) {
			return "Ja";
		}
		return (Boolean.parseBoolean(messwert.getWert()) ? "Ja" : "Nein");
	}
	
	public String getDefault(){
		return Boolean.toString(defVal);
	}
	
	public void setDefault(String def){
		defVal = Boolean.parseBoolean(def);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		Button button = new Button(parent, SWT.CHECK);
		button.setSelection(Boolean.parseBoolean(messwert.getWert()));
		return button;
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		int flags = 0;
		if (!bEditable) {
			flags |= TextField.READONLY;
		}
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		BooleanField bf = new BooleanField(parent, flags, labelText);
		bf.setText(messwert.getDarstellungswert());
		return bf;
	}
	
	public void saveInput(Widget widget, Messwert messwert){
		Button button = (Button) widget;
		messwert.setWert(Boolean.toString(button.getSelection()));
	}
}
