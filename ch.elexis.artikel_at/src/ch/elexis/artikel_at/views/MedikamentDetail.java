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
 *  $Id: MedikamentDetail.java 6333 2010-05-04 15:02:59Z marcode79 $
 *******************************************************************************/
package ch.elexis.artikel_at.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

import ch.elexis.artikel_at.data.Medikament;
import ch.elexis.views.IDetailDisplay;

public class MedikamentDetail implements IDetailDisplay {
	
	MedikamentDetailBlatt detail;
	
	public Composite createDisplay(Composite parent, IViewSite site){
		detail = new MedikamentDetailBlatt(parent);
		return detail;
		
	}
	
	public Class getElementClass(){
		return Medikament.class;
	}
	
	public String getTitle(){
		return "Medikamente (Vidal)";
	}
	
	public void display(Object obj){
		detail.display((Medikament) obj);
	}
	
}
