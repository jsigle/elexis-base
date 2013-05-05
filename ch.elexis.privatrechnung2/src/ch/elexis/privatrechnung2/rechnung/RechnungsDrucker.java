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

package ch.elexis.privatrechnung2.rechnung;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.banking.ESR;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Mandant;
import ch.elexis.data.Rechnung;
import ch.elexis.data.Verrechnet;
import ch.elexis.privatrechnung2.prefs.Preferences;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.elexis.util.IRnOutputter;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Money;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class RechnungsDrucker implements IRnOutputter {
	private static final String DEFAULT_TEMPLATE = "Privatrechnung";
	
	private static final String settings = "privatrechnung/vorlage";
	String template;
	TextContainer tc;
	
	Text tVorlage;
	
	/**
	 * We'll take all sorts of bills
	 */
	public boolean canBill(final Fall fall){
		return true;
	}
	
	/**
	 * We never storno
	 */
	public boolean canStorno(final Rechnung rn){
		return false;
	}
	
	/**
	 * Create the Control that will be presented to the user before selecting the bill output
	 * target. Here we simply chose a template to use for the bill
	 */
	public Control createSettingsControl(final Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		new Label(ret, SWT.NONE).setText("Formatvorlage für Rechnung");
		tVorlage = new Text(ret, SWT.BORDER);
		tVorlage.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tVorlage.setText(Hub.globalCfg.get(settings, ""));
		tVorlage.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent ev){
				template = tVorlage.getText();
				Hub.globalCfg.set(settings, template);
			}
		});
		tVorlage.setText(Hub.globalCfg.get(settings, DEFAULT_TEMPLATE));
		return ret;
	}
	
	/**
	 * Print the bill(s)
	 */
	public Result<Rechnung> doOutput(final TYPE type, final Collection<Rechnung> rnn,
		final Properties props){
		template = tVorlage.getText();
		if (StringTool.isNothing(template)) {
			template = DEFAULT_TEMPLATE;
		}
		Hub.globalCfg.set(settings, template);
		Result<Rechnung> ret = new Result<Rechnung>(); // =new Result<Rechnung>(Log.ERRORS,99,"Not
		// yet implemented",null,true);
		
		Dialog dlg = new Dialog(Desk.getTopShell()) {
			
			@Override
			protected Control createDialogArea(Composite parent){
				// parent.setLayout(new FillLayout());
				Composite area = new Composite(parent, SWT.NONE);
				tc = new TextContainer(parent.getShell());
				
				Control container =
					tc.getPlugin().createContainer(area, new ITextPlugin.ICallback() {
						
						public void save(){
							// we don't save
						}
						
						public boolean saveAs(){
							return false; // nope
						}
					});
				
				return area;
				
			}
			
		};
		dlg.setBlockOnOpen(false);
		dlg.open();
		String printer = Hub.localCfg.get("Drucker/A4ESR/Name", null);
		
		try {
			for (Rechnung rn : rnn) {
				ret.add(doPrint(rn, tc));
				// tc.getPlugin().print(printer, null, true);
			}
		} catch (Throwable ex) {
			ex.printStackTrace(System.err);
		}
		dlg.close();
		if (!ret.isOK()) {
			ResultAdapter.displayResult(ret, "Fehler beim Rechnungsdruck");
		}
		return ret;
	}
	
	public String getDescription(){
		return "Privatrechnung auf Drucker";
	}
	
	public boolean printESR(){
		// ESR esr=new ESR();
		return false;
	}
	
	/**
	 * print a bill into a text container
	 * 
	 * @param rn
	 * @param tc
	 * @return
	 */
	public Result<Rechnung> doPrint(final Rechnung rn, final TextContainer tc){
		Fall fall = rn.getFall();
		Kontakt adressat = fall.getGarant();
		if ((adressat == null) || (!adressat.exists())) {
			adressat = fall.getPatient();
		}
		adressat.getPostAnschrift(true);
		
		tc.createFromTemplateName(null, template, Brief.RECHNUNG, adressat, rn.getNr());
		
		Result<Rechnung> ret = new Result<Rechnung>();
		List<Konsultation> kons = rn.getKonsultationen();
		Collections.sort(kons, new Comparator<Konsultation>() {
			TimeTool t0 = new TimeTool();
			TimeTool t1 = new TimeTool();
			
			public int compare(final Konsultation arg0, final Konsultation arg1){
				t0.set(arg0.getDatum());
				t1.set(arg1.getDatum());
				return t0.compareTo(t1);
			}
			
		});
		
		Mandant mandant = rn.getMandant();
		
		adressat.getPostAnschrift(true); // damit sicher eine existiert
		String userdata = rn.getRnId();
		ESR esr =
			new ESR(mandant.getInfoString(Preferences.ESRNUMBER),
				mandant.getInfoString(Preferences.ESRSUB), userdata, ESR.ESR27);
		
		Object pos = tc.getPlugin().insertText("[Leistungen]", "", SWT.LEFT);
		Money sum = new Money();
		for (Konsultation k : kons) {
			String date = new TimeTool(k.getDatum()).toString(TimeTool.DATE_GER);
			for (Verrechnet vv : k.getLeistungen()) {
				int anzahl = vv.getZahl();
				String text = vv.getText();
				Money preis = vv.getEffPreis();
				Money gesamtPreis = new Money(preis).multiply(anzahl);
				
				String line =
					anzahl + "\t" + text + "\t" + preis.getAmountAsString() + "\t"
						+ gesamtPreis.getAmountAsString() + "\n";
				
				tc.getPlugin().insertText(pos, line, SWT.LEFT);
				sum.addMoney(gesamtPreis);
			}
		}
		pos = tc.getPlugin().insertText("[Total]", sum.getAmountAsString(), SWT.LEFT);
		
		// TODO consider pre-payments
		
		Kontakt bank = Kontakt.load(mandant.getInfoString(Preferences.RNBANK));
		if (esr.printBESR(bank, adressat, mandant, sum.roundTo5().getCentsAsString(), tc) == false) {
			// TODO
		}
		
		String toPrinter = Hub.localCfg.get("Drucker/A4ESR/Name", null);
		String esrTray = Hub.localCfg.get("Drucker/A4ESR/Schacht", null); //$NON-NLS-1$
		if (StringTool.isNothing(esrTray)) {
			esrTray = null;
		}
		tc.getPlugin().print(toPrinter, esrTray, false);
		return ret;
	}
	
	public void saveComposite(){
		// TODO Auto-generated method stub
		
	}
}
