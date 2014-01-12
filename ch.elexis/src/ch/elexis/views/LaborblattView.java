/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle   - added StatusMonitor, reliable activation on typing, reliable saving, automatic saving depending on passed time since last save and last isModified() 
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

/**
 * TODO: 20131027js: I noticed that before - but: Please review naming conventions: Briefauswahl.java (document selection/controller dialog) and TextView.java (document display/editor), vs. RezepteView.java (selection/controller) and RezeptBlatt.java (display/editor), etc. for Bestellung, AUFZeugnis and maybe more similar combinations. This inconsistency is highly confusing if you want to do updates throughout all external document processing plugins/classes/etc. 
 */

package ch.elexis.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.jdom.Document;
import org.jdom.Element;

import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.data.Brief;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.text.TextContainer;

import ch.elexis.util.IStatusMonitorCallback;	//201306170935js - ensure edits in text documents are noted by Elexis and ultimately stored

public class LaborblattView extends ViewPart implements ICallback, IStatusMonitorCallback {
	public static final String ID = "ch.elexis.Laborblatt"; //$NON-NLS-1$
	TextContainer text;
	//201306252207js Added actBrief for similarity re StatusMonitor with TextView, LaborblattView, AUFZeugniss BestellBlatt, Laborblatt...
	Brief actBrief;
	
	public LaborblattView(){}
	
