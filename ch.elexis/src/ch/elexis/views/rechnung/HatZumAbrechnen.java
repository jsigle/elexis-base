/*******************************************************************************
 * Copyright (c) 2013, Joerg Sigle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle - initial implementation
 *******************************************************************************/

package ch.elexis.views.rechnung;

import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.rgw.tools.IFilter;		//20130530js

//20130530js
//Implement a filter class that can be used with the available Tree/LazyTree/IFilter infrastructure
public class HatZumAbrechnen implements IFilter {
	//String kriterium;

	public HatZumAbrechnen(){
System.out.println("js KonsZumVerrechnen: IFilter hatZumAbrechnen aufgerufen - hier könnte man einen Filterstring/ein Array übergeben, das intern in einer Variable gespeichert wird - siehe pattern in RegexpFilter als Beispiel");
	//kriterium="Trallala";
	}

	@Override
	public boolean select(Object element) {
System.out.println("js: KonsZumVerrechnenView: hatZumAbrechnen.select() - begin");
		//System.out.println("js: KonsZumVerrechnenView: hatZumAbrechnen.select(): returning false for filter effect testing");
		//return false;
		
		Boolean result = true;
		if (element instanceof Patient) {
			Patient p = (Patient) element;
			result = p.hatFaelleZumAbrechnen();
			System.out.println("js: KonsZumVerrechnenView: hatZumAbrechnen: Patient: "+p.getName()+" "+p.getVorname()+": "+result);
		} else
		if (element instanceof Fall) {
			Fall f = (Fall) element;
			result = f.hatBehandlungenZumAbrechnen();
			System.out.println("js: KonsZumVerrechnenView: hatZumAbrechnen: Fall: "+f.getAbrechnungsSystem()+": "+result);
		} 			
System.out.println("js: KonsZumVerrechnenView: hatzumAbrechnen.select() - end - about to return "+result);
		return result;
	}
}
