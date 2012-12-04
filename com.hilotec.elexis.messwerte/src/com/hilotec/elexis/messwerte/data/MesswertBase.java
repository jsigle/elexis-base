/*******************************************************************************
 * Copyright (c) 2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: MesswertBase.java 5386 2009-06-23 11:34:17Z rgw_ch $
 *******************************************************************************/

package com.hilotec.elexis.messwerte.data;

/**
 * Abstrakte Basisklasse fuer die einzelnen Messwerttypen
 * 
 * @author Antoine Kaufmann
 */
public abstract class MesswertBase {
	private String name;
	private String title;
	private String unit;
	
	public MesswertBase(String n, String t, String u){
		name = n;
		title = t;
		unit = u;
	}
	
	public String getName(){
		return name;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getUnit(){
		return unit;
	}
}
