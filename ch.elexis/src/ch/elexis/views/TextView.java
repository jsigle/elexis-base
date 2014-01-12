/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg Sigle www.jsigle.com
 * All rights reserved.
 *
 * Contributors:
 *    J. Sigle   - addes StatusMonitor, reliable activation on typing, reliable saving, automatic saving depending on passed time since last save and last isModified() 
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.views;

import java.awt.Panel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;		//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.events.KeyListener;	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.events.MouseEvent;	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.events.MouseListener;//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.elexis.dialogs.DocumentSelectDialog;
import ch.elexis.dialogs.SelectFallDialog;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.TextContainer;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.io.FileTool;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.MimeTool;

import ch.elexis.util.IStatusMonitorCallback;	//201306170935js - ensure edits in text documents are noted by Elexis and ultimately stored

public class TextView extends ViewPart implements IActivationListener {
	public final static String ID = "ch.elexis.TextView"; //$NON-NLS-1$
	TextContainer txt;
	// CommonViewer cv;
	Composite textContainer = null;

	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored.
	//Wenn ich actBrief public haben wolle, müsste es auch static werden - wofür ich jetzt nicht
	//schnell garantieren kann und will, dass das keine Probleme macht - und schön wäre das auch nicht -
	//Ausserdem würde es vermutlich ein com.jsigle.noatext_jsl ... NOAText.java,
	//das dieses Feature benötigt, inkompatibel zu Elexis-Versionen machen,
	//in denen TextView actBrief noch private ist. Ich vermute, dass dort "Unten" in NOAText
	//der Zugriff auf actBrief aber auch nicht besser als auf das dort ebenfalls erreichbare doc ist;
	//weil ja hier actBrief auf doc gesetzt wird. Also lasse ich es mal private.
	private Brief actBrief;
		
	private Log log = Log.get("TextView"); //$NON-NLS-1$
	private IAction briefLadenAction, loadTemplateAction, loadSysTemplateAction,
			saveTemplateAction, showMenuAction, showToolbarAction, importAction, newDocAction,
			exportAction;
	private ViewMenus menus;
	
	public TextView(){}
	
