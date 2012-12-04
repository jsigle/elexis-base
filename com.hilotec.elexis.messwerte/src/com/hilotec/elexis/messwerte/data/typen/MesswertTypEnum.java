/*******************************************************************************
 * Copyright (c)2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: MesswertTypEnum.java 5766 2009-10-04 13:21:21Z freakypenguin $
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data.typen;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import com.hilotec.elexis.messwerte.data.Messwert;
import com.hilotec.elexis.messwerte.data.MesswertBase;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.ComboField;
import ch.elexis.selectors.TextField;
import ch.rgw.tools.StringTool;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypEnum extends MesswertBase implements IMesswertTyp {
	int defVal = 0;
	
	/**
	 * Bezeichnungen fuer die einzelnen Auswahlmoeglichkeiten
	 */
	ArrayList<String> choices = new ArrayList<String>();
	
	/**
	 * Werte fuer die Auswahlmoeglichkeiten. (notwendig, da die Combo nur fortlaufende Werte nimmt.
	 */
	ArrayList<Integer> values = new ArrayList<Integer>();
	
	public MesswertTypEnum(String n, String t, String u){
		super(n, t, u);
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		int wert = StringTool.parseSafeInt(messwert.getWert());
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) == wert) {
				return choices.get(i);
			}
		}
		return "";
	}
	
	public String getDefault(){
		return Integer.toString(defVal);
	}
	
	public void setDefault(String str){
		defVal = StringTool.parseSafeInt(str);
	}
	
	/**
	 * Neue Auswahlmoeglichkeit fuer dieses Enum-Feld anfuegen
	 * 
	 * @param c
	 *            Beschriftung dieser Auswahlmoeglichkeit
	 * @param v
	 *            Wert fuer diese Auswahlmoeglichkeit
	 */
	public void addChoice(String c, int v){
		choices.add(c);
		values.add(v);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		Combo combo = new Combo(parent, SWT.DROP_DOWN);
		for (int i = 0; i < choices.size(); i++) {
			combo.add(choices.get(i), i);
		}
		
		int wert = Integer.parseInt(messwert.getWert());
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i).compareTo(wert) == 0) {
				combo.select(i);
				break;
			}
		}
		
		return combo;
	}
	
	@Override
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		int flags = 0;
		if (!bEditable) {
			flags |= TextField.READONLY;
		}
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		ComboField cf = new ComboField(parent, flags, labelText, choices.toArray(new String[0]));
		cf.setText(messwert.getDarstellungswert());
		return cf;
	}
	
	public void saveInput(Widget widget, Messwert messwert){
		Combo combo = (Combo) widget;
		messwert.setWert(Integer.toString(values.get(combo.getSelectionIndex())));
	}
	
}