	//201306252207js Added dispose() for similarity re StatusMonitor with TextView, LaborblattView, AUFZeugniss BestellBlatt, Laborblatt...
	@Override
	public void dispose(){
		System.out.println("\njs ch.elexis.views/LaborblattView.java dispose(): begin");

		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//201306161401js
		ch.elexis.util.StatusMonitor.removeMonitorEntry(actBrief);	//hopefully, this is a suitable variable here.
		
		//20130425js: Nach Einfügen der folgenden Zeile wird er NOText closeListener mit queryClosing() und notifyClosing() tatsächlich aufgerufen,
		//in der Folge wird dann auch OO beendet, wenn das Letzte NOAText Fenster geschlossen wurde.
		//UND ich kann danach sogar Elexis beenden, ohne dass es hängenbleibt, weil es selbst erst noch OO beenden wollte...
		//Jippieh - zumindest folgendes funktioniert jetzt (as of 201304250337js):
		//
		//Ausgangsbasis jeweils: soffice.bin und soffice.exe sind NICHT im Speicher.
		//
		//Elexis starten - muster max - brief doppelklicken (implizit wird AOO geladen) - BriefInhalt erscheint im Briefe Fenster -
		//	Dieses Fenster schliessen - soffice.bin und soffice.exe werden entladen - Elexis beenden - problemlos.  
		//
		//Elexis starten - muster max - brief doppelklicken (implizit wird AOO geladen) - BriefInhalt erscheint im Briefe Fenster -
		//	Elexis direkt schliessen - soffice.bin und soffice.exe werden entladen - Elexis beendet sich problemlos.
		//
		
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): ToDo: SOLLTE hier ein plugin().dispose() rein - siehe Kommentare - oder würde das im Betrieb nur unerwünscht Exceptions werfen (gerade gesehen)?");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		//20131027js: Die text.getPlugin().dispose(); wieder aktiviert,
		//andernfalls würde beim Schliessen der RezeptBlatt.java View weder soffice.bin per xDesktop.terminate entladen, noch soffice.exe per oooServer.xkill,
		//also vermutlich auch kein noas.remove; noas.isEmpty() -> bootStrapConnector.disconnect() erfolgen.
		//YEP, seit ich das wieder aktiviert habe, verschwinden das geladene soffice.bin und soffice.exe nach Schliessen der RezeptBlatt View,
		//jedenfalls bei nur einem offenen Elexis, und nur diesem offenen OO Dokument - so ist das auch gedacht. 
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): about to txt.getPlugin().dispose()");
		text.getPlugin().dispose();		
		
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): ToDo: WARNING / PLEASE REVIEW: The method GlobalEventDispatcher.removeActivationListener(LaborblattView, LaborblattView) is not applicable.");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): ToDo: Das LaborblattView hat auch 1 Jahr älteres Copyright - wird das dort nicht benötigt, oder wurde es nur noch nicht nachgetragen?");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//TODO: Bitte bei LaborblattView wie bei BestellBlatt.dispose() nachschauen, ob GlobalEventDispatcher.removeActivationListener(this,this) aktivierbar ist - siehe dort TODO notes in System.out.printl(), und vergleiche mit Rezepte.View!
		//System.out.println("js ch.elexis.views/LaborblattView.java dispose(): about to GlobalEventDispatcher.removeActivationListener()...");
		//GlobalEventDispatcher.removeActivationListener(this, this);

		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): about super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.views/LaborblattView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		super.dispose();
	}

	
	@Override
	public void createPartControl(Composite parent){
		text = new TextContainer(getViewSite());
		text.getPlugin().createContainer(parent, this);
		
	}
	
	@Override
	public void setFocus(){
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public boolean createLaborblatt(final Patient pat, final String[] header, final TableItem[] rows){
		System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(final Patient pat, final String[] header, final TableItem[] rows): begin");

		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		
		
		Brief br =
			text.createFromTemplateName(Konsultation.getAktuelleKons(),
				Messages.getString("LaborblattView.LabTemplateName"), Brief.LABOR, pat, null); //$NON-NLS-1$
		if (br == null) {
			System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(final Patient pat, final String[] header, final TableItem[] rows): br == null -> nop; early return false");
			return false;
		}
		
		Table table = rows[0].getParent();
		int cols = table.getColumnCount();
		int[] colsizes = new int[cols];
		float first = 25;
		float second = 10;
		if (cols > 2) {
			int rest = Math.round((100f - first - second) / (cols - 2f));
			for (int i = 2; i < cols; i++) {
				colsizes[i] = rest;
			}
		}
		colsizes[0] = Math.round(first);
		colsizes[1] = Math.round(second);
		
		LinkedList<String[]> usedRows = new LinkedList<String[]>();
		usedRows.add(header);
		for (int i = 0; i < rows.length; i++) {
			boolean used = false;
			String[] row = new String[cols];
			for (int j = 0; j < cols; j++) {
				row[j] = rows[i].getText(j);
				if ((j > 1) && (row[j].length() > 0)) {
					used = true;
					// break;
				}
			}
			if (used == true) {
				usedRows.add(row);
			}
		}
		String[][] fld = usedRows.toArray(new String[0][]);
		boolean ret = text.getPlugin().insertTable("[Laborwerte]", //$NON-NLS-1$
			ITextPlugin.FIRST_ROW_IS_HEADER, fld, colsizes);
		text.saveBrief(br, Brief.LABOR);

		//201306252207js Added actBrief for similarity re StatusMonitor with TextView, LaborblattView, AUFZeugniss BestellBlatt, Laborblatt...
		actBrief = br;
		//201306161205js: Now also add a statusMonitor entry:
		text.getPlugin().setBriefServicedByThis(actBrief);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBrief, this, this);		

		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): Schauen, wie das mit TextView.createDocument korrespondiert. Dort: setName(); hier unten: setBrief(),->setLetterID oder ähnlich...?");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): Wird ein nützlicher Fenstertitel gesetzt?");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		
		System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(final Patient pat, final String[] header, final TableItem[] rows): about to return ret == " + ret);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public boolean createLaborblatt(Patient pat, Document doc){
		System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(Patient pat, Document doc): begin");

		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/LaborblattView.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		

		//201306250439js found: 
		///* Brief br= */text.createFromTemplateName(Konsultation.getAktuelleKons(),
		//reenabled storage of result in br; which I will need to add the StatusMonitor functionality.
		Brief br = text.createFromTemplateName(Konsultation.getAktuelleKons(),
			Messages.getString("LaborblattView.LabTemplateName"), Brief.LABOR, pat, null); //$NON-NLS-1$
		
		ArrayList<String[]> rows = new ArrayList<String[]>();
		Element root = doc.getRootElement();
		String druckdat = root.getAttributeValue(Messages.getString("LaborblattView.created")); //$NON-NLS-1$
		Element daten = root.getChild("Daten"); //$NON-NLS-1$
		List datlist = daten.getChildren();
		int cols = datlist.size() + 1;
		String[] firstline = new String[cols];
		firstline[0] = druckdat;
		for (int i = 1; i < cols; i++) {
			Element dat = (Element) datlist.get(i - 1);
			firstline[i] = dat.getAttributeValue("Tag"); //$NON-NLS-1$
		}
		rows.add(firstline);
		List groups = root.getChildren("Gruppe"); //$NON-NLS-1$
		for (Element el : (List<Element>) groups) {
			rows.add(new String[] {
				el.getAttribute("Name").getValue()}); //$NON-NLS-1$
			List<Element> params = el.getChildren("Parameter"); //$NON-NLS-1$
			for (Element param : params) {
				Element ref = param.getChild("Referenz"); //$NON-NLS-1$
				String[] row = new String[cols];
				StringBuilder sb = new StringBuilder();
				sb.append(param.getAttributeValue("Name")).append(" (").append( //$NON-NLS-1$ //$NON-NLS-2$
					ref.getAttributeValue("min")).append("-").append( //$NON-NLS-1$ //$NON-NLS-2$
					ref.getAttributeValue("max")).append(") ").append( //$NON-NLS-1$ //$NON-NLS-2$
					param.getAttributeValue("Einheit")); //$NON-NLS-1$
				row[0] = sb.toString();
				List<Element> results = param.getChildren("Resultat"); //$NON-NLS-1$
				int i = 1;
				for (Element result : results) {
					row[i++] = result.getValue();
				}
				rows.add(row);
			}
		}

		//201306252207js Added actBrief for similarity re StatusMonitor with TextView, LaborblattView, AUFZeugniss BestellBlatt, Laborblatt...
		actBrief = br;
		//201306161205js: Now also add a statusMonitor entry:
		text.getPlugin().setBriefServicedByThis(actBrief);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBrief, this, this);		

		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): Schauen, wie das mit TextView.createDocument korrespondiert. Dort: setName(); hier unten: setBrief(),->setLetterID oder ähnlich...?");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): Wird ein nützlicher Fenstertitel gesetzt?");
		System.out.println("js ch.elexis.views/LaborblattView.java createLaborblatt(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		
		
		if (text.getPlugin().insertTable("[Laborwerte]", //$NON-NLS-1$
			ITextPlugin.FIRST_ROW_IS_HEADER, rows.toArray(new String[0][]), null)) {
			if (text.getPlugin().isDirectOutput()) {
				text.getPlugin().print(null, null, true);
				getSite().getPage().hideView(this);
				System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(Patient pat, Document doc): about to return true");
				return true;
			}
		}
		System.out.println("\njs ch.elexis.views/LaborblattView.java createLaborblatt(Patient pat, Document doc): about to return false");
		return false;
		
	}
	
	//201306170655js: I implement this additional callback method to give StatusMonitor a possibility to trigger a ShowView from TextView.java
	//Required, as getView() is not easily accessible from StatusMonitor (and not in a number of more complicated ways I tried either),
	//and moreover, we can keep the TextView.ID etc. up here, and do similar but yet specific things for all other corresponding text processing windows -
	//just letting each of them specify whatever they want to have called from StatusMonitor.java
	//class ShowViewHandler implements IStatusMonitorCallback {

	//In TextView.java, SaveHandler was a separate class implementing ICallback with its save() and saveAs() methods.
	//Also, I added ShowViewHandler as another separate class implementing IStatusMonitorCallback with its showView method.
	//There, we used:
	//ch.elexis.util.StatusMonitor.addMonitorEntry(..., new SaveHandler(), new ShowViewHandler());
	//In LaborblattView.java, LaborblattView directly implements ICallback, and I added that it also directly implements IStatusMonitorCallback.
	//Especially, because supplying ShowViewHandler() to LaborblattView...addMonitoring would activate the TextView window (Briefe),
	//but not the RezetpBlatt window (Rezept). No, sorry - that was more probably because the NOAText based isModified() event handler set
	//the isModified() flag always for the TextView related statusMonitor entry, and not for the LaborblattView related entry.
	
	//So LaborblattView() has to replace both SaveHandler() and ShowViewHandler().
	//And as we do not want a *new* LaborblattView to be called, but the existing one instead, we might just as well supply (..., this, this).
	//ToDo: Please homogenize, if possible. Quite possibly, Textview might be changed to become similar to LaborblattView etc.

	@Override
	public void showView() {
		//Any Eclipse RCP Display related code must be run in the Display thread,
		//because otherwise it would cause an Invalid thread access Exception.
		//We can run it sync or async, here I chose the async:
		Display.getDefault().asyncExec(new Runnable() {
		    public void run() {				    	
		    	/*
		    	 * Strangely, I see: isEnabled = true; isFocusContrl = false, when kbd action goes to the Office win but it appears inactive.
		    	 * 
		    	 * textContainer.isEnabled(): 		Wird false, wenn das Fenster minimized ist; ansonsten immer true, 
		    	 *									selbst wenn die View nicht den aktiven Rahmen hat, oder ich eine andere View aktiviere = anklicke.
		    	 *
		    	 * textContainer.isFocusControl():	Sehe ich die ganze Zeit als false, auch wenn kbd action in den Office Window Inhalt geht.
		    	 *
		    	 */
		    	//System.out.println("js ch.elexis.views/LaborblattView.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isFocusControl(): " + textContainer.isFocusControl());
				//System.out.println("js ch.elexis.views/LaborblattView.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isEnabled():      " + textContainer.isEnabled());
					
				//DAS BITTE NUR, WENN isModified() akut unten gesetzt wurde!
				//Sonst kann man Elexis ausserhalb des TextPluginWindows nur noch sehr schlecht steuern.
		
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - PLEASE IMPLEMENT Activation of the correct office window!");								
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - Done: TextView.java");								
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - Done: LaborblattView.java, AUFZeugnis, BestellBlatt, LaborblattView.java, ");								
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - ToDo: ... ");								
				System.out.println("js com.jsigle.noa/LaborblattView.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
		    	
				//YEP. DAS macht die View aktiv, incl. hervorgehobenem Rahmen, und Focus, in dem der Text drinnen steckt.
				//Im Moment leider noch alle Zeit, also auch dann, wenn gerade NICHT isModified() durch neue Eingaben immer wieder gesetzt würde.
				//TextView.ID liefert: ch.elexis.TextView
				LaborblattView lb = null;
				try {
					System.out.println("js com.jsigle.noa/LaborblattView.java - run() - Thread: " + Thread.currentThread().getName() + " - about to rb.showView(LaborblattView.ID) with LaborblattView.ID == " + LaborblattView.ID);
					lb = (LaborblattView) getViewSite().getPage().showView(LaborblattView.ID /*,StringTool.unique("textView"),IWorkbenchPage.VIEW_ACTIVATE*/);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//Ein textcontainer.setEnabled(true) dürfte vermutlich die Briefe-View "wiederherstellen", wenn sie minimiert war.
				//textContainer.setEnabled(true);
				
		    	//Ein textContainer.setFocus() alleine scheint immer wieder den Keyboard Focus auf das Briefe-View zu setzen,
				//nachdem ich ihn woanders hingesetzt habe. Jedoch auch das, ohne dass der Rahmen der View sichtbar "aktiv" wird!
				//textContainer.setFocus(); 
		    }
		});
	}
	//}

	//In TextView.java, there was:
	//201306170655js: This pre-existing SaveHandler can also be used with/from my new ch.elexis.util.StatusMonitoring class.
	//class SaveHandler implements ITextPlugin.ICallback {
	//Here, LaborblattView is a handler implements ITextPlugin.ICallback, that must implement save() and saveAs(), so we're on a different level.
	//Also note that TextView.txt -> LaborblattView.text
	//ToDo: Homogenize levels and variable names.
	
	//log.log(Messages.getString("LaborblattView.save"), Log.DEBUGMSG); //$NON-NLS-1$
	
	//201306252207js save() was completely empty. Added content for similarity re StatusMonitor with TextView, LaborblattView, AUFZeugniss BestellBlatt, Laborblatt...
	public void save(){
		// TODO Automatisch erstellter Methoden-Stub
		if (actBrief != null) {
			System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.save(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
			System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.save(): about to save actBrief to DB...");
			System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.save(): ToDo: Homogenize abstraction/class/method levels and variable/method names between TextView.java and LaborblattView.java, AUFZeugnis, BestellBlatt etc.");
			//TODO: Why wouldn't we return the result here, but in SaveAs? js
            actBrief.save(text.getPlugin().storeToByteArray(), text.getPlugin().getMimeType());
        } else {
			System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.save(): actBrief == null, doing nothing.");
		}

		System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.save(): end\n");
	
	}
	
	public boolean saveAs(){
		// TODO Automatisch erstellter Methoden-Stub
		System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.saveAs(): TODO / TO REVIEW: Why would we return false in RezeptBlatt, BestellBlatt, LaborblattView, and true in AUFZeugnis.java???");
		System.out.println("js ch.elexis.views/LaborblattView.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");

		return false;
	}
}
