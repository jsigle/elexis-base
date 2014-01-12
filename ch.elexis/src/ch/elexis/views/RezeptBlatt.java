/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.views;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
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
import ch.elexis.text.TextContainer;
import ch.elexis.util.StatusMonitor;
import ch.rgw.tools.StringTool;

public class RezeptBlatt extends ViewPart implements ICallback, IActivationListener, IOutputter {
	public final static String ID = "ch.elexis.RezeptBlatt"; //$NON-NLS-1$
	TextContainer text;
	Brief actBrief;
	
	public RezeptBlatt(){
		
	}
	
	@Override
	public void dispose(){
		//201306161401js
		System.out.println("js ch.elexis.views/TextView.java dispose(): About to interrupt the statusMonitorThread...");			
		statusMonitorThread.interrupt();
		System.out.println("js ch.elexis.views/TextView.java dispose(): About to statusMonitorThread = null");			
		statusMonitorThread = null;
		
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: Bitte in TextView.java, RezeptBlatt.java, AU, etc. das alles noch spiegeln: StatusMonitor, dispose handler, etc.!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		
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
		text.getPlugin().dispose();		
		
		GlobalEventDispatcher.removeActivationListener(this, this);
		super.dispose();
	}
	
	/**
	 * load a Rezept from the database
	 * 
	 * @param brief
	 *            the Brief for the Rezept to be shown
	 */
	public void loadRezeptFromDatabase(Rezept rp, Brief brief){
		actBrief = brief;
		text.open(brief);
		rp.setBrief(actBrief);
	
		//201306161205js: Now also create a status monitor thread:
		if (statusMonitorThread == null) {
			System.out.println("js ch.elexis.views/TextView.java openDocument(Brief doc): about to start new statusMonitorThread()...\n");
			statusMonitorThread = new Thread(new StatusMonitor()); 
			statusMonitorThread.start();
		} else {
			System.out.println("js ch.elexis.views/TextView.java openDocument(Brief doc): WARNING: statusMonitorThread is already != null. This should NOT be the case now. Will not start new status monitor thread.");				
		}
	}
	
	@Override
	public void createPartControl(Composite parent){
		text = new TextContainer(getViewSite());
		text.getPlugin().createContainer(parent, this);
		GlobalEventDispatcher.addActivationListener(this, this);
	}
	
	@Override
	public void setFocus(){
		// TODO Automatisch erstellter Methoden-Stub
		
	}
	
