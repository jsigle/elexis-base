/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg Sigle www.jsigle.com
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

package ch.elexis.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.AUF;
import ch.elexis.data.Brief;
import ch.elexis.data.Konsultation;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.text.ITextPlugin.PageFormat;

import ch.elexis.util.IStatusMonitorCallback;	//201306170935js - ensure edits in text documents are noted by Elexis and ultimately stored

public class AUFZeugnis extends ViewPart implements ICallback, IActivationListener, IStatusMonitorCallback {
	public static final String ID = "ch.elexis.AUFView"; //$NON-NLS-1$
	TextContainer text;
	Brief actBrief;
	
	public AUFZeugnis(){}
	
	@Override
	public void dispose(){
		System.out.println("\njs ch.elexis.views/AUFZeugnis.java dispose(): begin");

		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

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
		
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): ToDo: SOLLTE hier ein plugin().dispose() rein - siehe Kommentare - oder würde das im Betrieb nur unerwünscht Exceptions werfen (gerade gesehen)?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): about to txt.getPlugin().dispose()");
		//text.getPlugin().dispose();		
		
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): about to GlobalEventDispatcher.removeActivationListener()...");
		GlobalEventDispatcher.removeActivationListener(this, this);

		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): about super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		super.dispose();
	}
	
	@Override
	public void createPartControl(Composite parent){
		setTitleImage(Desk.getImage(Desk.IMG_PRINTER));
		text = new TextContainer(getViewSite());
		text.getPlugin().createContainer(parent, this);
		GlobalEventDispatcher.addActivationListener(this, this);
	}
	
	@Override
	public void setFocus(){
		text.setFocus();
	}
	
	public void createAUZ(final AUF auf){
		System.out.println("\njs ch.elexis.views/AUFZeugnis.java createAUZ(final AUF auf): begin");

		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/AUFZeugnis.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/AUFZeugnis.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/AUFZeugnis.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		

		actBrief =
			text.createFromTemplateName(Konsultation.getAktuelleKons(), "AUF-Zeugnis", Brief.AUZ, //$NON-NLS-1$
				null, null);
		
		//201306230924js: Added this similar to what I found in Textview.
		if (actBrief == null) {
			System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(3): WARNING: returning false\n");
			return;
		}

		//201306161205js: Now also add a statusMonitor entry:
		text.getPlugin().setBriefServicedByThis(actBrief);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBrief, this, this);		

		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): Schauen, wie das mit TextView.createDocument korrespondiert. Dort: setName(); hier unten: setBrief(),->setLetterID oder ähnlich...?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): Wird ein nützlicher Fenstertitel gesetzt?");
		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): Schauen, wie nachfolgendes isDirectOutput() sich auswirken soll / es tut / mit StatusMonitor zusammengeht.");
		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
		
		// text.getPlugin().setFormat(PageFormat.A5);
		if (text.getPlugin().isDirectOutput()) {
			System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): text.getPlugin().isDirectOutput == true -> about to print(...); hideView(...)");		
			text.getPlugin().print(null, null, true);
			getSite().getPage().hideView(this);
		}

		System.out.println("js ch.elexis.views/AUFZeugnis.java createAUZ(): end");		
	}
	
	public TextContainer getTextContainer(){
		return text;
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
	//In AUFZeugnis.java, AUFZeugnis directly implements ICallback, and I added that it also directly implements IStatusMonitorCallback.
	//Especially, because supplying ShowViewHandler() to AUFZeugnis...addMonitoring would activate the TextView window (Briefe),
	//but not the RezetpBlatt window (Rezept). No, sorry - that was more probably because the NOAText based isModified() event handler set
	//the isModified() flag always for the TextView related statusMonitor entry, and not for the AUFZeugnis related entry.
	
	//So AUFZeugnis() has to replace both SaveHandler() and ShowViewHandler().
	//And as we do not want a *new* AUFZeugnis to be called, but the existing one instead, we might just as well supply (..., this, this).
	//ToDo: Please homogenize, if possible. Quite possibly, Textview might be changed to become similar to AUFZeugnis etc.

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
		    	//System.out.println("js ch.elexis.views/AUFZeugnis.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isFocusControl(): " + textContainer.isFocusControl());
				//System.out.println("js ch.elexis.views/AUFZeugnis.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isEnabled():      " + textContainer.isEnabled());
					
				//DAS BITTE NUR, WENN isModified() akut unten gesetzt wurde!
				//Sonst kann man Elexis ausserhalb des TextPluginWindows nur noch sehr schlecht steuern.
		
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - PLEASE IMPLEMENT Activation of the correct office window!");								
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - Done: TextView.java");								
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - Done: RezeptBlatt.java, AUFZeugnis, BestellBlatt.java, ");								
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - ToDo: ... ");								
				System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
		    	
				//YEP. DAS macht die View aktiv, incl. hervorgehobenem Rahmen, und Focus, in dem der Text drinnen steckt.
				//Im Moment leider noch alle Zeit, also auch dann, wenn gerade NICHT isModified() durch neue Eingaben immer wieder gesetzt würde.
				//TextView.ID liefert: ch.elexis.TextView
				AUFZeugnis au = null;
				try {
					System.out.println("js com.jsigle.noa/AUFZeugnis.java - run() - Thread: " + Thread.currentThread().getName() + " - about to rb.showView(AUFZeugnis.ID) with AUFZeugnis.ID == " + AUFZeugnis.ID);
					au = (AUFZeugnis) getViewSite().getPage().showView(AUFZeugnis.ID /*,StringTool.unique("textView"),IWorkbenchPage.VIEW_ACTIVATE*/);
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
	//Here, AUFZeugnis is a handler implements ITextPlugin.ICallback, that must implement save() and saveAs(), so we're on a different level.
	//Also note that TextView.txt -> AUFZeugnis.text
	//ToDo: Homogenize levels and variable names.
	
	//log.log(Messages.getString("AUFZeugnis.save"), Log.DEBUGMSG); //$NON-NLS-1$
	
	
	public void save(){
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.save(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.save(): about to save actBrief to DB...");
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.save(): ToDo: Homogenize abstraction/class/method levels and variable/method names between TextView.java and RezeptBlatt.java, AUFZeugnis, BestellBlatt etc.");
		//TODO: Why wouldn't we return the result here, but in SaveAs? js
		if (actBrief != null) {
			actBrief.save(text.getPlugin().storeToByteArray(), text.getPlugin().getMimeType());
		} else {
			System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.save(): actBrief == null, doing nothing.");
		}

		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.save(): end\n");

	}
	
	public boolean saveAs(){
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.saveAs(): TODO / TO REVIEW: Why would we return false in RezeptBlatt, BestellBlatt, LaborblattView, and true in AUFZeugnis.java???");
		System.out.println("js ch.elexis.views/AUFZeugnis.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		
		return true;
	}
	
	/*
	 * 201306161348js: Attempt to add missing doc:
	 * If view AUFZeugnis loses "activation" state (related to, but sadly not equal to "focus"),
	 * it shall save it's contents back to the database.
	 * This is NOT sufficient, however, that's why I added StatusMonitor.
	 * See TextView.java for my discussion of limitations of this concept.
	 * @see ch.elexis.actions.GlobalEventDispatcher.IActivationListener#activation(boolean)
	 */
	public void activation(boolean mode){
		System.out.println("\njs ch.elexis.views/AUFZeugnis.java activation(mode="+mode+"): begin");
		if (mode == false) {
			System.out.println("js ch.elexis.views/AUFZeugnis.java activation(false) about to (simply) save()...");
			System.out.println("js ch.elexis.views/AUFZeugnis.java ToDo: TextView directly calls actBrief.save() here; like save() there and above. Please review and homogenize.");
			save();
		} else {
			System.out.println("js ch.elexis.views/AUFZeugnis.java activation(true) requested.");
		}
		
		System.out.println("\njs ch.elexis.views/AUFZeugnis.java activation(): end\n");
	}
	
	public void visible(boolean mode){}
	
}