	@Override
	public void createPartControl(Composite parent){
		System.out.println("\njs ch.elexis.views/TextView.java createPartControl(): begin");
		
		txt = new TextContainer(getViewSite());
		textContainer = txt.getPlugin().createContainer(parent, new SaveHandler());
		if (textContainer == null) {
			SWTHelper
				.showError(
					Messages.getString("TextView.couldNotCreateTextView"), Messages.getString("TextView.couldNotLoadTextPlugin")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			makeActions();
			menus = new ViewMenus(getViewSite());
			// menus.createToolbar(briefNeuAction);
			menus.createMenu(newDocAction, briefLadenAction, loadTemplateAction,
				loadSysTemplateAction, saveTemplateAction, null, showMenuAction, showToolbarAction,
				null, importAction, exportAction);
			
			/*
			 * An dieser Stelle sehe ich keinen Effekt der ausprobierten Listeners.
			 * Ich probiere es in TextView, BriefAuswahl...
			
			//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored (begin).
			
			//added a keyListener to the TextContainer Object
			//just for testing - ultimately I may want to add a keyListner/mouseListener/whateverListener
			//to dectect when tnput goes to a Brief/Rezept/etc. edited via OpenOffice/LibreOffice -
			//that should consequently *activate* = bring the focus to the respective "view".
			//Until now, it is perfectly possible to write in such a document,
			//then click on something in view "Briefauswahl" (or other views) whereupon focus will go there,
			//then click again in the text area of the Brief, whereupon a working text cursor will go there,
			//but *focus will not come back* at the same time. Whereupon Elexis does *not* note that the
			//Brief window has been activated, that edits are made there, and that it should save the
			//document once more at the next available opportunity. The resulting edits will probably be lost.
			System.out.println("\njs ch.elexis.views/TextView.java createPartControl(): about to textContainer.addKeyListener()...");
			textContainer.addKeyListener(new KeyListener() {
				@Override
			    public void keyPressed(KeyEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().KeyListener(): " + e.keyCode + " pressed");
			    }
				@Override
			    public void keyReleased(KeyEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().KeyListener(): " + e.keyCode + " released");
			    }
				@Override
			    public void keyTyped(KeyEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().KeyListener(): " + e.character + " typed");
			    }
			});
			
			System.out.println("\njs ch.elexis.views/TextView.java createPartControl(): about to textContainer.addMouseListener()...");
			textContainer.addMouseListener(new MouseListener() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().MouseListener(): " + e.button + " DoubleClick");					
				}
				@Override
				public void mouseDown(MouseEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().MouseListener(): " + e.button + " Down");
				}
				@Override
				public void mouseUp(MouseEvent e) {
			        System.out.println("js ch.elexis.views/TextView.java createPartControl().MouseListener(): " + e.button + " Up");
				}
			});
			//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored (end).
			*/
				
			GlobalEventDispatcher.addActivationListener(this, this);
			setName();
		}

		System.out.println("js ch.elexis.views/TextView.java createPartControl(): end\n");
	}
	
	@Override
	public void setFocus(){
		System.out.println("\njs ch.elexis.views/TextView.java setFocus(): begin");

		if (textContainer != null) {
			textContainer.setFocus();
			//201306170113js: This has not helped activating the Text view when once again clicking into the office text.
			//Still, it only receives keyboard focus, but the view frame remains "inactive".
			//textContainer.setEnabled(true);
		}

		System.out.println("js ch.elexis.views/TextView.java setFocus(): end\n");
	}
	
	public TextContainer getTextContainer(){
		System.out.println("js ch.elexis.views/TextView.java getTextContainer(): begin and returning txt="+txt.toString());
		return txt;
	}
	
	@Override
	public void dispose(){
		System.out.println("\njs ch.elexis.views/TextView.java dispose(): begin");

		//20130425js added this - otherwise, neither would the doc have reliably been saved upon closing its frame
		//(but possibly when that lost focus?), NOR would the frame have been take from the noas list of NOAText instances,
		//and remained on that forever, so the OpenOffice/LibreOffice server use would never have been deactivated. 
		
		System.out.println("\n\njs ch.elexis.views/TextView.java dispose(): TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO");
		System.out.println("js ch.elexis.views/TextView.java dispose(): When you close a document frame, NOAText closeListener() should probably run. DO YOU OBSERVE THIS?");
		System.out.println("js ch.elexis.views/TextView.java dispose(): And NOAText closeListener should call removeMe(), thereby saving the last instance of the document,");
		System.out.println("js ch.elexis.views/TextView.java dispose(): and housekeeping like noas.remove() and deactivateOfficeIfNoasIsEmpty()...");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO REVIEW TODO");			
		
		//201306161401js
		ch.elexis.util.StatusMonitor.removeMonitorEntry("TextView");

		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: Bitte auskommentierte Zeilen in TextView.dispose() löschen.");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		//Das hier war nur ein Versuch, mal die Aufrufbarkeit von ...showView() zu testen.
		//Nicht mehr nützlich, nicht produktiv, nur overhead.
		//try {
		//	getSite().getPage().showView(TextView.ID);
		//} catch (PartInitException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//} 
		
		
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

		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): ToDo: SOLLTE hier ein plugin().dispose() rein - siehe Kommentare - oder würde das im Betrieb nur unerwünscht Exceptions werfen (gerade gesehen)?");
		System.out.println("js ch.elexis.views/TextViewr.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//System.out.println("js ch.elexis.views/TextView.java dispose(): about to txt.getPlugin().dispose()");
		//txt.getPlugin().dispose();
		
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: Bitte in TextView.java, RezeptBlatt.java, AU, etc. das alles noch spiegeln: StatusMonitor, dispose handler, etc.!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java dispose(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				
		System.out.println("js ch.elexis.views/TextView.java dispose(): about to GlobalEventDispatcher.removeActivationListener()...");
		GlobalEventDispatcher.removeActivationListener(this, this);
		System.out.println("js ch.elexis.views/TextView.java dispose(): about to actBrief = null; super.dispose()...");
		actBrief = null;
		super.dispose();

		System.out.println("js ch.elexis.views/TextView.java dispose(): end\n");
	}
	
	public boolean openDocument(Brief doc){
		//20130421js: Added more monitoring code to see what's happening...
		System.out.println("\njs ch.elexis.views/TextView.java openDocument(Brief doc): begin");
		System.out.println("js ch.elexis.views/TextView.java openDocument(Brief doc): about to txt.open("+doc.getBetreff().toString()+")");

		if (txt.open(doc) == true) {		//"einen Brief einlesen" says javadoc...
			
			System.out.println("js ch.elexis.views/TextView.java openDocument(): txt.open(doc) returned TRUE.");
			System.out.println("js ch.elexis.views/TextView.java openDocument(): about to actBrief = doc...");
			actBrief = doc;
			System.out.println("js ch.elexis.views/TextView.java openDocument(): about to setName()...");
			setName();
			System.out.println("js ch.elexis.views/TextView.java openDocument(): actBrief == ("+actBrief.getBetreff().toString()+")...");
			
			//201306161205js: Now also create a status monitor thread:
			ch.elexis.util.StatusMonitor.addMonitorEntry("TextView", new SaveHandler(), new ShowViewHandler());

			System.out.println("js ch.elexis.views/TextView.java openDocument(Brief doc) successful. Returning true; actBrief == ("+actBrief.getBetreff().toString()+")...");
			return true;
		} else {
			System.out.println("js ch.elexis.views/TextView.java WARNING: txt.open(doc) returned FALSE.");

			//SWTHelper.showError(
			//Messages.getString("TextView.noTemplateSelected"), Messages.getString("TextView.pleaseSelectTemplate")); //$NON-NLS-1$ //$NON-NLS-2$
			SWTHelper.showError("js: Fehler in TextView.java", "txt.open(doc) returned false. Das kann passieren, wenn ein Dokument nicht wie erwartet aus der Datenbank geladen werden kann.");
			
			System.out.println("js ch.elexis.views/TextView.java openDocument(): about to actBrief = null...");
			actBrief = null;
			System.out.println("js ch.elexis.views/TextView.java openDocument(): about to setName()...");
			setName();

			System.out.println("js ch.elexis.views/TextView.java INFO: About to call corrected MimeTool.getExtension(),");
			System.out.println("js ch.elexis.views/TextView.java INFO: This will map \".odt\" to \"application/vnd.oasis.opendocument.text\",");
			System.out.println("js ch.elexis.views/TextView.java INFO: and NOT \".ods\", like previous versions of MimeTool.java");
			String ext = MimeTool.getExtension(doc.getMimeType());
			
			if (ext.length() == 0) {
				System.out.println("js ch.elexis.views/TextView.java INFO: MimeTool.getExtension() returned an empty extension.");
				System.out.println("js ch.elexis.views/TextView.java INFO: Using standard open document text extension \".odt\",");
				System.out.println("js ch.elexis.views/TextView.java INFO: and NOT \".ods\", which was in previous versions of TextView.java");
				//20130423js: Using standard open document text extension .odt and not .ods, which was in previous versions of MimeTool.java and TextView.java 
				//ext = "ods"; //$NON-NLS-1$
				ext = "odt"; //$NON-NLS-1$
			}
			
			try {
				System.out.println("js ch.elexis.views/TextView.java WARNING: now trying to createTempFile(\"+elexis*brief."+ext+"\") to store the ByteArrayInputStream...");
				
				//Test mit: Muster Max 07.03.2012 Status - das fehlt in der DB.
				System.out.println("\n\n\nTODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO\njs+\n"+
				"der elexis-handler schreibt zwar wine kurze und knappe fehlermeldung (\"Kann Datein nicht laden\")\n,"+
				"lässt dann aber ebenfalls das alte Dokument im textViewer, mit geleertem Titel. -> Verbessern.\n"+
				"Ausserdem mal prüfen, warum beides passiert -\n"+
				"also gibt es einen legitimen Situation, bei der der obere Teil failen darf, und der untere dann etwas brauchbares liefert???\n" +
				//"\n(ICH HAB UNTEN MAL EIN txt.dispose(); dazugetan, das scheint gut auszusehen. :-)\n\n"+ - nein, jedenfalls nicht alleine, details siehe unten
				" TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO\n\n"); 
			
				System.out.println("js Shouldn't we create a configurable temporary filename here as well?!?\n"+
				" TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO\n\n"); 
			
				System.out.println("js ch.elexis.views/TextView.java: About to createTempFile()...");
				File tmp = File.createTempFile("elexis", "brief." + ext); //$NON-NLS-1$ //$NON-NLS-2$
				tmp.deleteOnExit();

				/*
				 * 
				 The alternative Version will NOT work, if ByteArrayInputStream encounters doc.loadBinary() returning null
				 In that case, it would throw a null exception, and this would persist even through the catch{} block
				 So really use the differentiated multilevel try/catch code I made - and success: This really avoids the null exception breaking everything.
				 
				 
				 
				//An alternative version where each command is caught in detail and a warning dialog displayed is shown below.
				//Instead of implementing that, it's probably MORE reasonable, practical, and effectively correct, to just catch
				//all possible problems as (Throwable e); rather than merele catching (IOEception e).
				//Because that will finally get the TextView frame empty, rather than leaving the previously displayed content (but w/o any connection). 
				System.out.println("js ch.elexis.views/TextView.java: About to new ByteArrayInputStream()...");
				ByteArrayInputStream bais = new ByteArrayInputStream(doc.loadBinary());
				System.out.println("js ch.elexis.views/TextView.java: About to new new FileOutputStream()...");
				FileOutputStream fos = new FileOutputStream(tmp);
				System.out.println("js ch.elexis.views/TextView.java: About to FileTool.copyStreams()...");
				FileTool.copyStreams(bais, fos);
				*/
				
			    //20130423js: Adding additional try/catches around the following statements
				//Even when txt.openDocument() above returns false, Elexis will go on here trying to write something to a document.
				//If a document is not available from the DB as expected, the byteArrayInputStream() will fail; but I want to see in detail
				//- and still handle it gracefully - when any statements of the following sequence fail.
				//N.B.: NOT all possible exceptions are covered by the originally included catch (IOException e).   
				try {
					System.out.println("js ch.elexis.views/TextView.java: About to new ByteArrayInputStream()...");
					ByteArrayInputStream bais = new ByteArrayInputStream(doc.loadBinary());
					try {
						System.out.println("js ch.elexis.views/TextView.java: About to new new FileOutputStream()...");
						FileOutputStream fos = new FileOutputStream(tmp);
						try {
							System.out.println("js ch.elexis.views/TextView.java: About to FileTool.copyStreams()...");
							FileTool.copyStreams(bais, fos);
						
							System.out.println("js ch.elexis.views/TextView.java WARNING: returning (attempted) Program.launch(tmp.getAbsolutePath())...");
							System.out.println("js ch.elexis.views/TextView.java WARNING: tmp.getAbsolutePath()==<"+tmp.getAbsolutePath()+">\n");
							return Program.launch(tmp.getAbsolutePath());
							
						} catch (Throwable throwable) {
							SWTHelper.showError("js: Fehler in TextView.java", "FileTool.copyStreams(bais, fos) returned false. Das kann passieren, wenn ein Dokument nicht wie erwartet aus der Datenbank geladen werden kann.");
							System.out.println("WARNING: The fact that I caugt this exception now may change program behaviour for future debugging!!!");
							System.out.println("WARNING: It might be more reasonable and practical to simply change the catch (IOException e) below to catch (Throwable e)");
							System.out.println("WARNING: If we wanted to open a document from the DB and this failed, the setName() may/will? alreday have cleared the title of the target TextView tab, but we won't see the expected document loaded therein, but the previously shown one will remain instead.");
							System.out.println("WARNING: It is THEN possible to type in the left over TextView content, but that will NOT be saved back to the DB - neither to the original provider of its content, nor to the desired, but unavailable more recently attempted to open one.");

							System.out.println("WARNING: So when we clear the tab Title by SetName() (setting that to empty), we should probably also cause a clear frame/unload OO/whatever make it more visible there's nothing usable left over here");
						}
					} catch (Throwable throwable) {
						SWTHelper.showError("js: Fehler in TextView.java", "new FileOutputStream(tmp) returned false. Das kann passieren, wenn ein Dokument nicht wie erwartet aus der Datenbank geladen werden kann.");
						System.out.println("WARNING: The fact that I caugt this exception now may change program behaviour for future debugging!!!");
						System.out.println("WARNING: It might be more reasonable and practical to simply change the catch (IOException e) below to catch (Throwable e)");
						System.out.println("WARNING: If we wanted to open a document from the DB and this failed, the setName() may/will? alreday have cleared the title of the target TextView tab, but we won't see the expected document loaded therein, but the previously shown one will remain instead.");
						System.out.println("WARNING: It is THEN possible to type in the left over TextView content, but that will NOT be saved back to the DB - neither to the original provider of its content, nor to the desired, but unavailable more recently attempted to open one.");

						System.out.println("WARNING: So when we clear the tab Title by SetName() (setting that to empty), we should probably also cause a clear frame/unload OO/whatever make it more visible there's nothing usable left over here");
					}
				} catch (Throwable throwable) {
					SWTHelper.showError("js: Fehler in TextView.java", "new ByteArrayInputStream(doc.loadBinary()) returned false. Das kann passieren, wenn ein Dokument nicht wie erwartet aus der Datenbank geladen werden kann.");
					System.out.println("WARNING: The fact that I caugt this exception now may change program behaviour for future debugging!!!");
					System.out.println("WARNING: It might be more reasonable and practical to simply change the catch (IOException e) below to catch (Throwable e)");
					System.out.println("WARNING: If we wanted to open a document from the DB and this failed, the setName() may/will? alreday have cleared the title of the target TextView tab, but we won't see the expected document loaded therein, but the previously shown one will remain instead.");
					System.out.println("WARNING: It is THEN possible to type in the left over TextView content, but that will NOT be saved back to the DB - neither to the original provider of its content, nor to the desired, but unavailable more recently attempted to open one.");

					System.out.println("WARNING: So when we clear the tab Title by SetName() (setting that to empty), we should probably also cause a clear frame/unload OO/whatever make it more visible there's nothing usable left over here");
				}
				
				//Well, if creating the file should fail, then returning an absolute path might fail as well.
				//AND, if I caught the failures thrown at new ByteArrayInputStream, the file would still be created and returned,
				//so this *might* change behaviour... or wouldn't it? Because the file was there anyway, and would be returned,
				//even when ByteArrayInputStream would have produced an exception... or no, it wouldn't as that exception would have caused
				//a transition to some distant other point in the program, probably without a return into the same block down here.

				//I put this up in the multilevel try catch loop:
				//System.out.println("js ch.elexis.views/TextView.java WARNING: now returning (attempted) Program.launch(tmp.getAbsolutePath())...");
				//System.out.println("js ch.elexis.views/TextView.java WARNING: tmp.getAbsolutePath()==<"+tmp.getAbsolutePath()+">");
				//return Program.launch(tmp.getAbsolutePath());
				
			//20130423js: This also catches fruitless attempts to retrieve database content that unexpectedly isn't available.
			//Gerry's ExHandler.handle(e) is so simple that it (probably, according to javadoc) won't handle these events differently anyway.
			//} catch (IOException e) {
			//	System.out.println("js ch.elexis.views/TextView.java WARNING: catching IOException e=="+e);
			} catch (Throwable e) {
				System.out.println("js ch.elexis.views/TextView.java WARNING: catching Exception e=="+e);
				System.out.println("js ch.elexis.views/TextView.java (This would cover both IOException and the NullPointer Exception after failing new ByteArrayInputStream()  )");
	
				//After this exception handler, the exception (in case of a null exception, that could get here before the multilevel try/catch approach)
				//continues to be viable -
				//no matter whether we return directly from here, or after going through the catch {} block.  
				//(for some scenarios) this shows the cause: "null"
				System.out.println("js ch.elexis.views/TextView.java: cause for exception: "+e.getCause());
				//this returns: error: cannot suppress a null exception. OK, that explains a few things...
				//e.addSuppressed(null);
				
				ExHandler.handle(e);
			}
			
			//When come here, something probably hasn't go as intended.
			
			System.out.println("js ch.elexis.views/TextView.java TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO js");
			//System.out.println("js ch.elexis.views/TextView.java WARNING: Ich mach hier mal txt.dispose(); dazu, mal schauen, ob das den gewünschten Effekt hat - also, wenn das Laden fehlschlägt, und vorher was drin war, dann nicht nur Fenstertitel löschen und Verbindung killen, sondern auch angezeigten Inhalt löschen!"+e);
			
			//Das folgende entfernt das geladene Dokument, aber auch gleich das plugin mit. Wäre meinetwegen ganz ok - aber beim nächsten Load wird das plugin nicht neu kontaktiert, stattdessen Fehlermeldung xyz has been disposed... 
			//txt.dispose();
			
			//Das folgende befreit das Dokument schonmal sichtbar von text, aber es belässt einen geladenen Brief. Also keine schöne Lösung::
			//txt.replace(".*","");
			
			//This does not work. NOAText.clear() first checks, whether texthandler != null, and either that's NOT the case - so the
			//clearing routines are not executed (that would be: save(), setModified(false) - so where's the clearing, actually???) -
			//but that portion is not carried out as clear() quite immediately returns false.
		
			//Das bringt wenigstens ein leeres Dokument in die Briefe.View.
			//Dahinein kann man immer noch tippen, wobei jedoch der Inhalt später nirgends gespeichert wird.
			txt.getPlugin().createEmptyDocument();
			//txt.getPlugin().insertText("^", "Warnung: Dummy-Dokument nach Ladefehler. Inhalt wird nicht gespeichert.", SWT.LEFT);
			//txt.getPlugin().insertText(null, "Warnung: Dummy-Dokument nach Ladefehler. Inhalt wird nicht gespeichert.", SWT.LEFT);
			txt.getPlugin().insertTextAt(30,50,200,200,"Warnung:\n\nDies ist ein Dummy-Dokument nach Ladefehler.\n"+
					"Der Inhalt wird nirgendwo gespeichert.\n"+
					"Bitte öffnen Sie ein anderes Dokument zum Bearbeiten\n"+
					"oder erstellen Sie ein neues Dokument.\n\n"+
					"ToDo für Jörg Sigle, joerg.sigle@jsigle.com:\n"+
					"TextView Inhalt löschen, statt leeres Dokument zu erzeugen.", SWT.LEFT);

			//Das bring t KEINE sichtbare Änderung des Inhalts...
			//System.out.println("js ch.elexis.views/TextView.java About to txt.getPlugin().clear()...");
			txt.getPlugin().clear();
			
			System.out.println("js ch.elexis.views/TextView.java TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO js");
			System.out.println("js ch.elexis.views/TextView.java Question: Why would txt.getPlugin().clear() apparently not do anything like clearing?!? It appears to just clear the modified flag, nothing else!");
			System.out.println("js ch.elexis.views/TextView.java TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO js");
			
		
			System.out.println("js ch.elexis.views/TextView.java JEP, das sieht gut aus :-) der Frame wird leer...  js");
			System.out.println("js ch.elexis.views/TextView.java JEP, das sieht gut aus :-) HMPF danach ist's aber nicht mehr gut: Widget is disposed, wenn ich das nächste doc laden will. js");
			System.out.println("js ch.elexis.views/TextView.java TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO js");
		
			System.out.println("js ch.elexis.views/TextView.java WARNING: returning false\n");
			return false;
		}
	}
	
	/**
	 * Ein Document von Vorlage erstellen.
	 * 
	 * @param template
	 *            die Vorlage
	 * @param subject
	 *            Titel, kann null sein
	 * @return true bei erfolg
	 */
	public boolean createDocument(Brief template, String subject){
		System.out.println("\njs ch.elexis.views/TextView.java createDocument(Brief template, String subject): begin");
		
		if (template == null) {
			SWTHelper
				.showError(
					Messages.getString("TextView.noTemplateSelected"), Messages.getString("TextView.pleaseSelectTemplate")); //$NON-NLS-1$ //$NON-NLS-2$
			
			System.out.println("js ch.elexis.views/TextView.java createDocument(2): WARNING: returning false\n");
			return false;
		}
		actBrief =
			txt.createFromTemplate(Konsultation.getAktuelleKons(), template, Brief.UNKNOWN, null,
				subject);
		setName();
		if (actBrief == null) {
			System.out.println("js ch.elexis.views/TextView.java createDocument(2): WARNING: returning false\n");
			return false;
		}
		
		//201306161205js: Now also create a status monitor thread:
		ch.elexis.util.StatusMonitor.addMonitorEntry("TextView", new SaveHandler(), new ShowViewHandler());
		
		System.out.println("js ch.elexis.views/TextView.java createDocument(2): returning true\n");
		return true;
	}
	
	/**
	 * Ein Document von Vorlage erstellen. Adressat kann hier angegeben werden
	 * 
	 * @param template
	 *            die Vorlage
	 * @param subject
	 *            Titel, kann null sein
	 * @param adressat
	 *            der Adressat, der im Dokument angezeigt werden soll
	 * @return true bei erfolg
	 */
	public boolean createDocument(Brief template, String subject, Kontakt adressat){
		System.out.println("\njs ch.elexis.views/TextView.java createDocument(Brief template, String subject, Kontakt adressat): begin");
		
		if (template == null) {
			SWTHelper
				.showError(
					Messages.getString("TextView.noTemplateSelected"), Messages.getString("TextView.pleaseSelectTemplate")); //$NON-NLS-1$ //$NON-NLS-2$
			
			System.out.println("js ch.elexis.views/TextView.java createDocument(3): WARNING: returning false\n");
			return false;
		}

		actBrief = txt.createFromTemplate(Konsultation.getAktuelleKons(), template, Brief.UNKNOWN, adressat, subject);
		if (actBrief == null) {
			System.out.println("js ch.elexis.views/TextView.java createDocument(3): WARNING: returning false\n");
			return false;
		}
		setName();
		
		//201306161205js: Now also create a status monitor thread:
		ch.elexis.util.StatusMonitor.addMonitorEntry("TextView", new SaveHandler(), new ShowViewHandler());

		System.out.println("js ch.elexis.views/TextView.java createDocument(3): returning true\n");
		return true;
	}
	
	private void makeActions(){
		System.out.println("\njs ch.elexis.views/TextView.java makeActions(): begin");
		
		briefLadenAction = new Action(Messages.getString("TextView.openLetter")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("js ch.elexis.views/TextView.java makeActions().briefLadenAction().run(): begin");
					
					Patient actPatient = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
					DocumentSelectDialog bs =
						new DocumentSelectDialog(getViewSite().getShell(), actPatient,
							DocumentSelectDialog.TYPE_LOAD_DOCUMENT);

					System.out.println("js ch.elexis.views/TextView.java makeActions().briefLadenAction().run(): about to openDocument()");
					if (bs.open() == Dialog.OK) {
						openDocument(bs.getSelectedDocument());
					}
					
					System.out.println("js ch.elexis.views/TextView.java makeActions().briefLadenAction().run(): end\n");
				}
				
			};
		
		loadSysTemplateAction = new Action(Messages.getString("TextView.openSysTemplate")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().loadSysTemplateAction().run(): begin");

					DocumentSelectDialog bs =
						new DocumentSelectDialog(getViewSite().getShell(), Hub.actMandant,
							DocumentSelectDialog.TYPE_LOAD_SYSTEMPLATE);

					System.out.println("js ch.elexis.views/TextView.java makeActions().loadSysTemplateAction().run(): about to openDocument()");
					if (bs.open() == Dialog.OK) {
						openDocument(bs.getSelectedDocument());					
					}
					
					System.out.println("js ch.elexis.views/TextView.java makeActions().loadSysTemplateAction().run(): end\n");
				}
			};

		loadTemplateAction = new Action(Messages.getString("TextView.openTemplate")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().loadTemplateAction().run(): begin");

					DocumentSelectDialog bs =
						new DocumentSelectDialog(getViewSite().getShell(), Hub.actMandant,
							DocumentSelectDialog.TYPE_LOAD_TEMPLATE);

					System.out.println("js ch.elexis.views/TextView.java makeActions().loadTemplateAction().run(): about to openDocument()");
					if (bs.open() == Dialog.OK) {
						openDocument(bs.getSelectedDocument());
					}
					
					System.out.println("js ch.elexis.views/TextView.java makeActions().loadTemplateAction().run(): end\n");
				}
			};
			