	public boolean createList(Rezept rp, String template, String replace){
		actBrief =
			text.createFromTemplateName(Konsultation.getAktuelleKons(), template, Brief.RP,
				(Patient) ElexisEventDispatcher.getSelected(Patient.class),
				template + " " + rp.getDate());
		
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
		
		rp.setBrief(actBrief);

		//201306161205js: Now also create a status monitor thread:
		if (statusMonitorThread == null) {
			System.out.println("js ch.elexis.views/RezeptBlatt.java createList(): about to start new statusMonitorThread()...\n");
			statusMonitorThread = new Thread(new StatusMonitor()); 
			statusMonitorThread.start();
		} else {
			System.out.println("js ch.elexis.views/RezeptBlatt.java createList(): WARNING: statusMonitorThread is already != null. This should NOT be the case now. Will not start new status monitor thread.");				
		}

		
		if (text.getPlugin().insertTable(replace, 0, fields, wt)) {
			if (text.getPlugin().isDirectOutput()) {
				text.getPlugin().print(null, null, true);
				getSite().getPage().hideView(this);
			}
			text.saveBrief(actBrief, Brief.RP);
			return true;
		}
		text.saveBrief(actBrief, Brief.RP);
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
	
	public void save(){
		if (actBrief != null) {
			actBrief.save(text.getPlugin().storeToByteArray(), text.getPlugin().getMimeType());
		}
	}
	
	public boolean saveAs(){
		// TODO Automatisch erstellter Methoden-Stub
		return false;
	}
	
	/*
	 * 201306171151js
	 * A status monitoring method that I want to call regularly.
	 * It shall:
	 * (a) check whether the TextView view has focus, but is not enabled, and should thus be enabled.
	 * This needs to be done from outside, as merely accessing the needed structures below wb on a sufficiently
	 * low level will cause the ModicifacionListener already added to NOAText to stall the NOAText plugin.
	 * (b) Regularly call the storeToByteArray() method - which can access the doc.isModified(), and which I have
	 * modified to skip the actual storing when isModified() is false.
	 * It would be better if this storing was triggered a short time after the last keystroke,
	 * rather than in a regular interval. But at the moment, I can't see how isModified() could get the information
	 * up here, or how we could look down for that in another way (apart from solutions requiring major construction
	 * work, which I may attempt later on).   
	 */
	//I assume that ONE instance of TextView handles ONE document at time,
	//so it may contain ONE statusMonitorThread monitoring exactly this document.
	//Sadly, I need to define statusMonitorThread on this level - furhter down, like in NOAText or in TextContainer will probably NOT do.
	//Because the storeToByteArray() is called from here, and its result forwarded to Brief.save() from here... -
	//no module further down the road can see both the source and target of that operation,
	//so none can trigger an automatic save action.
	//TODO: This also means that I need to copy that functionality - if I want to have it there -
	//in (probably) RezepteBlatt.java, ... and others from ch.elexis.views.
	//For now: in TextView.java, RezepteBlatt.java
	private Thread statusMonitorThread = null;
	private int statusMonitorCounter  = 0;
	private int statusMonitorCallSaveAt = 60; 
	//It would be nice to monitor user input - and save preferrably, 
	//when a pause of e.g. 10 secs after the last input has occured,
	//or - if no pause of that size has occured - e.g. after a maximum delay of 5 min after the first unsaved input.
	//So that users can usually type without interruption/delay by automatic saving,
	//and still the maximum amount of unsaved work/time is limited.
	//That would effectively move a regularly timed auto save action closer to the last user input, when the user pauses. 
	//This optimal? behaviour is, however,
	//at least approached to a usable state by calling save after a fixed time and only really performing a save if isModified().
	//TODO: Warum heisst der TextContainer in RezeptBlatt.java text; und in TextView.java textContainer???
	public class StatusMonitor implements Runnable {
		
		public void run() {
	    	System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() begin");
			while (true) {
				System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - about to do RezeptBlatt status checking work...");
				System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - actBrief: " + actBrief.getBetreff());
				try {
					//At regular intervals, check that: if textContainer has keyboard focus, it also is enabled
					
					/*
					 * System.out.println("js ch.lexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - text.isFocusControl(): " + rp.isFocusControl());
					System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - text.isEnabled():      " + rp.isEnabled());
					if ((text.isFocusControl() ) && (!text.isEnabled())) {
						System.out.println("js ch.lexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - about to text.setEnabled(true)...");
						text.setEnabled(true);
					}
					 */

					//save at regular intervals
					statusMonitorCounter = statusMonitorCounter  + 1;
					System.out.println("js ch.elexis.views/RezeptBlatt.java statusMonitor() - Thread: " + Thread.currentThread().getName() + " - statusMonitorCounter: " + statusMonitorCounter);
					if (statusMonitorCounter >= statusMonitorCallSaveAt)
					{
						save();
						statusMonitorCounter  = 0;
					}
					Thread.sleep(1000);
				} catch (InterruptedException irEx) {
					//if the thread is interrupted, then return from it
					return;
				}

				
				/*
				// bean.getDocument().print(pprops);
				xPrintable.print(pprops);
				long timeout = System.currentTimeMillis();
				while ((myXPrintJobListener.getStatus() == null)
					|| (myXPrintJobListener.getStatus() == PrintableState.JOB_STARTED)) {
					Thread.sleep(100);
					long to = System.currentTimeMillis();
					if ((to - timeout) > 10000) {
						break;
					}
				*/	    
			}
		}
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
			System.out.println("js ch.elexis.views/RezeptBlatt.java activation(false) requested: save()...");
			save();
		} else {
			System.out.println("js ch.elexis.views/TextView.java activation(true) requested: loadSystTemplateAction.setEnabled(); saveTemplateAction.setEnabled()");
		}
		
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
