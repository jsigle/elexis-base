/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     J. Sigle   - 20130728js: added a close method to ensure that Connection and Office server inside the TextPlugin,
 *                  namely inside NOAText_jsl, are closed again after use of this dialog, if appropriate.
 *                  Similarly to TextView.java/Briefauswahl.java, etc. (all other users of TextContainer).
 *                  
 *                  --
 *                  
 *                  Howto test:
 *                  
 *                  Open Elexis
 *                  Perspektive Reminder(Agenda).
 *                  Einen Patienten auswählen
 *                  Open Windows TaskManager; ensure that no soffice.bin/soffice.exe is running yet (no Office Client Window should be open)
 *                  In View Agenda xy oder Parallelanzeige, Wochenanzeige: ViewMenu: Patienten-Termine drucken
 *                  The TermineDruckenDialog window should open, containing an OpenOffice client; and soffice.bin/soffice.exe should appear in the Task manager
 *                  Close the TermineDruckenDialog dialog window
 *                  soffice.bin/soffice.exe should disappear from the Task manager
 *                  
 *                  Testing Variations:
 *                  
 *                  Try it with multiple different Office documents open: LAST document closed should terminate the soffice.bin/soffice.exe.
 *                  Try it with multiple instances of Elexis.
 *                  Try it with multiple instances of Elexis and multiple different Office documents open in each of them.
 *                  Try it with external OpenOffice clients open; e.g. Schnellstarter or a simle Writer document.
 *                  
 *                  N.B.: If you tried this first in Elexis with another print... menu entry in Elexis,
 *                  that would NOT cause correct decrementing of noas after closing the document,
 *                  noas might be stuck at > 0 even if you close the last document from a dialog that DOES implement correct decrementing of noas.
 *                  So even if you killed the soffice.bin/soffice.exe after a failed test of a wrongly working print command,
 *                  you would not get modules that decrement noas properly after closing the doc to make OO unload/disconnect.
 *                  This is just a measurement problem, applies to all other similar modules, I just noticed right now.
 *                  
 *                   //TODO: by the way: Check whether the text.getPlugin().dispose() call could be put (back?) into NOAText, TextContainer, or so.
 *                   //TODO: After all it's quite a hazzle to upgrade each individual TextContainer using module with one dispose() or close() method. 
 *                  
 *                  As of 20131028js: Please note: Problem may occur IF externally, a simple Writer document is open, changed and never saved.
 *
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.Hub;
import ch.elexis.agenda.data.Termin;
import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.agenda.util.Plannables;
import ch.elexis.data.Brief;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

public class TermineDruckenDialog extends TitleAreaDialog implements ICallback {
	Termin[] liste;
	
	private TextContainer text = null;
	
	public TermineDruckenDialog(Shell shell, Termin[] liste){
		super(shell);
		this.liste = liste;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new FillLayout());
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		
		String template =
			Hub.localCfg.get(PreferenceConstants.AG_PRINT_APPOINTMENTCARD_TEMPLATE,
				PreferenceConstants.AG_PRINT_APPOINTMENTCARD_TEMPLATE_DEFAULT);
		