		saveTemplateAction = new Action(Messages.getString("TextView.saveAsTemplate")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().saveTemplateAction().run(): begin");

					if (actBrief != null) {
						txt.saveTemplate(actBrief.get(Messages.getString("TextView.Subject"))); //$NON-NLS-1$
					} else {
						txt.saveTemplate(null);
					}
					
					System.out.println("js ch.elexis.views/TextView.java makeActions().saveTemplateAction().run(): end\n");
				}
			};
		
		showMenuAction = new Action(Messages.getString("TextView.showMenu"), Action.AS_CHECK_BOX) { //$NON-NLS-1$			

				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().showMenuAction().run(): begin");

					txt.getPlugin().showMenu(isChecked());

					System.out.println("js ch.elexis.views/TextView.java makeActions().showMenuAction().run(): end\n");
				}

			};
		
		showToolbarAction =
			new Action(Messages.getString("TextView.Toolbar"), Action.AS_CHECK_BOX) { //$NON-NLS-1$

			public void run(){
				System.out.println("\njs ch.elexis.views/TextView.java makeActions().showToolbarAction().run(): begin");

				txt.getPlugin().showToolbar(isChecked());

				System.out.println("js ch.elexis.views/TextView.java makeActions().showToolbarAction().run(): end\n");
				}
			
			};
			
		importAction = new Action(Messages.getString("TextView.importText")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().importAction().run(): begin");

					try {
						FileDialog fdl = new FileDialog(getViewSite().getShell());
						String filename = fdl.open();
						if (filename != null) {
							System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): about to file = new File(filename)...");
							File file = new File(filename);
							if (file.exists()) {
								System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): about to actBrief = null...");
								actBrief = null;
								System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): about to setPartName(filename) with filename=="+filename+"...");
								setPartName(filename);
								System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): about to fis = new FileInputStream(file)...");
								FileInputStream fis = new FileInputStream(file);
								System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): about to txt.getPlugin().loadFromStream(fis,false)...");
								txt.getPlugin().loadFromStream(fis, false);
								System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
							}
							
						}
						
					} catch (Throwable ex) {
						System.out.println("js ch.elexis.views/TextView.java makeActions().importAction(): catching Throwable ex...");
						ExHandler.handle(ex);
					}

				System.out.println("js ch.elexis.views/TextView.java makeActions().importAction.run(): end\n");
				}
			};
		
		exportAction = new Action(Messages.getString("TextView.exportText")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().exportAction().run(): begin");

					try {
						if (actBrief == null) {
							SWTHelper.alert("Fehler", //$NON-NLS-1$
								"Es ist kein Dokument zum exportieren geladen"); //$NON-NLS-1$
						} else {
							FileDialog fdl = new FileDialog(getViewSite().getShell(), SWT.SAVE);
							fdl.setFilterExtensions(new String[] {
								"*.odt", "*.xml", "*.*" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							});
							fdl.setFilterNames(new String[] {
								"OpenOffice.org Text", "XML File", "All files" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							});
							String filename = fdl.open();
							if (filename != null) {
								if (FileTool.getExtension(filename).equals("")) { //$NON-NLS-1$
									filename += ".odt"; //$NON-NLS-1$
								}
								File file = new File(filename);
								byte[] contents = actBrief.loadBinary();
								ByteArrayInputStream bais = new ByteArrayInputStream(contents);
								FileOutputStream fos = new FileOutputStream(file);
								FileTool.copyStreams(bais, fos);
								fos.close();
								bais.close();
								
							}
						}
						
					} catch (Throwable ex) {
						ExHandler.handle(ex);
					}
				
				System.out.println("js ch.elexis.views/TextView.java makeActions().exportAction().run(): end\n");
				}
			};
			
		newDocAction = new Action(Messages.getString("TextView.newDocument")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				}
				
				public void run(){
					System.out.println("\njs ch.elexis.views/TextView.java makeActions().newDocAction().run(): begin");

					Patient pat = ElexisEventDispatcher.getSelectedPatient();
					if (pat != null) {
						Fall selectedFall = (Fall) ElexisEventDispatcher.getSelected(Fall.class);
						if (selectedFall == null) {
							SelectFallDialog sfd = new SelectFallDialog(Desk.getTopShell());
							sfd.open();
							if (sfd.result != null) {
								ElexisEventDispatcher.fireSelectionEvent(sfd.result);
							} else {
								MessageDialog
									.openInformation(Desk.getTopShell(),
										Messages.getString("TextView.NoCaseSelected"), //$NON-NLS-1$
										Messages
											.getString("TextView.SaveNotPossibleNoCaseAndKonsSelected")); //$NON-NLS-1$
								System.out.println("js ch.elexis.views/TextView.java makeActions().newDocAction().run(): return\n");
								return;
							}
						}
						Konsultation selectedKonsultation =
							(Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
						if (selectedKonsultation == null) {
							Konsultation k = pat.getLetzteKons(false);
							if (k == null) {
								k =
									((Fall) ElexisEventDispatcher.getSelected(Fall.class))
										.neueKonsultation();
								k.setMandant(Hub.actMandant);
							}
							ElexisEventDispatcher.fireSelectionEvent(k);
						}
						
						System.out.println("js ch.elexis.views/TextView.java makeActions().newDocAction().run(): about to actBrief=null;");
						actBrief = null;
						System.out.println("js ch.elexis.views/TextView.java makeActions().newDocAction().run(): about to setName()");
						setName();
						System.out.println("js ch.elexis.views/TextView.java makeActions().newDocAction().run(): about to txt.getPlugin().createEmptyDocument()");
						txt.getPlugin().createEmptyDocument();
					} else {
						MessageDialog.openInformation(Desk.getTopShell(),
							Messages.getString("BriefAuswahlNoPatientSelected"), //$NON-NLS-1$
							Messages.getString("BriefAuswahlNoPatientSelected")); //$NON-NLS-1$
					}
				
				System.out.println("js ch.elexis.views/TextView.java makeActions().newDocAction().run(): end\n");
				}
				
			};
			
		briefLadenAction.setImageDescriptor(Hub.getImageDescriptor(Messages.getString("TextView.15"))); //$NON-NLS-1$
		briefLadenAction.setToolTipText("Brief zum Bearbeiten öffnen"); //$NON-NLS-1$
		// briefNeuAction.setImageDescriptor(Hub.getImageDescriptor("rsc/schreiben.gif"));
		// briefNeuAction.setToolTipText("Einen neuen Brief erstellen");
		showMenuAction.setToolTipText(Messages.getString("TextView.showMenuBar")); //$NON-NLS-1$
		showMenuAction.setImageDescriptor(Hub.getImageDescriptor("rsc/menubar.ico")); //$NON-NLS-1$
		showMenuAction.setChecked(true);
		showToolbarAction.setImageDescriptor(Hub.getImageDescriptor("rsc/toolbar.ico")); //$NON-NLS-1$
		showToolbarAction.setToolTipText(Messages.getString("TextView.showToolbar")); //$NON-NLS-1$
		showToolbarAction.setChecked(true);
	}

	//201306170655js: I implement this additional callback method to give StatusMonitor a possibility to trigger a ShowView from TextView.java
	//Required, as getView() is not easily accessible from StatusMonitor (and not in a number of more complicated ways I tried either),
	//and moreover, we can keep the TextView.ID etc. up here, and do similar but yet specific things for all other corresponding text processing windows -
	//just letting each of them specify whatever they want to have called from StatusMonitor.java
	class ShowViewHandler implements IStatusMonitorCallback {
		
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
			
					System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
					System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - PLEASE IMPLEMENT Activation of the correct office window!");								
					System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - ToDo: TextView.java");								
					System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - ToDo: RezeptBlatt.java et al.");								
					System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");								
			    	
					//YEP. DAS macht die View aktiv, incl. hervorgehobenem Rahmen, und Focus, in dem der Text drinnen steckt.
					//Im Moment leider noch alle Zeit, also auch dann, wenn gerade NICHT isModified() durch neue Eingaben immer wieder gesetzt würde.
					//TextView.ID liefert: ch.elexis.TextView
					TextView tv = null;
					try {
						System.out.println("js com.jsigle.noa/StatusMonitor.java - run() - Thread: " + Thread.currentThread().getName() + " - about to tv.showView(TextView.ID) with TextView.ID == " + TextView.ID);
						tv = (TextView) getViewSite().getPage().showView(TextView.ID /*,StringTool.unique("textView"),IWorkbenchPage.VIEW_ACTIVATE*/);
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
	}

	//201306170655js: This pre-existing SaveHandler can also be used with/from my new ch.elexis.util.StatusMonitoring class.
	class SaveHandler implements ITextPlugin.ICallback {
		
		public void save(){
			System.out.println("\njs ch.elexis.views/TextView.java SaveHandler.save(): begin");

			log.log(Messages.getString("TextView.save"), Log.DEBUGMSG); //$NON-NLS-1$
			
			if (actBrief != null) {
				System.out.println("js ch.elexis.views/TextView.java SaveHandler.save(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
				System.out.println("js ch.elexis.views/TextView.java SaveHandler.save(): about to save actBrief to DB...");
				//TODO: Why wouldn't we return the result here, but in SaveAs? js
				actBrief.save(txt.getPlugin().storeToByteArray(), txt.getPlugin().getMimeType());
			} else {
				System.out.println("js ch.elexis.views/TextView.java SaveHandler.save(): actBrief == null, doing nothing.");
			}

			System.out.println("js ch.elexis.views/TextView.java SaveHandler.save(): end\n");
		}
		
		public boolean saveAs(){
			System.out.println("\njs ch.elexis.views/TextView.java SaveHandler.saveAs(): end - begin");
			
			//20130424js: Added testing for actBrief != null before doing anything.
			//If the menu option should only be available otherwise, well, call me paranoia.
			
			log.log(Messages.getString("TextView.saveAs"), Log.DEBUGMSG); //$NON-NLS-1$

			if (actBrief != null) {				
				System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): asking user for new name");
				InputDialog il =
					new InputDialog(
						getViewSite().getShell(),
						Messages.getString("TextView.saveText"), Messages.getString("TextView.enterTitle"), "", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
				if (il.open() == Dialog.OK) {
					System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): OldName: actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
					System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): aboutTo: actBrief.setBetreff()...");
					actBrief.setBetreff(il.getValue());
					System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): NewName: actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
					System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): about to save actBrief to DB and end - returning result...\n");
					return actBrief.save(txt.getPlugin().storeToByteArray(), txt.getPlugin().getMimeType());
				}
			}

			System.out.println("js ch.elexis.views/TextView.java SaveHandler.saveAs(): end - returning false\n");
			return false;
		}
		
	}
	
	/*
	 * TODO: js: Bitte mal hier dringend eine Dokumentation ergänzen. Versuch mit Notizen und Vermutungen folgt:
	 * 
	 * Offenbar hat Gerry / whoever - das Speichern des aktuellen TextPlugin-Frame-Whatever-Inhaltes in die Datenbank
	 * an das deaktivieren (+- auch ans close) des zuständigen views gekoppelt - wobei er die .isModified() Funktion
	 * von TextView aus offenbar von hier aus nicht erreicht - siehe:
	 * js NOAText: createMe: doc.addDocumentModifyListener() reactOnUnspecificEvent() doc.isModified()
	 * Cave: Das dortige doc ist wohl nicht identisch mit dem doc hier in TextView.
	 * 
	 * TODO: NOTE: DORT in NOAText.java storeToByteArray() könnte ich das aber leicht abfragen,
	 * nur was sollte ich dann zurückliefern, wenn man nicht speichern müsste? null?
	 * Oder eine Methode isModified() parallel zu storeToByteArray() ergänzen?
	 * Muss ich dann das Plugin-Interface ändern? 
	 * 
	 * 
	 * Weil das iModified hier nicht verfügbar ist, wird wohl vereinfachend angenommen,
	 * dass ein Speichern beim Verlassen des Focus vom Bearbeitungsfenster eine gute Idee wäre.
	 * 
	 * Nur LEIDER bekommt (!) ( und dadurch auch: -> verliert) diese "View" den Focus nicht komplett/zuverlässig,
	 * wenn man mit der Maus hineinklickt:
	 * Da kann durchaus die "View" "Briefauswahl" aktiv sein, und bleiben, während man im Text weiterschreibt.
	 * Dabei fällt also kein activation(true) -> activation(false) Zyklus an, und so können dann Änderungen verloren gehen.
	 *   
	 * Warum ein activation(true) dann noch irgendwelche Menüeinträge enabled, erschliesst sich mir momentan überhaupt nicht.
	 * Oder...:
	 * 
	 * Ich weiss aber, dass diese Einträge gelegentlich nicht aktiv waren, wenn ich sie brauchte, und ein klick-raus, klick-rein
	 * hat sie dann aktiviert. Das ist hier implementiert; ich vermute, dass das ein Fehler ist; stattdessen sollten die Einträge
	 * entweder immer aktiv sein, und/oder beim create... irgendwo angelegt und gleich aktiviert werden, wenn sinnvoll.
	 * Ach, vielleicht ist es dafür gemacht, dass der Aktivierungsstatus der Menüeinträge aktualisiert wird, wenn man
	 * nach dem Abmelden/Anmelden eines neuen Users auf das Textfenster klickt.
	 * Dann fehlt aber die zuverlässige Basis-Aktivierung beim ersten Erzeugen/Verwenden des Plugin-Fensters.
	 * Vermutlich ist die Aktivierung sicher erreicht, wenn man mit der Maus in den Manübereich des Plugin-Views klickt;
	 * beim Klick in den Tab=Titel-Bereich geht die Aktivierung auch sicher ein. Nur beim Klicke mitten in den Textbereich etc.
	 * hinein nicht.
	 * Mglw. wäre Voraussetzung auch, dass die aktivierung beim Ab/Anmelden vom TextView zuverlässig verloren wird??? Passiert das? 
	 *   
	 * @see ch.elexis.actions.GlobalEventDispatcher.IActivationListener#activation(boolean)
	 */
	public void activation(boolean mode){
		System.out.println("\njs ch.elexis.views/TextView.java activation(mode="+mode+"): begin");
		System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		System.out.println("js ch.elexis.views/TextView.java TODO: What in the world would this method be intended to do? Are comments sooo expensive?");
		System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		if (mode == false) {
			System.out.println("js ch.elexis.views/TextView.java activation(false) requested: if actBrief != null then actBrief.save()");
			
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views/TextView.java TODO: Should'nt we test for doc.isModified() here as well? only save if anything changed?");
			System.out.println("js ch.elexis.views/TextView.java TODO: doc.isModified() == HMPF NICHT ERREICHBAR.");
			//Das folgende geht nur, wenn die Dinge auch existieren - also nicht während des Startups des Programms. Da muss man dann == null extra handeln.
			//System.out.println("js ch.elexis.views/TextView.java TODO: actBrief.getPatient().getName() == " + actBrief.getPatient().getName());
			//System.out.println("js ch.elexis.views/TextView.java TODO: actBrief.getBetreff() == " + actBrief.getBetreff());
			//System.out.println("js ch.elexis.views/TextView.java TODO: actBrief.getLabel() == " + actBrief.getLabel());
			System.out.println("js ch.elexis.views/TextView.java TODO: txt.getPlugin().toString() == " + txt.getPlugin().toString());				
			System.out.println("js ch.elexis.views/TextView.java TODO: Hmmm. OK, I'm adding that test to NoaText_jsl: NOAText.java storeToByteArray()...");
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views/TextView.java TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			
			if (actBrief != null) {
				System.out.println("js ch.elexis.views/TextView.java activation(): actBrief == "+actBrief.toString()+": "+actBrief.getBetreff());
				System.out.println("js ch.elexis.views/TextView.java activation(): about to save actBrief to DB...");
				actBrief.save(txt.getPlugin().storeToByteArray(), txt.getPlugin().getMimeType());
			} else {
				System.out.println("js ch.elexis.views/TextView.java activation(): actBrief == null - nothing to save.");				
			}
			
			/*
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE ");
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: PLEASE REVIEW  PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW\n\n\n");
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: HERE IS A COMMENTED OUT txt.getPlugin().clear(), maybe we should re-enable this for stability???");
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: Or only, if the save() above was actually and reliably successful?");
			//txt.getPlugin().clear();
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: NO, PROBABLY NOT - as this code runs e.g. as activation(false), when the focus in elexis is moved from the TextView Window to e.g. another entry in the BriefeAuswahl Window.");
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: PLEASE REVIEW  PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW PLEASE REVIEW\n\n\n");
			System.out.println("js ch.elexis.views/TextView.java activation(): TODO: DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE DONE ");
			*/
			
		} else {
			System.out.println("js ch.elexis.views/TextView.java activation(true) requested: loadSystTemplateAction.setEnabled(); saveTemplateAction.setEnabled()");

			loadSysTemplateAction.setEnabled(Hub.acl.request(AccessControlDefaults.DOCUMENT_SYSTEMPLATE));
			saveTemplateAction.setEnabled(Hub.acl.request(AccessControlDefaults.DOCUMENT_TEMPLATE));
		}

	System.out.println("js ch.elexis.views/TextView.java activation(): end\n");
	}
	
	public void visible(boolean mode){
		System.out.println("js ch.elexis.views/TextView.java visible(): WARNING: This function is called but has no code.");		
	}
	
	void setName(){
		System.out.println("\njs ch.elexis.views/TextView.java setName(): begin");

		String n = ""; //$NON-NLS-1$
		
		if (actBrief == null) {
			System.out.println("js ch.elexis.views/TextView.java setName(): WARNING: actBrief==null; about to produce warning dialog...");

			setPartName(Messages.getString("TextView.noLetterSelected")); //$NON-NLS-1$
		} else {
			System.out.println("js ch.elexis.views/TextView.java setName(): actBrief=="+actBrief.toString()+": <"+actBrief.getBetreff()+">");

			Person pat = actBrief.getPatient();

			if (pat==null) {
				System.out.println("js ch.elexis.views/TextView.java setName(): WARNING: actBrief.getPatient()==null");
			} else {
				System.out.println("js ch.elexis.views/TextView.java setName(): actBrief.getPatient()=="+pat);
			}

			if (pat != null) {
				n = pat.getLabel() + ": "; //$NON-NLS-1$

				System.out.println("js ch.elexis.views/TextView.java setName(): n=actBrief.getPatient().getLabel()+\": \"=="+n);
			}
			n += actBrief.getBetreff();

			System.out.println("js ch.elexis.views/TextView.java setName(): n+=actBrief.getBetreff()=="+n);
			System.out.println("js ch.elexis.views/TextView.java setName(): about to setPartName(n)... (the TextView tab window title)");
			
			setPartName(n);
		}

	System.out.println("js ch.elexis.views/TextView.java setName(): end\n");
	}
	
}
