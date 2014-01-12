/*******************************************************************************
 * Copyright (c) 2006-2009, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.data.Brief;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Bestellung.Item;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Money;
import ch.rgw.tools.StringTool;

import ch.elexis.util.IStatusMonitorCallback;	//201306170935js - ensure edits in text documents are noted by Elexis and ultimately stored

public class BestellBlatt extends ViewPart implements ICallback, IStatusMonitorCallback {
	public final static String ID = "ch.elexis.BestellBlatt"; //$NON-NLS-1$
	TextContainer text;
	Brief actBest;
	
	private final static String TEMPLATENAME = Messages.getString("BestellBlatt.TemplateName"); //$NON-NLS-1$
	private static final String ERRMSG_CAPTION = Messages
		.getString("BestellBlatt.CouldNotCreateOrder"); //$NON-NLS-1$
	private static final String ERRMSG_BODY = Messages
		.getString("BestellBlatt.CouldNotCreateOrderBody"); //$NON-NLS-1$
	
	@Override
	public void createPartControl(final Composite parent){
		setTitleImage(Desk.getImage(Desk.IMG_PRINTER));
		text = new TextContainer(getViewSite());
		text.getPlugin().createContainer(parent, this);
	}
	
	//201306251814js: BestellBlatt hatte kein Dispose -
	//ist das nötig oder sinnvoll - zur Verwaltung der noa-Module z.B.? für removeMonitorEntry() sicherlich.
	//Ich hab's mal von RezeptBlatt auch hier herein übernommen.
	@Override
	public void dispose(){
		System.out.println("\njs ch.elexis.views/BestellBlatt.java dispose(): begin");

		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//201306161401js
		ch.elexis.util.StatusMonitor.removeMonitorEntry(actBest);	//hopefully, this is a suitable variable here.
		
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
		
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): ToDo: SOLLTE hier ein plugin().dispose() rein - siehe Kommentare - oder würde das im Betrieb nur unerwünscht Exceptions werfen (gerade gesehen)?");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//20131027js: Die text.getPlugin().dispose(); wieder aktiviert,
		//andernfalls würde beim Schliessen der RezeptBlatt.java View weder soffice.bin per xDesktop.terminate entladen, noch soffice.exe per oooServer.xkill,
		//also vermutlich auch kein noas.remove; noas.isEmpty() -> bootStrapConnector.disconnect() erfolgen.
		//YEP, seit ich das wieder aktiviert habe, verschwinden das geladene soffice.bin und soffice.exe nach Schliessen der RezeptBlatt View,
		//jedenfalls bei nur einem offenen Elexis, und nur diesem offenen OO Dokument - so ist das auch gedacht. 
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): about to txt.getPlugin().dispose()");
		text.getPlugin().dispose();		
		
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): ToDo: WARNING / PLEASE REVIEW: The method GlobalEventDispatcher.removeActivationListener(BestellBlatt, BestellBlatt) is not applicable.");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): ToDo: Das BestellBlatt hat auch 1 Jahr älteres Copyright - wird das dort nicht benötigt, oder wurde es nur noch nicht nachgetragen?");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//TODO: Bitte bei BestellBlatt.dispose() nachschauen, ob GlobalEventDispatcher.removeActivationListener(this,this) aktivierbar ist - siehe dort TODO notes in System.out.printl(), und vergleiche mit Rezepte.View!
		//System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): about to GlobalEventDispatcher.removeActivationListener()...");
		//GlobalEventDispatcher.removeActivationListener(this, this);

		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): about super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.views/BestellBlatt.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		super.dispose();
	}

	public void createOrder(final Kontakt adressat, final List<Item> items){
		System.out.println("\njs ch.elexis.views/BestellBlatt.java createOrder(final Kontakt adressat, final List<Item> items): begin");

		String[][] tbl = new String[items.size() + 2][];
		int i = 1;
		Money sum = new Money();
		tbl[0] =
			new String[] {
				Messages.getString("BestellBlatt.Number"), Messages.getString("BestellBlatt.Pharmacode"), Messages.getString("BestellBlatt.Name"), Messages.getString("BestellBlatt.UnitPrice"), Messages.getString("BestellBlatt.LinePrice") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			};
		// DecimalFormat df=new DecimalFormat("\u00a4\u00a4  #.00");
		for (Item it : items) {
			String[] row = new String[5];
			row[0] = Integer.toString(it.num);
			row[1] = it.art.getPharmaCode();
			row[2] = it.art.getName();
			row[3] = it.art.getEKPreis().getAmountAsString(); // Integer.toString(it.art.getEKPreis());
			// int amount=it.num*it.art.getEKPreis();
			Money amount = it.art.getEKPreis().multiply(it.num);
			row[4] = amount.getAmountAsString();
			sum.addMoney(amount);
			tbl[i++] = row;
		}
		tbl[i] =
			new String[] {
				Messages.getString("BestellBlatt.Sum"), StringTool.leer, StringTool.leer, StringTool.leer, sum.getAmountAsString() //$NON-NLS-1$
			};
		
		//201306250439js: Den ggf. vorhandenen Eintrag für ein Dokument in TextView, das gleich ersetzt wird, aus der StatusMonitoring Liste entfernen,
		//und - falls das nachfolgende Laden schief geht - auch aus der zugehörigen Instanz von NOAText.briefServicedByThis entfernen.
		//Falls noch gar kein Dokument geladen/verzeichnet war, sollte auch null richtig gehandelt werden.
		//Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird.
		System.out.println("js ch.elexis.views/BestellBlatt.java TODO / TO REVIEW: ********************************************************************************");
		System.out.println("js ch.elexis.views/BestellBlatt.java TODO / TO REVIEW: Normalerweise sollte das Entladen ja mit dispose() erfolgen - tut es aber nicht, weil dispose() beim Doppelklick auf ein anderes Dokument anscheinend nicht aufgerufen wird. Vielleicht müsste ich es auch an clean() o.ä. in NOAText ankoppeln...?");
		System.out.println("js ch.elexis.views/BestellBlatt.java TODO / TO REVIEW: Review auch weitere Auftretens von addMonitor...() - sicherstellen, dass vor denen auch erst bestehende Einträge gelöscht werden.");
		System.out.println("js ch.elexis.views/BestellBlatt.java TODO / TO REVIEW: ********************************************************************************");		
		Brief vorigerBrief = text.getPlugin().getBriefServicedByThis();
		text.getPlugin().setBriefServicedByThis(null);
		ch.elexis.util.StatusMonitor.removeMonitorEntry(vorigerBrief);		
	
		actBest = text.createFromTemplateName(null, TEMPLATENAME, Brief.BESTELLUNG, adressat, null);
		if (actBest == null) {
			SWTHelper.showError(ERRMSG_CAPTION, ERRMSG_BODY + "'" + TEMPLATENAME + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			actBest.setPatient(Hub.actUser);
			text.getPlugin().insertTable("[" + TEMPLATENAME + "]", //$NON-NLS-1$ //$NON-NLS-2$
				ITextPlugin.FIRST_ROW_IS_HEADER | ITextPlugin.GRID_VISIBLE, tbl, null);
			if (text.getPlugin().isDirectOutput()) {
				text.getPlugin().print(null, null, true);
				getSite().getPage().hideView(this);
			}
		}
		
		//201306161205js: Now also add a statusMonitor entry:
		text.getPlugin().setBriefServicedByThis(actBest);
		ch.elexis.util.StatusMonitor.addMonitorEntry(actBest, this, this);		

		System.out.println("js ch.elexis.views/BestellBlatt.java createOrder(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/BestellBlatt.java createOrder(): Schauen, wie das mit TextView.createDocument korrespondiert. Dort: setName(); hier unten: setBrief(),->setLetterID oder ähnlich...?");
		System.out.println("js ch.elexis.views/BestellBlatt.java createOrder(): Wird ein nützlicher Fenstertitel gesetzt?");
		System.out.println("js ch.elexis.views/BestellBlatt.java createOrder(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
	
		System.out.println("\njs ch.elexis.views/BestellBlatt.java createOrder(final Kontakt adressat, final List<Item> items): end");		
	}
	
	@Override
	public void setFocus(){
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	//201306170655js: I implement this additional callback method to give StatusMonitor a possibility to trigger a ShowView from TextView.java
	//Required, as getView() is not easily accessible from StatusMonitor (and not in a number of more complicated ways I tried either),
	//and moreover, we can keep the TextView.ID etc. up here, and do similar but yet specific things for all other corresponding text processing windows -
	//just letting each of them specify whatever they want to have called from StatusMonitor.java
	//class ShowViewHandler implements IStatusMonitorCallback {

	//In TextView.java, SaveHandler was a separate class implementing ICallback with its save() and saveAs() methods.
	//Also, I added ShowViewHandler as another separate class implementing IStatusMonitorCallback with its showView method.
	//There, we used:
	//ch.elexis.util.StatusMonitor.addMonitorEntry("BestellBlatt", new SaveHandler(), new ShowViewHandler());
	//In BestellBlatt.java, BestellBlatt directly implements ICallback, and I added that it also directly implements IStatusMonitorCallback.
	//Especially, because supplying ShowViewHandler() to BestellBlatt...addMonitoring would activate the TextView window (Briefe),
	//but not the RezetpBlatt window (Rezept). No, sorry - that was more probably because the NOAText based isModified() event handler set
	//the isModified() flag always for the TextView related statusMonitor entry, and not for the BestellBlatt related entry.
	
	//So BestellBlatt() has to replace both SaveHandler() and ShowViewHandler().
	//And as we do not want a *new* BestellBlatt to be called, but the existing one instead, we might just as well supply (..., this, this).
	//ToDo: Please homogenize, if possible. Quite possibly, Textview might be changed to become similar to BestellBlatt etc.

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
		    	//System.out.println("js ch.elexis.views/BestellBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isFocusControl(): " + textContainer.isFocusControl());
				//System.out.println("js ch.elexis.views/BestellBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - textContainer.isEnabled():      " + textContainer.isEnabled());
					
				//DAS BITTE NUR, WENN isModified() akut unten gesetzt wurde!
				//Sonst kann man Elexis ausserhalb des TextPluginWindows nur noch sehr schlecht steuern.
		
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - PLEASE IMPLEMENT Activation of the correct office window!");								
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - Done: TextView.java");								
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - Done: BestellBlatt.java, AUFZeugnis, BestellBlatt.java, ");								
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - ToDo: ... ");								
				System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
		    	
				//YEP. DAS macht die View aktiv, incl. hervorgehobenem Rahmen, und Focus, in dem der Text drinnen steckt.
				//Im Moment leider noch alle Zeit, also auch dann, wenn gerade NICHT isModified() durch neue Eingaben immer wieder gesetzt würde.
				//TextView.ID liefert: ch.elexis.TextView
				BestellBlatt bb = null;
				try {
					System.out.println("js com.jsigle.noa/BestellBlatt.java - run() - Thread: " + Thread.currentThread().getName() + " - about to rb.showView(BestellBlatt.ID) with BestellBlatt.ID == " + BestellBlatt.ID);
					bb = (BestellBlatt) getViewSite().getPage().showView(BestellBlatt.ID /*,StringTool.unique("textView"),IWorkbenchPage.VIEW_ACTIVATE*/);
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
	//Here, BestellBlatt is a handler implements ITextPlugin.ICallback, that must implement save() and saveAs(), so we're on a different level.
	//Also note that TextView.txt -> BestellBlatt.text
	//ToDo: Homogenize levels and variable names.
	
	//log.log(Messages.getString("BestellBlatt.save"), Log.DEBUGMSG); //$NON-NLS-1$
	public void save(){
		if (actBest != null) {
			System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.save(): actBest == "+actBest.toString()+": "+actBest.getBetreff());
			System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.save(): about to save actBest to DB...");
			System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.save(): ToDo: Homogenize abstraction/class/method levels and variable/method names between TextView.java and BestellBlatt.java, AUFZeugnis, BestellBlatt etc.");
			//TODO: Why wouldn't we return the result here, but in SaveAs? js
			actBest.save(text.getPlugin().storeToByteArray(), text.getPlugin().getMimeType());
        } else {
			System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.save(): actBest == null, doing nothing.");
		}

		System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.save(): end\n");
	}
	
	public boolean saveAs(){
		// TODO Automatisch erstellter Methoden-Stub
		System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");
		System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: Why would we return false in BestellBlatt, BestellBlatt, LaborblattView, and true in AUFZeugnis.java???");
		System.out.println("js ch.elexis.views/BestellBlatt.java SaveHandler.saveAs(): TODO / TO REVIEW: **********************************************************************************");

		return false;
	}
}
