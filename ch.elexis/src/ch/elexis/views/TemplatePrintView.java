/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich, Daniel Lutz and Elexis; Portions (c) 2012-2013, Joerg M. Sigle (js, jsigle)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Joerg Sigle - Added debug monitoring output.
 *    				Support improved OO/LO Textplugin usage tracking / disconnecting / unloading / where appropriate;
 *                  close previously used document containing ViewPart before re-opening another for printing (coded elsewhere)
 *                  
 *                  Exception-Handler ergänzt mit Erinnerungsdialog für eine Exception mit noch nicht nachverfolgter Ursache,
 *                  die aber den ordungsgemässen Abschluss der doPrint() verhindern würde, und dann auch soffice.bin/soffice.exe/
 *                  im Speicher lassen würde, weil dispose, noas.remove, noas.isEmpty wohl alles nicht mehr richtig laufen würde.
 *                  
 *                  TemplatePrintView.java wird referenziert in ch.elexis.util/TemplateDrucker/doPrint(Patient)
 *                  
 *                  Zum Testen:
 *                  
 *                  Patient Muster auswählen
 *                  Patient Detail
 *                  ViewMenu: KG-Deckblatt drucken (oder auch Röntgen-Blatt drucken)
 *                  
 *                  As of 20131028js funktioniert jedenfalls der Durchlauf mit Ausdruck und nachfolgendem wieder beenden/entladen von soffice.bin/soffice.exe jetzt.
 *                  
 *    Daniel Lutz - initial implementation based on RnPrintView
 *    
 *******************************************************************************/

package ch.elexis.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.data.Brief;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.TemplateDrucker;

public class TemplatePrintView extends ViewPart {
	private static final String KEY_TEXT = "text"; //$NON-NLS-1$
	
	private static final String KEY_BRIEF = "brief"; //$NON-NLS-1$
	
	public static final String ID = "ch.elexis.views.TemplatePrintView"; //$NON-NLS-1$
	
	CTabFolder ctab;
	private int existing;
	
	private TextContainer text;
	
	public TemplatePrintView(){}
	
	@Override
	public void createPartControl(Composite parent){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: createPartControl() begin");
		
		ctab = new CTabFolder(parent, SWT.BOTTOM);
		ctab.setLayout(new FillLayout());
		
		System.out.println("js ch.elexis.views/TemplatePrintView.java: createPartControl() end");
	}
	