		text = new TextContainer(getShell());
		text.getPlugin().createContainer(ret, this);
		text.getPlugin().showMenu(true);
		text.getPlugin().showToolbar(true);
		text.createFromTemplateName(null, template, Brief.UNKNOWN, Hub.actUser, "Agenda");
		/*
		 * String[][] termine=new String[liste.length+1][3]; termine[0]=new String[]{"Datum",
		 * "Zeit","Bei"}; for(int i=0;i<liste.length;i++){ TimeTool day=new
		 * TimeTool(liste[i].getDay()); termine[i+1][0]=day.toString(TimeTool.DATE_GER);
		 * termine[i+1][1]=Plannables.getStartTimeAsString(liste[i]);
		 * termine[i+1][2]=liste[i].getBereich(); } text.getPlugin().setFont("Helvetica",
		 * SWT.NORMAL, 9); text.getPlugin().insertTable("[Termine]",
		 * ITextPlugin.FIRST_ROW_IS_HEADER, termine, new int[]{20,20,60});
		 */
		StringBuilder sb = new StringBuilder();
		for (Termin t : liste) {
			TimeTool day = new TimeTool(t.getDay());
			sb.append(day.toString(TimeTool.WEEKDAY)).append(", ")
				.append(day.toString(TimeTool.DATE_GER)).append(" - ")
				.append(Plannables.getStartTimeAsString(t)).append("\n");
		}
		text.replace("\\[Termine\\]", sb.toString());
		if (text.getPlugin().isDirectOutput()) {
			text.getPlugin().print(null, null, true);
			okPressed();
		}
		return ret;
	}
	
	//20131028js: Added this method completely to GenericPrintDialog, it does parts of what dispose() does in RezeptBlatt.java etc.
	//A similar edit has been made to EtiketteDruckenDialog.java
	//This works as expected for Kontakte: Adressliste.
	@Override
	public boolean close(){
		//Call the original overwritten close method?
		boolean ret = super.close();
		
		System.out.println("\njs ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): begin");

		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: Added a close function with at least the text.getPlugin().dispose(); functionality");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: as also added to other TextContainer clients, to ensure that TextPlugin connection/server");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: is closed again after usage. Please review other commented out content of this dispose():");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: Does GenericPrintDialog need more functionality as well?");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		/*
		 * Other content usually found in other TextContainer using clients: commented out, but please review if we should need anything. 
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

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
		*/
				
		//20131027js: Die text.getPlugin().dispose(); wieder aktiviert,
		//andernfalls würde beim Schliessen der RezeptBlatt.java View weder soffice.bin per xDesktop.terminate entladen, noch soffice.exe per oooServer.xkill,
		//also vermutlich auch kein noas.remove; noas.isEmpty() -> bootStrapConnector.disconnect() erfolgen.
		//YEP, seit ich das wieder aktiviert habe, verschwinden das geladene soffice.bin und soffice.exe nach Schliessen der RezeptBlatt View,
		//jedenfalls bei nur einem offenen Elexis, und nur diesem offenen OO Dokument - so ist das auch gedacht. 
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): about to txt.getPlugin().dispose()");
		text.getPlugin().dispose();		
		
		/*
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): about to GlobalEventDispatcher.removeActivationListener()...");
		GlobalEventDispatcher.removeActivationListener(this, this);
		*/

		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): about to super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.agenda/ch.elexis.dialogs/TermineDruckenDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//20131027js:
		//Was nun das super.dispose genau macht, kann (will) ich im Moment nicht nachvollziehen. in Texteview.java steht das nicht / stand das wohl nie.
		//Vielleicht soll es tatsächlich den View-Rahmen (Composite, xyz, Tab mit Titel "Rezept") entsorgen, nachdem das Rezept fertig gedruckt ist?
		//Wofür ich extra das closePreviouslyOpen... in RezepteView bzw. in Briefauswahl.java erfunden habe?
		//Ich hoffe einmal, dass das nicht stört.
		//TODO: 20131027js: review: Was macht in RezeptBlatt.dispose() das super.dispose()? Braucht's das auch in TextView.java? Cave: Modulnamen-Verwirrung beachten, siehe intro comment von js.
		//super.dispose();
		return ret;
	}
	
	@Override
	public void create(){
		super.create();
		setMessage("Terminliste ausdrucken");
		setTitle("Terminliste");
		getShell().setText("Agenda");
		getShell().setSize(800, 700);
		
	}
	
	@Override
	protected void okPressed(){
		super.okPressed();
	}
	
	public void save(){}
	
	public boolean saveAs(){
		return false;
	}
	
	public boolean doPrint(){
		if (text == null) {
			// text container is not initialized
			return false;
		}
		
		String printer =
			Hub.localCfg.get(PreferenceConstants.AG_PRINT_APPOINTMENTCARD_PRINTER_NAME, "");
		String tray =
			Hub.localCfg.get(PreferenceConstants.AG_PRINT_APPOINTMENTCARD_PRINTER_TRAY, null);
		
		return text.getPlugin().print(printer, tray, false);
	}
}
