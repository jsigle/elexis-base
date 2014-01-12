/*******************************************************************************
 * Copyright (c) 2007-2011, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     J. Sigle   - added a close method to ensure that Connection and Office server inside the TextPlugin,
 *                  namely inside NOAText_jsl, are closed again after use of this dialog, if appropriate.
 *                  Similarly to TextView.java/Briefauswahl.java, etc. (all other users of TextContainer).
 *                  PUH. VERY LUCKY. This works.
 *                  
 *                  --
 *                  
 *                  Howto test:
 *                  
 *                  Open Elexis
 *                  Perspektive Kontakte.
 *                  Open Windows TaskManager; ensure that no soffice.bin/soffice.exe is running yet (no Office Client Window should be open)
 *                  In View Kontakt: select a few addresses, then ViewMenu: Adressliste drucken
 *                  The Adressliste dialog window (this *is* the GenericPrintDialog) should open, containing an OpenOffice client; and soffice.bin/soffice.exe should appear in the Task manager
 *                  Close the Adressliste dialog window
 *                  soffice.bin/soffice.exe should disappear from the Task manager
 *                  
 *                  Testing Variations:
 *                  
 *                  Try it with multiple different Office documents open: LAST document closed should terminate the soffice.bin/soffice.exe.
 *                  Try it with multiple instances of Elexis.
 *                  Try it with multiple instances of Elexis and multiple different Office documents open in each of them.
 *                  Try it with external OpenOffice clients open; e.g. Schnellstarter or a simle Writer document.
 *                  
 *                  As of 20131028js: Please note: Problem may occur IF externally, a simple Writer document is open, changed and never saved.
 *                  
 *                  --
 *                  
 *                  A current search in Eclipse shows that References to this module come only from ch.elexis.views/KontakteView/makeActions()/new Action()/run()
 *                  
 *     G. Weirich - initial API and implementation
 ******************************************************************************/
package ch.elexis.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.Hub;
//20131028js: never used: import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.data.Brief;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;

/**
 * A Dialog to display data to print
 * 
 * @author Gerry Weirich
 * 
 */
public class GenericPrintDialog extends Dialog implements ICallback {
	String title;
	String subject;
	TextContainer text;
	Brief brief;
	
	public GenericPrintDialog(Shell shell, String title, String subject){
		super(shell);
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java GenericPrintDialog(): begin constructor just returned from super(shell)");
		this.title = title;
		this.subject = subject;
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java GenericPrintDialog(): end constructor");
	}
	
	//20131028js: Added this method completely to GenericPrintDialog, it does parts of what dispose() does in RezeptBlatt.java etc.
	//A similar edit has been made to EtiketteDruckenDialog.java
	//This works as expected for Kontakte: Adressliste.
	@Override
	public boolean close(){
		//Call the original overwritten close method?
		boolean ret = super.close();
		
		System.out.println("\njs ch.elexis.dialogs/GenericPrintDialog.java close(): begin");

		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: Added a close function with at least the text.getPlugin().dispose(); functionality");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: as also added to other TextContainer clients, to ensure that TextPlugin connection/server");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: is closed again after usage. Please review other commented out content of this dispose():");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: Does GenericPrintDialog need more functionality as well?");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		/*
		 * Other content usually found in other TextContainer using clients: commented out, but please review if we should need anything. 
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

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
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): about to txt.getPlugin().dispose()");
		text.getPlugin().dispose();		
		
		/*
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): about to GlobalEventDispatcher.removeActivationListener()...");
		GlobalEventDispatcher.removeActivationListener(this, this);
		*/

		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): about to super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java create(): beginning - just returned from super.create()");
		if (title==null) 	System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java create(): about to getShell().setText(title) with WARNING: title == null");
		else				System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java create(): about to getShell().setText(title) with title="+title);
		getShell().setText(title);
		getShell().setSize(900, 700);
		SWTHelper.center(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell(),
			getShell());
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java create(): end");
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java createDialogArea(): begin");
		Composite ret = (Composite) super.createDialogArea(parent);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new FillLayout());
		text = new TextContainer(getShell());
		text.getPlugin().createContainer(ret, this);
		text.getPlugin().showMenu(true);
		text.getPlugin().showToolbar(true);
		brief = text.createFromTemplateName(null, "Liste", Brief.UNKNOWN, Hub.actUser, subject); //$NON-NLS-1$ 
		text.getPlugin().insertText("[Titel]", subject, SWT.LEFT);
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java createDialogArea(): about to return ret");
		return ret;
	}
	
	public void insertText(String pattern, String t){
		text.getPlugin().insertText(pattern, t, SWT.LEFT);
	}
	
	public void insertTable(String place, String[][] table, int[] cellWidths){
		text.getPlugin().insertTable(place, 0, table, cellWidths);
	}
	
	@Override
	public void save(){
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java save(): begin");
		text.saveBrief(brief, Brief.UNKNOWN);
		System.out.println("js ch.elexis.dialogs/GenericPrintDialog.java save(): end");
	}
	
	@Override
	public boolean saveAs(){
		// TODO Auto-generated method stub
		return false;
	}
}
