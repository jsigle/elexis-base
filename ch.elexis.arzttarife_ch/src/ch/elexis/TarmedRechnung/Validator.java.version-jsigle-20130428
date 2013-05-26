/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; portions Copyright (c) 2013 Joerg Sigle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    Joerg Sigle   - warning if a case containes no billables or a total turnover of zero
 *    
 *******************************************************************************/

package ch.elexis.TarmedRechnung;

//201303130626js: WARNING: A lot of code from this file also exists in ch.elexis.data.Rechnung.java.
//But over there, maybe it is in a less advanced state, regarding modularization and internationalization.
//But that file over there appears to be actually used. 


import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.tarmedprefs.TarmedRequirements;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;

public class Validator {
	
	public Result<Rechnung> checkBill(final XMLExporter xp, final Result<Rechnung> res){
		System.out.println("js Validator: checkBill(2) begin");
		
		System.out.println("js Validator: checkBill(2): TO DO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js Validator: checkBill(2): TO DO: Apparently, this code is not used.");
		System.out.println("js Validator: checkBill(2): TO DO: But ch.elexis.data.Rechnung.java is used instead.");
		System.out.println("js Validator: checkBill(2): TO DO: Why are both of them in the code?");
		System.out.println("js Validator: checkBill(2): TO DO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		Rechnung rn = xp.rn;
		Kontakt m = rn.getMandant();
		if (rn.getStatus() > RnStatus.OFFEN) {
			return res; // Wenn sie eh schon gedruckt war machen wir kein Büro mehr auf
		}
		
		System.out.println("js Validator: checkBill(2): TO DO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js Validator: checkBill(2): TO DO: Check if each patient is a person.");
		System.out.println("js Validator: checkBill(2): TO DO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		if ((m == null) || (!m.isValid())) {
			rn.reject(RnStatus.REJECTCODE.NO_MANDATOR, Messages.Validator_NoMandator);
			res.add(Result.SEVERITY.ERROR, 2, Messages.Validator_NoMandator, rn, true);
			
		}
		
		Fall fall = rn.getFall();
		
		if ((fall == null) || (!fall.isValid())) {
			rn.reject(RnStatus.REJECTCODE.NO_CASE, Messages.Validator_NoCase);
			res.add(Result.SEVERITY.ERROR, 4, Messages.Validator_NoCase, rn, true);
		}

		//201303130500js: Lets check whether a consultation contains a non-Zero sum
		//of recorded Verrechnungen. This will interrupt creation of a bill for a case
		//that has at least one consultation where the sum of all verrechnungen is 0.00 -
		//or, where no Verrechnung has been recorded yet.
		//As this Validator.checkBill() is apparently not called when I create an invoice for a case,
		//I copy/adopt this code over to Rechnung.build(); where other coarse checks are already located (and used) as well.
		//In the long term, I guess that this code here should replace the code over there. Best bet will be to ask Gerry/Niklaus.
		System.out.println("js Validator: checkBill(2): Check whether all consultations contain Verrechnungen > 0 and a total sum > 0");
		System.out.println("js Validator: checkBill(2): TODO: This functionality might possibly be moved to Fall.isValid()");
		System.out.println("js Validator: checkBill(2): TODO: or even by definition of Abrechnungsregeln outside of the program.");
		if ((fall != null) && (fall.isValid())) {
			Konsultation[]	konsultationen	= fall.getBehandlungen(false);
			int nk = konsultationen.length;		//number of consultations in this fall	
			System.out.println("js Validator: checkBill(2): number of consultations: "+nk);
			if (nk>0)							//most probably already checked in fall.isValid, so paranoia doing it here, and incomplete paranoia:
				for (int i=0;i<nk;i++) {		//cause *if* nk==0, we should also produce an error message (but that should indeed have been covered above).
					if (konsultationen[i].getLeistungen().isEmpty()
						||
						konsultationen[i].getUmsatz()==0) {
							rn.reject(RnStatus.REJECTCODE.RG_KONS_NO_BILLABLES_NOR_REVENUE, Messages.Validator_RgWithKonsWithoutBillablesNorRevenue);
							res.add(Result.SEVERITY.ERROR, 9, Messages.Validator_RgWithKonsWithoutBillablesNorRevenue, rn, true);
						}
					}
				}		
		
		/*
		 * String g=fall.getGesetz(); if(g.equalsIgnoreCase(Fall.LAW_OTHER)){ return res; }
		 */
		String ean = TarmedRequirements.getEAN(m);
		if (StringTool.isNothing(ean)) {
			rn.reject(RnStatus.REJECTCODE.NO_MANDATOR, Messages.Validator_NoEAN);
			res.add(Result.SEVERITY.ERROR, 3, Messages.Validator_NoEAN, rn, true);
		}
		Kontakt kostentraeger = fall.getRequiredContact(TarmedRequirements.INSURANCE);
		if (kostentraeger == null) {
			rn.reject(RnStatus.REJECTCODE.NO_GUARANTOR, Messages.Validator_NoName);
			res.add(Result.SEVERITY.ERROR, 7, Messages.Validator_NoName, rn, true);
			return res;
		}
		ean = TarmedRequirements.getEAN(kostentraeger);
		
		if (StringTool.isNothing(ean) || (!ean.matches(TarmedRequirements.EAN_PATTERN))) {
			rn.reject(RnStatus.REJECTCODE.NO_GUARANTOR, Messages.Validator_NoEAN2);
			res.add(Result.SEVERITY.ERROR, 6, Messages.Validator_NoEAN2, rn, true);
		}
		String bez = kostentraeger.get(Kontakt.FLD_NAME1);
		if (StringTool.isNothing(bez)) {
			rn.reject(RnStatus.REJECTCODE.NO_GUARANTOR, Messages.Validator_NoName);
			res.add(Result.SEVERITY.ERROR, 7, Messages.Validator_NoName, rn, true);
		}
		if (StringTool.isNothing(xp.diagnosen)) {
			rn.reject(RnStatus.REJECTCODE.NO_DIAG, Messages.Validator_NoDiagnosis);
			res.add(Result.SEVERITY.ERROR, 8, Messages.Validator_NoDiagnosis, rn, true);
		}

		System.out.println("js Validator: checkBill(2) returning res="+res.toString());
		return res;
	}
}
