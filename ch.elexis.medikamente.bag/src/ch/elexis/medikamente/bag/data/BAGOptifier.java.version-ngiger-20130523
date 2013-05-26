/*******************************************************************************
 * Copyright (c) 2007-2011, G. Weirich and Elexis; portions Copyright (c) 2013 Joerg Sigle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    Joerg Sigle   - warning if a position is about to be billed at 0.00
 *
 *******************************************************************************/

package ch.elexis.medikamente.bag.data;

import java.util.List;

import ch.elexis.arzttarife_schweiz.Messages;
import ch.elexis.data.IVerrechenbar;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Verrechnet;
import ch.elexis.util.IOptifier;
import ch.rgw.tools.Result;

public class BAGOptifier implements IOptifier {
	
	public static final int ISZERO = 9;		
	
	public Result<Object> optify(final Konsultation kons){
		return new Result<Object>(kons);
	}
	
	public Result<IVerrechenbar> add(final IVerrechenbar code, final Konsultation kons){
		if (code instanceof BAGMedi) {
			
			// Eine Rechnung mit einer Position zu 0.00 wird von der Aerztekasse zurueckgewiesen.
			// Deshalb zumindest eine Warnung ausgeben.
			// also see: ch.elexis.arzttarife_schweiz.TarmedOptifier.java, ch.elexis.medikamente_bag.BAGOptifier.java
			if (((BAGMedi) code).getVKPreis().isZero()) {
				return new Result<IVerrechenbar>(Result.SEVERITY.WARNING, ISZERO,
						((BAGMedi) code).getCode() + " " + 
						Messages.TarmedOptifier_PriceZeroNotAllowed + " " +
						Messages.TarmedOptifier_PriceZeroAskDrugNoSellingPrice, null, false);
			}
	
			List<Verrechnet> old = kons.getLeistungen();
			for (Verrechnet v : old) {
				IVerrechenbar vv = v.getVerrechenbar();
				if (vv.getCode().equals(code.getCode())) {
					v.changeAnzahl(v.getZahl() + 1);
					// v.setZahl(v.getZahl()+1);
					return new Result<IVerrechenbar>(code);
				}
				if (vv instanceof BAGMedi) {
					BAGMedi bm = (BAGMedi) vv;
					
				}
			}
			old.add(new Verrechnet(code, kons, 1));
			
		}
		return new Result<IVerrechenbar>(code);
	}
	
	public Result<Verrechnet> remove(final Verrechnet v, final Konsultation kons){
		List<Verrechnet> old = kons.getLeistungen();
		old.remove(v);
		v.delete();
		return new Result<Verrechnet>(null);
	}
	
}
