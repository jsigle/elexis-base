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

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.Brief;
import ch.elexis.data.Konsultation;
import ch.elexis.data.OutputLog;
import ch.elexis.data.Patient;
import ch.elexis.data.Prescription;
import ch.elexis.data.Rezept;
import ch.elexis.exchange.IOutputter;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.rgw.tools.StringTool;

import ch.elexis.util.Log;

import ch.elexis.util.IStatusMonitorCallback;	//201306170935js - ensure edits in text documents are noted by Elexis and ultimately stored

public class RezeptBlatt extends ViewPart implements ICallback, IActivationListener, IOutputter, IStatusMonitorCallback {
	public final static String ID = "ch.elexis.RezeptBlatt"; //$NON-NLS-1$
	TextContainer text;
	Brief actBrief;
	
	public RezeptBlatt(){		
	}
	
	@Override
	public void dispose(){
		System.out.println("\njs ch.elexis.views/RezeptBlatt.java dispose(): begin");

		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//201306161401js
		//20131026js: Das konnte == null sein zumindest im Rahmen der Versuche beobachtet, das TextView Fenster zu schliessen. -> Exception.
		if (actBrief != null) ch.elexis.util.StatusMonitor.removeMonitorEntry(actBrief);	//hopefully, this is a suitable variable here.
		
		//20130425js: Nach Einfügen der folgenden Zeile wird der NOText closeListener mit queryClosing() und notifyClosing() tatsächlich aufgerufen,
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
		
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): ToDo: SOLLTE hier ein plugin().dispose() rein - siehe Kommentare - oder würde das im Betrieb nur unerwünscht Exceptions werfen (gerade gesehen)?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		//20131027js: Die text.getPlugin().dispose(); wieder aktiviert,
		//andernfalls würde beim Schliessen der RezeptBlatt.java View weder soffice.bin per xDesktop.terminate entladen, noch soffice.exe per oooServer.xkill,
		//also vermutlich auch kein noas.remove; noas.isEmpty() -> bootStrapConnector.disconnect() erfolgen.
		//YEP, seit ich das wieder aktiviert habe, verschwinden das geladene soffice.bin und soffice.exe nach Schliessen der RezeptBlatt View,
		//jedenfalls bei nur einem offenen Elexis, und nur diesem offenen OO Dokument - so ist das auch gedacht. 
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): about to txt.getPlugin().dispose()");
		text.getPlugin().dispose();		
		
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): about to GlobalEventDispatcher.removeActivationListener()...");
		GlobalEventDispatcher.removeActivationListener(this, this);

		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): about super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//20131027js:
		//Was nun das super.dispose genau macht, kann (will) ich im Moment nicht nachvollziehen. in Texteview.java steht das nicht / stand das wohl nie.
		//Vielleicht soll es tatsächlich den View-Rahmen (Composite, xyz, Tab mit Titel "Rezept") entsorgen, nachdem das Rezept fertig gedruckt ist?
		//Wofür ich extra das closePreviouslyOpen... in RezepteView bzw. in Briefauswahl.java erfunden habe?
		//Ich hoffe einmal, dass das nicht stört.
		//TODO: 20131027js: review: Was macht in RezeptBlatt.dispose() das super.dispose()? Braucht's das auch in TextView.java? Cave: Modulnamen-Verwirrung beachten, siehe intro comment von js.
		super.dispose();
	}
	
	/**
	 * load a Rezept from the database
	 * 
	 * @param brief
	 *            the Brief for the Rezept to be shown
	 */
	public void loadRezeptFromDatabase(Rezept rp, Brief brief){
		System.out.println("\njs ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(Rezept rp, Brief brief): begin");
		
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about Wird hier auch ein Meaningful name verwendet?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about Warum ist loadRezeptFromDatabase() so kurz, aeber TextView openDocument() viel länger?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		
		
		actBrief = brief;		
		text.open(brief);
		rp.setBrief(actBrief);
		
		//201306161205js: Now also create a status monitor thread:
		//In TextView.java, SaveHandler was a separate class implementing ICallback with its save() and saveAs() methods.
		//Also, I added ShowViewHandler as another separate class implementing IStatusMonitorCallback with its showView method.
		//There, we used:
		//ch.elexis.util.StatusMonitor.addMonitorEntry(..., new SaveHandler(), new ShowViewHandler());
		//In RezeptBlatt.java, RezeptBlatt directly implements ICallback, and I added that it also directly implements IStatusMonitorCallback.
		//Especially, because supplying ShowViewHandler() to RezeptBlatt...addMonitoring would activate the TextView window (Briefe),
		//but not the RezetpBlatt window (Rezept). No, sorry - that was more probably because the NOAText based isModified() event handler set
		//the isModified() flag always for the TextView related statusMonitor entry, and not for the RezeptBlatt related entry.
		
		//So RezeptBlatt() has to replace both SaveHandler() and ShowViewHandler().
		//And as we do not want a *new* RezeptBlatt to be called, but the existing one instead, we might just as well supply (..., this, this).
		//ToDo: Please homogenize, if possible. Quite possibly, Textview might be changed to become similar to RezeptBlatt etc.
		text.getPlugin().setBriefServicedByThis(actBrief);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBrief, this, this);
		
		//What element could we record to tell NOAText which entry to update on addDocumentModifyListener() / isModified() events?
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): actBrief.getId() == " + actBrief.getId());
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): actBrief.getLabel() == " + actBrief.getLabel());
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): actBrief.getTyp() == " + actBrief.getTyp());
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): actBrief.getLastUpdate() == " + actBrief.getLastUpdate());
		
		//ch.elexis.data.Rezept@ce0114d8
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): actBrief.getXid() == " + actBrief.getXid());

		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): rp.toString() == " + rp.toString());
		
		//K5587ca8cc4c9a8f06146
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): rp.getId() == " + rp.getId());
		
		//02.05.2013 jh
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): rp.getLabel() == " + rp.getLabel());

		//ch.elexis.text.TextContainer@7acabf
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): text.toString() == " + text.toString());
		
		//8047295
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): text.hashCode() == " + text.hashCode());
		
		//class ch.elexis.text.TextContainer
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): text.getClass().toString() == " + text.getClass().toString());

		System.out.println("\njs ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(Rezept rp, Brief brief): end");
	}
	
	@Override
	public void createPartControl(Composite parent){
		text = new TextContainer(getViewSite());
		text.getPlugin().createContainer(parent, this);
		//TODO: 20131027js: In TextView gibt es hier eine Fehlermeldung, falls getPlugin failt. Ausserdem andere Bezeichnungen. Bitte homogenisieren.
		GlobalEventDispatcher.addActivationListener(this, this);
	}
	
	@Override
	public void setFocus(){
		// TODO Automatisch erstellter Methoden-Stub
		//TODO: 20131027js: In TextView gibt es hier mehr. Bitte review und homogenisieren.
		
	}
	
	public boolean createList(Rezept rp, String template, String replace){
		System.out.println("\njs ch.elexis.views/RezeptBlatt.java createList(Rezept rp, String template, String replace): begin");

		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/RezeptBlatt.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		

		actBrief =
			text.createFromTemplateName(Konsultation.getAktuelleKons(), template, Brief.RP,
				(Patient) ElexisEventDispatcher.getSelected(Patient.class),
				template + " " + rp.getDate());
		
		//201306230924js: Added this similar to what I found in Textview.
		if (actBrief == null) {
			System.out.println("js ch.elexis.views/RezeptBlatt.java createDocument(3): WARNING: returning false\n");
			return false;
		}

		List<Prescription> lines = rp.getLines();
		String[][] fields = new String[lines.size()][];
		int[] wt = new int[] {
			10, 70, 20
		};
		for (int i = 0; i < fields.length; i++) {
			Prescription p = lines.get(i);
			fields[i] = new String[3];
			fields[i][0] = p.get(Messages.getString("RezeptBlatt.number")); //$NON-NLS-1$
			String bem = p.getBemerkung();
			if (StringTool.isNothing(bem)) {
				fields[i][1] = p.getSimpleLabel();
			} else {
				fields[i][1] = p.getSimpleLabel() + "\n" + bem; //$NON-NLS-1$
			}
			fields[i][2] = p.getDosis();
			
		}
		
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): Schauen, wie das mit TextView.createDocument korrespondiert. Dort: setName(); hier unten: setBrief(),->setLetterID oder ähnlich...?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): Wird ein nützlicher Fenstertitel gesetzt?");
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		
		rp.setBrief(actBrief);

		//201306161205js: Now also add a statusMonitor entry:
		text.getPlugin().setBriefServicedByThis(actBrief);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBrief, this, this);		

		if (text.getPlugin().insertTable(replace, 0, fields, wt)) {
			if (text.getPlugin().isDirectOutput()) {
				System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): text.getPlugin().isDirectOutput == true -> about to print(...); hideView(...)");		
				text.getPlugin().print(null, null, true);
				getSite().getPage().hideView(this);
			}
			
			System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about to text.save(actBrief, Brief.RP...");		
			text.saveBrief(actBrief, Brief.RP);
			System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about to return true");		
			return true;
		}
		
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about to text.save(actBrief, Brief.RP...");		
		text.saveBrief(actBrief, Brief.RP);
		System.out.println("js ch.elexis.views/RezeptBlatt.java loadRezeptFromDatabase(): about to return false");		
		return false;
	}
	
	public boolean createRezept(Rezept rp){
		if (createList(
			rp,
			Messages.getString("RezeptBlatt.TemplateNamePrescription"), Messages.getString("RezeptBlatt.4"))) { //$NON-NLS-1$ //$NON-NLS-2$
			new OutputLog(rp, this);
			return true;
		}
		return false;
	}
	
	public boolean createEinnahmeliste(Patient pat, Prescription[] pres){
		Rezept rp = new Rezept(pat);
		for (Prescription p : pres) {
			/*
			 * rp.addLine(new RpZeile(" ",p.getArtikel().getLabel(),"",
			 * p.getDosis(),p.getBemerkung()));
			 */
			rp.addPrescription(new Prescription(p));
		}
		return createList(rp,
			Messages.getString("RezeptBlatt.TemplateNameList"), Messages.getString("RezeptBlatt.6")); //$NON-NLS-1$ //$NON-NLS-2$
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
	//In RezeptBlatt.java, RezeptBlatt directly implements ICallback, and I added that it also directly implements IStatusMonitorCallback.
	//Especially, because supplying ShowViewHandler() to RezeptBlatt...addMonitoring would activate the TextView window (Briefe),
	//but not the RezetpBlatt window (Rezept). No, sorry - that was more probably because the NOAText based isModified() event handler set
	//the isModified() flag always for the TextView related statusMonitor entry, and not for the RezeptBlatt related entry.
	
	//So RezeptBlatt() has to replace both SaveHandler() and ShowViewHandler().
	//And as we do not want a *new* RezeptBlatt to be called, but the existing one instead, we might just as well supply (..., this, this).
	//ToDo: Please homogenize, if possible. Quite possibly, Textview might be changed to become similar to RezeptBlatt etc.

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
		    	//System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isFocusControl(): " + textContainer.isFocusControl());
				//System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isEnabled():      " + textContainer.isEnabled());
					
				//DAS BITTE NUR, WENN isModified() akut unten gesetzt wurde!
				//Sonst kann man Elexis ausserhalb des TextPluginWindows nur noch sehr schlecht steuern.
		
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - PLEASE IMPLEMENT Activation of the correct office window!");								
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - Done: TextView.java");								
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - Done: RezeptBlatt.java, AUFZeugnis, BestellBlatt.java, ");								
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - ToDo: ... ");								
				System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
		    	
				//YEP. DAS macht die View aktiv, incl. hervorgehobenem Rahmen, und Focus, in dem der Text drinnen steckt.
				//Im Moment leider noch alle Zeit, also auch dann, wenn gerade NICHT isModified() durch neue Eingaben immer wieder gesetzt würde.
				//TextView.ID liefert: ch.elexis.TextView
				RezeptBlatt rb = null;
				try {
					System.out.println("js com.jsigle.noa/RezeptBlatt.java - run() - Thread: " + Thread.currentThread().getName() + " - about to rb.showView(RezeptBlatt.ID) with RezeptBlatt.ID == " + RezeptBlatt.ID);
					rb = (RezeptBlatt) getViewSite().getPage().showView(RezeptBlatt.ID /*,StringTool.unique("textView"),IWorkbenchPage.VIEW_ACTIVATE*/);
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
	//Here, RezeptBlatt is a handler implements ITextPlugin.ICallback, that must implement save() and saveAs(), so we're on a different level.
	//Also note that TextView.txt -> RezeptBlatt.text
	//ToDo: Homogenize levels and variable names.
	
	//log.log(Messages.getString("RezeptBlatt.save"), Log.DEBUGMSG); //$NON-NLS-1$
	
	public void save(){
		if (actBrief != null) {
			System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.save(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
			System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.save(): about to save actBrief to DB...");
			System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.save(): ToDo: Homogenize abstraction/class/method levels and variable/method names between TextView.java and RezeptBlatt.java, AUFZeugnis, BestellBlatt etc.");
			//TODO: Why wouldn't we return the result here, but in SaveAs? js
            actBrief.save(text.getPlugin().storeToByteArray(), text.getPlugin().getMimeType());
        } else {
			System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.save(): actBrief == null, doing nothing.");
		}

		System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.save(): end\n");
	}
	
	public boolean saveAs(){
		// TODO Automatisch erstellter Methoden-Stub
		System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: Why would we return true false in RezeptBlatt, BestellBlatt,, and true in AUFZeugnis.java???");
		System.out.println("js ch.elexis.views/RezeptBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		
		return false;
	}
	

	/*
	 * 201306161348js: Attempt to add missing doc:
	 * If view RezeptBlatt loses "activation" state (related to, but sadly not equal to "focus"),
	 * it shall save it's contents back to the database.
	 * This is NOT sufficient, however, that's why I added StatusMonitor.
	 * See TextView.java for my discussion of limitations of this concept.
	 * @see ch.elexis.actions.GlobalEventDispatcher.IActivationListener#activation(boolean)
	 */
	public void activation(boolean mode){
		System.out.println("\njs ch.elexis.views/RezeptBlatt.java activation(mode="+mode+"): begin");
		if (mode == false) {
			System.out.println("js ch.elexis.views/RezeptBlatt.java activation(false) about to (simply) save()...");
			System.out.println("js ch.elexis.views/RezeptBlatt.java ToDo: TextView directly calls actBrief.save() here; like save() there and above. Please review and homogenize.");
			save();
		} else {
			System.out.println("js ch.elexis.views/RezeptBlatt.java activation(true) requested.");
		}
		
		System.out.println("\njs ch.elexis.views/RezeptBlatt.java activation(): end\n");
	}
	
	public void visible(boolean mode){
		
	}
	
	public String getOutputterDescription(){
		return "Druckerausgabe erstellt";
	}
	
	public String getOutputterID(){
		return "ch.elexis.RezeptBlatt";
	}
	
	public Image getSymbol(){
		return Desk.getImage(Desk.IMG_PRINTER);
	}
}