	CTabItem addItem(final String template, final String title, final Kontakt adressat){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem() begin");

		CTabItem ret = new CTabItem(ctab, SWT.NONE);
		text = new TextContainer(getViewSite());
		ret.setControl(text.getPlugin().createContainer(ctab, new ICallback() {
			public void save(){}
			
			public boolean saveAs(){
				return false;
			}
			
		}));
		
		System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): about to Brief actBrief = text.createFromTemplateName(Konsultation.getAktuelleKons(), template, Brief.UNKNOWN, adressat, title) with");
		if (template==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): WARNING: template == null");
		else				System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): template == "+template.toString());
		if (adressat==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): WARNING: adressat == null");
		else				System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): adressat == "+adressat.toString());
		if (title==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): WARNING: title == null");
		else				System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): title == "+title.toString());
		
		Brief actBrief =
			text.createFromTemplateName(Konsultation.getAktuelleKons(), template, Brief.UNKNOWN,
				adressat, title);
		ret.setData(KEY_BRIEF, actBrief);
		ret.setData(KEY_TEXT, text);
		ret.setText(title);

		System.out.println("js ch.elexis.views/TemplatePrintView.java: addItem(): about to return ret");
		return ret;
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose(){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: begin");
		
		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: about to clearItems()");
		clearItems();
		
		//20131027js: Die text.getPlugin().dispose(); eingefügt, auf Verdacht - analog wie bei TextView, RezeptBlatt, BestellBlatt, AUFZeugnis.
		//andernfalls würde beim Schliessen der KGPrintView.java View weder soffice.bin per xDesktop.terminate entladen, noch soffice.exe per oooServer.xkill,
		//also vermutlich auch kein noas.remove; noas.isEmpty() -> bootStrapConnector.disconnect() erfolgen.
		//TODO: Die vor .dispose() geprüften Bedingungen homogenisieren zwischen allen ähnlich gebauten Modulen.
		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: about to text.getPlugin().dispose()");
		if (text != null && text.getPlugin() != null) text.getPlugin().dispose();		

		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: about to super.dispose()");
		super.dispose();

		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: end");
	}
	
	public void clearItems(){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: clearItems: begin");
		for (int i = 0; i < ctab.getItems().length; i++) {
			useItem(i, null, null);
		}
		System.out.println("js ch.elexis.views/TemplatePrintView.java: dispose: end");
	}
	
	public void useItem(int idx, String template, Kontakt adressat){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem() begin");

		CTabItem item = ctab.getItem(idx);
		
		//TODO: TemplatePrintView.java: Warum gibt's hier exceptions?
		//20131028js: Das "Brief brief = (Brief) item.getData(KEY_BRIEF)" wirft (mindestens) beim Drucken von KG-Deckblatt eine
		//org.eclipse.swt.STException: Widget is disposed. Egal, ob ich oben in dispose() mein text.getPlugin().dispose() drin habe, oder nicht.
		//Warum/Wie/Wozu weiss ich nicht und habe im Moment auch keine Zeit, das nachzuverfolgen -
		//Das mördert den korrekten weiteren Verlauf der Druckerei, vermutlich auch das korrekte Beenden. Deshalb fange ich die Exceptions hier intern mal ab.
		
		//ZUM AUSLOESEN: Patient Muster auswählen, Patient Detail, KG-Deckblatt drucken; oder Röntgen-Blatt drucken	
		try {
			System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): TODO: Bitte prüfen, warum hier exception geworfen wird");

			System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): about to Brief brief = (Brief) item.getData(KEY_BRIEF); with");
			if (KEY_BRIEF==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: KEY_BRIEF == null");
			else				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): KEY_BRIEF == "+KEY_BRIEF);
			Brief brief = (Brief) item.getData(KEY_BRIEF);
		
			System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): about to TextContainer text = (TextContainer) item.getData(KEY_TEXT); with");
			if (KEY_TEXT==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: KEY_TEXT == null");
			else				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): KEY_TEXT == "+KEY_TEXT);
			TextContainer text = (TextContainer) item.getData(KEY_TEXT);
		
			text.saveBrief(brief, Brief.UNKNOWN);
			String betreff = brief.getBetreff();
			brief.delete();
		
			if (template != null) {
			
				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): about to Brief actBrief = text.createFromTemplateName(Konsultation.getAktuelleKons(), template, Brief.UNKNOWN, adressat, betreff) with");
				if (template==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: template == null");
				else				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): template == "+template.toString());
				if (adressat==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: adressat == null");
				else				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): adressat == "+adressat.toString());
				if (betreff==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: betreff == null");
				else				System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): betreff== "+betreff.toString());

				Brief actBrief =
						text.createFromTemplateName(Konsultation.getAktuelleKons(), template,
								Brief.UNKNOWN, adressat, betreff);
				item.setData(KEY_BRIEF, actBrief);
			}
		} catch (Exception e) {
			System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): WARNING: Caught Exception for unknown reason!");

			//Ich mach mal eine deutlich sichtbare, wenn auch nervige Erinnerung hier hin, die sonst keine Nebeneffekte hat. js
			MessageBox mb =
					new MessageBox(Desk.getDisplay().getActiveShell(), SWT.ICON_INFORMATION	| SWT.OK );
				mb.setText("Exception gefangen / TODO von Jörg Sigle"); //$NON-NLS-1$
				mb.setMessage("TemplatePrintView wirft eine Exception mit unbekannter genauer Ursache.\n\nDiese wird vorläufig hier gefangen, damit eine sichtbare Erinnerung erfolgt.\n\nBitte Jörg Sigle anfragen, damit er das verbessert. Danke!"); //$NON-NLS-1$
				mb.open();
		}
			

		System.out.println("js ch.elexis.views/TemplatePrintView.java: useItem(): end");
	}
	
	/**
	 * Drukt Dokument anhand einer Vorlage
	 * 
	 * @param pat
	 *            der Patient
	 * @param templateName
	 *            Name der Vorlage
	 * @param printer
	 *            Printer
	 * @param tray
	 *            Tray
	 * @param monitor
	 * @return
	 */
	
	public boolean doPrint(Patient pat, String templateName, String printer, String tray,
		IProgressMonitor monitor){
		System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): begin");
		monitor.subTask(pat.getLabel());
		
		// TODO ?
		// GlobalEvents.getInstance().fireSelectionEvent(rn,getViewSite());
		
		existing = ctab.getItems().length;
		CTabItem ct;
		TextContainer text;
		
		if (--existing < 0) {
			ct = addItem(templateName, templateName, pat);
		} else {
			ct = ctab.getItem(0);
			useItem(0, templateName, pat);
		}

		System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): about to text = (TextContainer) ct.getData(KEY_TEXT); with");
		if (KEY_TEXT==null)	System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): WARNING: KEY_TEXT == null");
		else				System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): KEY_TEXT == "+KEY_TEXT);
		
		text = (TextContainer) ct.getData(KEY_TEXT);
		
		text.getPlugin().setFont("Serif", SWT.NORMAL, 9); //$NON-NLS-1$
		
		System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): about to text.getPlugin().print(printer,tray,false)...");

		if (text.getPlugin().print(printer, tray, false) == false) {
			System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): about return false");
			return false;
		}
		monitor.worked(1);
		System.out.println("js ch.elexis.views/TemplatePrintView.java: doPrint(): about return true");
		return true;
	}
}
