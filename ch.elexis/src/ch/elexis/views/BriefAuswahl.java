/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle   - added closePreExistingViewToEnsureOfficeCanHandleNewContentProperly and TerminateListener via Bootstrap.java;  
 *    J. Sigle - added stress test feature to aid stabilizing NOAText_jsl and/or other text plugins.
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

/**
 * TODO: 20131027js: I noticed that before - but: Please review naming conventions: Briefauswahl.java (document selection/controller dialog) and TextView.java (document display/editor), vs. RezepteView.java (selection/controller) and RezeptBlatt.java (display/editor), etc. for Bestellung, AUFZeugnis and maybe more similar combinations. This inconsistency is highly confusing if you want to do updates throughout all external document processing plugins/classes/etc. 
 */

package ch.elexis.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyEvent;		//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.events.KeyListener;	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPropertyListener;	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately store
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.dialogs.DocumentSelectDialog;
import ch.elexis.dialogs.SelectFallDialog;
import ch.elexis.preferences.PreferenceConstants;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.CommonViewer.DoubleClickListener;
import ch.elexis.util.viewers.DefaultContentProvider;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;

public class BriefAuswahl extends ViewPart implements ElexisEventListener, IActivationListener,
		ISaveablePart2 {
	
	public final static String ID = "ch.elexis.BriefAuswahlView"; //$NON-NLS-1$
	private final FormToolkit tk;
	private Form form;
	private Action briefNeuAction, briefLadenAction, editNameAction;
	private Action deleteAction;
	private Action stressTest1Action, stressTest2Action;	//20140421js: added stress test feature.
	private ViewMenus menus;
	private ArrayList<sPage> pages = new ArrayList<sPage>();
	public CTabFolder ctab;	//20130414js: use temporary meaningful filenames: made this public to access it from NOAText.java; before that: simply ctab, w/o any visibility designator
	
	// private ViewMenus menu;
	// private IAction delBriefAction;
	public BriefAuswahl(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: BriefAuswahl.BriefAuswahl() - about to tk = Desk.getToolkit()...");
		tk = Desk.getToolkit();
		if ( tk == null ) 	{	System.out.println("js ch.elexis.views/BriefAuswahl.java: BriefAuswahl.BriefAuswahl() - result: WARNING: tk == null"); }
		else 				{	System.out.println("js ch.elexis.views/BriefAuswahl.java: BriefAuswahl.BriefAuswahl() - result: tk == "+tk.toString()); }
	}
	
	/*
	 * 20131026js: First check if a View for documents of the same type (e.g. "Briefe") is already open.
	 * If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
	 * 
	 * This was needed to enable the user to directly and successfully load a new document,
	 * even when a view with a document of the similar type was already open,
	 * without closing the view of the already open document before,
	 * after corrections of noas / OpenOffice createMe()/removeMe() related task loading/unloading handling.
	 * Elexis 20130605js would not correctly keep track of OpenOffice usage and not unload soffice.bin/soffice.exe
	 *   when no more documents were using them, or Elexis was closed - and Office usage would become instable over time.
	 * Elexis 20130627js or so would have corrected track keeping, and office unloading, and reloading/reconnection,
	 *   but not be usable - at least because when a new office document was loaded while another one of the same type
	 *   was still open, the old connection/bridge to OpenOffice would be dropped,
	 *   and the ongoing load attempt would throw a ...disposed or bridge disposed or similar Exception,
	 *   and the resulting view would have an empty white space in its payload area, and soffice.bin/soffice.exe not be loaded.
	 *   This could ONLY be avoided, if the user CLOSED the existing document with the [x] close button of the view first,
	 *   and only afterwards loaded the new document. An empty Brief View, opened through the "OpenView" button,
	 *   would in contrast be perfectly unproblematic. After loading Elexis, without any previously open Briefe View,
	 *   no problems would appear either; and even after a problematic situation, just closing the view via [x] close,
	 *   and loading the document through Briefauswahl again, would also fix the problem.
	 *   It appears that the problem actually comes from OpenOffice - some other user of Office reported similar behaviour
	 *   on the WWW - namely, that the connection/bridge to OpenOffice would be lost when one loaded one document
	 *   while another was still loaded (even though doc.close() or similar was used before; there is no doc.dispose()
	 *   and no doc=null, or any other attempts, would achieve the same necessary clearance of the internal state of Office.)  
	 * Elexis 20131016js now, with this hideView approach:
	 *   You can now load documents directly even when others are still open - Elexis will call hideView(),
	 *   this will provide a sufficiently clean environment so that Office has NO TROUBLES loading the next document
	 *   (in the next instance of Office, after noas.isempty / office.deactivate / terminate / disconnect tested elsewhere).
	 *   Now, this "achievement" needs to be propagated through multiple places where document loading/creation can be
	 *   triggered in BriefAuswahl/TextView; AUFZeugnis, Bestellblatt, Rezetpt... etc. We'll see.
	 *   As this method does not use any local variable stuff, I moved it here to simplify propagation.
	 * 
	 * More details in comments inside the method, an around previous unsuccessful alternative approaches
	 * throughout BriefAuswahl.java, TextView.java, NOAText.java, OfficePanel.java, BootStrap... etc.
	 */			
	private void  closePreExistingViewToEnsureOfficeCanHandleNewContentProperly() {
	//20131026js: First check if a View "Briefe" is already open.
	//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
	//We could check this via TextView tv = (TextView) getViewSite().getPage().findView(TextView.ID);
	//if (tv != null)...
	//But it's more convenient and simpler to just check wbpTextViewView != null.
	//NOW, ONCE AGAIN... exactly like in previous attempts via other ways,
	//this will NOT close the View Briefe Window, but only the stuff contained therein.
	//http://www.eclipsezone.com/eclipse/forums/t71597.html
	
	//201310261531js: YES, this code at this position finally does it:
	//Although the visible representation of the open, populated "View Briefe" remains still visible all the time,
	//instead of disappearing completely (as if I used the close button, or hideView() from inside TextView.java),
	//the view payload area becomes empty, AND NEW SUCCESS OF USING hideView(): with it's Title now changes to "Kein Brief geladen".
	//This IS sufficient to ensure that the next document is properly loaded AND displayed.
	//And compatible with pre-existing approaches to prepare a working OO environment to load/edit the document within,
	//even though soffice.bin and soffice.exe can be (and have probably already been) unloaded from memory
	//after the preceeding doc has been closed. 
	
	//Import org.eclipse.ui.IWorkbenchPage and ...PlatformUI and ...IViewPart only for this:
	System.out.println("\n");
	System.out.println("js ch.elexis.views/BriefAuswahl.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): begin");

	/* 20131122js:
	 * This would only close a window with TextView.ID in the currently active perspective,
	 * but if a window with the same ID would be open (even if it were empty) in another perspective,
	 * that would cause the attempt to close the window to fail when about the 2nd document would be opened of that document type.
	 * So I will try instead to locate and close *all* matching ViewParts in all Perspektives.
	 * See below.
	IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	IViewPart wbpTextViewViewPart = wbp.findView(TextView.ID); 
	if (wbpTextViewViewPart != null) {
		System.out.println("js ch.elexis.views/BriefAuswahl.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): About to wbp.hideView(wbpTextViewViewPart)");
		wbp.hideView(wbpTextViewViewPart); 
		System.out.println("js ch.elexis.views/BriefAuswahl.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): Returned from wbp.hideView(wbpTextViewViewPart)");
	} else {
		//No preexisting populated viewPart (="View Briefe" in Elexis manual terminology) needed closing. Do nothing.
		System.out.println("js ch.elexis.views/BriefAuswahl.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): NO matching wbpTextViewViewPart found - Nothing to do.");
	} //if wbpTextViewView != null ; oder if tv!=null
	*/
	
	//20131122js: Let me try instead to close ALL Windows in ALL Perspectives with TextView.ID
	//Bisher findet das ganze aber immer nur 1 workbenchWindow mit 1 workbenchPage mit 1 viewPart.
	//Also keine Änderung im Effekt gegenüber dem obigen Vorgehen.
	//Und wie bekomme ich Zugang zum Inhalt unsichtbarer perspectives? Wie ist die Objekthierarchie aufgebaut?
	//Siehe: http://www.eclipse.org/articles/using-perspectives/PerspectiveArticle.html
	//Daraus unter anderem:
	//The user calls it a "perspective". At the implementation level it is an IWorkbenchPage.
	//(js: Not even that. But rather the visible representation of an instantiation of an IWorkbenchPage, I guess.)
	//(js: Und warum dann sowas wie: workbenchPage.getPerspective()??? Wie schon gesagt: Das Maximum an Intransparenz und Obscurity.)
	//(js: Und warum liefert dann workbenchWindow.getPages() (siehe unten) immer nur eine Page???)
	//Oh Klasse:
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=102268
	//------------------------------------------------------
	//Question:
	//------------------------------------------------------
	//i can find a view from an active perspective by using:
	//...(about the same stuff that I tried to use, js)...
	//i can't find a view opened in an inactive perspective.
	//Is there anyway to do this?
	//------------------------------------------------------
	//Answer:
	//------------------------------------------------------
	//Since Eclipse 2.1, there is only (at most) one page per window, and the page may
	//contain multiple perspectives.  There is currently no way of querying the
	//contents of inactive perspectives.  Can you provide more details of your
	//scenario and why it requires this?
	//------------------------------------------------------
	//Other Comment:
	//------------------------------------------------------
	//I want close particular views (multiple) in all perspective after closing database connection.
	//It looks impossible in legacy eclipse.I can remember all views, but can't hide view in inactive perspective.
	//------------------------------------------------------
	//Also points to: (Where they wanted to do in 2004 what I want to do now in 2013, for quite the same reason:
	//                 close/remove plugin related stuff in inactive perspectives; and it says there is a "close view" related bug.)
	//https://bugs.eclipse.org/bugs/show_bug.cgi?id=57841
	
	System.out.println("js ch.elexis.views/BriefAuswahl.java: about to find and close ALL ViewParts with TextView.ID");

	IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
	//PlatformUI.getWorkbench().getWorkingSetManager().???	
	
	System.out.println("js ch.elexis.views/BriefAuswahl.java: workbenchWindows.length = "+workbenchWindows.length);
	
	if (workbenchWindows != null) { 
		for (IWorkbenchWindow workbenchWindow : workbenchWindows ) {
			System.out.println("js ch.elexis.views/BriefAuswahl.java: about to process workbenchWindow: "+workbenchWindow.toString());

			IWorkbenchPage activeWorkbenchPage = workbenchWindow.getActivePage();
			if (activeWorkbenchPage == null)	{System.out.println("js ch.elexis.views/BriefAuswahl.java: Info: activeWorkbenchPage = null"); }
			else								{System.out.println("js ch.elexis.views/BriefAuswahl.java: Info: activeWorkbenchPage = "+activeWorkbenchPage.toString()); }						
				
			IWorkbenchPage[] workbenchPages = workbenchWindow.getPages();
			System.out.println("js ch.elexis.views/BriefAuswahl.java: workbenchPages.length = "+workbenchPages.length);
			
			if (workbenchPages != null) {
				for (IWorkbenchPage workbenchPage : workbenchPages) {
					System.out.println("js ch.elexis.views/BriefAuswahl.java: about to process workbenchPage: "+workbenchPage.toString());
					
					IPerspectiveDescriptor perspective = workbenchPage.getPerspective();
					String perspectiveLabel = perspective.getLabel(); 
					System.out.println("js ch.elexis.views/BriefAuswahl.java: workbenchPage.getperspective().getLabel() is: "+perspectiveLabel);
					
					IAdaptable input = workbenchPage.getInput();
					System.out.println("js ch.elexis.views/BriefAuswahl.java: workbenchPage.getInput() is: "+input.toString());
					
					IViewPart workbenchPageViewPart = workbenchPage.findView(TextView.ID);
					if (workbenchPageViewPart != null) {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: about to close ViewPart: "+workbenchPageViewPart.toString());
						workbenchPage.hideView(workbenchPageViewPart);
					}				
				}
			}
		}
	}
	
	
	
	System.out.println("js ch.elexis.views/BriefAuswahl.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): end");
	System.out.println("\n");
	}
	
	@Override
	public void createPartControl(final Composite parent){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: createPartControl() begin");

		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("BriefAuswahlAllLetters")).append(Brief.UNKNOWN).append(",").append(Brief.AUZ) //$NON-NLS-1$
			.append(",").append(Brief.RP).append(",").append(Brief.LABOR);
		String cats = Hub.globalCfg.get(PreferenceConstants.DOC_CATEGORY, sb.toString());
		parent.setLayout(new GridLayout());
		
		form = tk.createForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		form.setBackground(parent.getBackground());
		
		/*
		 * An dieser Stelle sehe ich keinen Effekt eines KeyListeners - er bekommt wohl die events nicht.
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
		System.out.println("\njs ch.elexis.views/BriefAuswahl.java createPartControl(): about to textContainer.addKeyListener()...");
		form.addKeyListener(new KeyListener() {
			@Override
		    public void keyPressed(KeyEvent e) {
		        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().KeyListener(): " + e.keyCode + " pressed");
		    }
			@Override
		    public void keyReleased(KeyEvent e) {
		        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().KeyListener(): " + e.keyCode + " released");
		    }
			@Override
		    public void keyTyped(KeyEvent e) {
		        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().KeyListener(): " + e.character + " typed");
		    }
		});
		
		//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored (end).
		 */
		
		
		// Grid layout with zero margins
		GridLayout slimLayout = new GridLayout();
		slimLayout.marginHeight = 0;
		slimLayout.marginWidth = 0;
		
		Composite body = form.getBody();
		body.setLayout(slimLayout);
		body.setBackground(parent.getBackground());
		
		ctab = new CTabFolder(body, SWT.BOTTOM);
		ctab.setLayout(slimLayout);
		ctab.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ctab.setBackground(parent.getBackground());
		makeActions();
		menus = new ViewMenus(getViewSite());
		
		for (String cat : cats.split(",")) {
			CTabItem ct = new CTabItem(ctab, SWT.NONE);
			ct.setText(cat);
			sPage page = new sPage(ctab, cat);
			pages.add(page);
			menus.createViewerContextMenu(page.cv.getViewerWidget(), editNameAction, deleteAction);
			ct.setData(page.cv);
			ct.setControl(page);
			page.cv.addDoubleClickListener(new DoubleClickListener() {
				@Override
				public void doubleClicked(PersistentObject obj, CommonViewer cv){
					briefLadenAction.run();
				}
			});
		}
		
		ctab.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e){
				relabel();
			}
			
		});
		
		GlobalEventDispatcher.addActivationListener(this, this);
		menus.createMenu(briefNeuAction, briefLadenAction, editNameAction, deleteAction, stressTest1Action, stressTest2Action);		//20140421js: added stress test feature.
		menus.createToolbar(briefNeuAction, briefLadenAction, deleteAction);
		ctab.setSelection(0);
		relabel();

		System.out.println("js ch.elexis.views/BriefAuswahl.java: createPartControl() end");
	}
	
	@Override
	public void dispose(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: dispose begin");
		ElexisEventDispatcher.getInstance().removeListeners(this);
		GlobalEventDispatcher.removeActivationListener(this, this);
		
		for (sPage page : pages) {
			page.getCommonViewer().getConfigurer().getContentProvider().stopListening();
		}
		System.out.println("js ch.elexis.views/BriefAuswahl.java: dispose end");
	}
	
	@Override
	public void setFocus(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: setFocus() begin nop end");		
	}
	
	public void relabel(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: relabel begin");

		Desk.asyncExec(new Runnable() {
			public void run(){
				Patient pat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
				if (pat == null) {
					form.setText(Messages.getString("BriefAuswahlNoPatientSelected")); //$NON-NLS-1$
				} else {
					form.setText(pat.getLabel());
					CTabItem sel = ctab.getSelection();
					if (sel != null) {
						CommonViewer cv = (CommonViewer) sel.getData();
						cv.notify(CommonViewer.Message.update);
					}
				}
			}
		});

		System.out.println("js ch.elexis.views/BriefAuswahl.java: relabel end");
	}
	
	class sPage extends Composite {
		private final CommonViewer cv;
		private final ViewerConfigurer vc;
		
		public CommonViewer getCommonViewer(){
			System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.getCommonViewer() begin about to return cv");
			return cv;
		}
		
		
		/*
		 * 201306170128js: Attempt to add missing documentation - may be right, may be wrong.
		 * This method probably constructs the list of selectable documents in the Briefauswahl view.
		 * Depending on which tab ("Allg", "Rp", "AU"...) is active, a different subset of all
		 * available documents of the current patient may be displayed.
		 * 
		 * Additionally, a button is displayed to load a selected document
		 * (This should cause the same actions to happen as a newer corresponding double click action does.)
		 */
		sPage(final Composite parent, final String cat){
			super(parent, SWT.NONE);
				
			System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() begin POST super(parent, SWT.NONE)");
			setLayout(new GridLayout());
			cv = new CommonViewer();
			vc = new ViewerConfigurer(new DefaultContentProvider(cv, Brief.class, new String[] {
					Brief.FLD_DATE
				}, true) {
					
					@Override
					public Object[] getElements(final Object inputElement){
						Patient actPat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
						if (actPat != null) {
							Query<Brief> qbe = new Query<Brief>(Brief.class);
							qbe.add(Brief.FLD_PATIENT_ID, Query.EQUALS, actPat.getId());
							if (cat.equals(Messages.getString("BriefAuswahlAllLetters2"))) { //$NON-NLS-1$
								qbe.add(Brief.FLD_TYPE, Query.NOT_EQUAL, Brief.TEMPLATE);
							} else {
								qbe.add(Brief.FLD_TYPE, Query.EQUALS, cat);
							}
							cv.getConfigurer().getControlFieldProvider().setQuery(qbe);
							
							List<Brief> list = qbe.execute();
							return list.toArray();
						} else {
							return new Brief[0];
						}
					}
					
				}, new DefaultLabelProvider(), new DefaultControlFieldProvider(cv, new String[] {
					"Betreff=Titel"
				}), new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
					SimpleWidgetProvider.TYPE_LIST, SWT.V_SCROLL, cv));
			cv.create(vc, this, SWT.NONE, getViewSite());
			cv.getViewerWidget().setComparator(new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2){
					if (e1 instanceof Brief && e2 instanceof Brief) {
						TimeTool bt1 = new TimeTool(((Brief) e1).getDatum());
						TimeTool bt2 = new TimeTool(((Brief) e2).getDatum());
						return bt2.compareTo(bt1);
					}
					System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() about to return 0 early");
					return 0;
				}
			});
			vc.getContentProvider().startListening();
	
			Button bLoad =	tk.createButton(this, Messages.getString("BriefAuswahlLoadButtonText"), SWT.PUSH); //$NON-NLS-1$
			bLoad.addSelectionListener(new SelectionAdapter() {
				@Override
				//ToDo: 201306170133js: This might be refactored into a separate method
				//to ensure that clicking on the "bLoad" button does quite the same stuff
				//as a double click on an entry of the document list - "briefLadenAction". 
				public void widgetSelected(final SelectionEvent e){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() (Via Button 'Laden') begin");

					//20131026js: First check if a View for documents of the same type is already open.
					//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
					closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
					
					try {					
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);

						Object[] o = cv.getSelection();
						if ((o != null) && (o.length > 0)) {
							Brief brief = (Brief) o[0];
							System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() about to tv.openDocument(brief) (Via Button 'Laden')...");
							if (tv.openDocument(brief) == false) {
								SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
									Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
							}
						} else {
							System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() about to tv.createDocument() (Via Button 'Laden')...");
							tv.createDocument(null, null);
						}
						
					} catch (Throwable ex) {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() WARNING: Caught Exception!");
						ExHandler.handle(ex);
					}
					System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() end");
				}
				
			});
			bLoad.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			
			System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage() sPage.sPage() end");
		}
	} // class sPage...
	
	private void makeActions(){	
		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() begin");

		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction = new Action...");
		briefNeuAction = new Action(Messages.getString("BriefAuswahlNewButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() begin");

					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to getSelectedPatient()...");
					Patient pat = ElexisEventDispatcher.getSelectedPatient();
					if (pat == null) {
						MessageDialog.openInformation(Desk.getTopShell(),
							Messages.getString("BriefAuswahlNoPatientSelected"),
							Messages.getString("BriefAuswahlNoPatientSelected"));
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() pat == null about to return early");
						return;
					}
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to getSelected(Fall.class)...");
					Fall selectedFall = (Fall) ElexisEventDispatcher.getSelected(Fall.class);
					if (selectedFall == null) {
						SelectFallDialog sfd = new SelectFallDialog(Desk.getTopShell());
						sfd.open();
						if (sfd.result != null) {
							ElexisEventDispatcher.fireSelectionEvent(sfd.result);
						} else {
							MessageDialog
								.openInformation(Desk.getTopShell(), Messages
									.getString("TextView.NoCaseSelected"), //$NON-NLS-1$
									Messages
										.getString("TextView.SaveNotPossibleNoCaseAndKonsSelected")); //$NON-NLS-1$
							System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() SelectFallDialog sfd.open() sfd.result == null about to return early");
							return;
						}
					}
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to getSelected(Konsultation.class)...");
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

					//20131026js: First check if a View for documents of the same type is already open.
					//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
					closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
					
					try {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to instantiate new TextView tv...");
						TextView tv = (TextView) getSite().getPage().showView(TextView.ID /*
																				 * ,StringTool.unique
																				 * ("textView")
																				 * ,IWorkbenchPage
																				 * .VIEW_ACTIVATE
																				 */);
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to instantiate new DocumentSelectDialog bs...");
						DocumentSelectDialog bs =
							new DocumentSelectDialog(getViewSite().getShell(), Hub.actMandant,
								DocumentSelectDialog.TYPE_CREATE_DOC_WITH_TEMPLATE);
						if (bs.open() == Dialog.OK) {
							// trick: just supply a dummy address for creating the doc
							Kontakt address = null;
							if (DocumentSelectDialog.getDontAskForAddresseeForThisTemplate(bs.getSelectedDocument()))
								address = Kontakt.load("-1");
							System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to tv.createDocument(bs.getSelectedDocument(), bs.getBetreff(), address);...");
							tv.createDocument(bs.getSelectedDocument(), bs.getBetreff(), address);
							tv.setName();
							CTabItem sel = ctab.getSelection();
							if (sel != null) {
								CommonViewer cv = (CommonViewer) sel.getData();
								cv.notify(CommonViewer.Message.update_keeplabels);
							}
							
						}
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() end");
				}
			};

			System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction = new Action...");
			briefLadenAction = new Action(Messages.getString("BriefAuswahlOpenButtonText")) { //$NON-NLS-1$
				@Override
				//ToDo: 201306170133js: This might be refactored into a separate method
				//to ensure that clicking on the "bLoad" button does quite the same stuff
				//as a double click on an entry of the document list - "briefLadenAction". 
				public void run(){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() begin (via DblClick or Menu)");
					
					/*
					 * This will also completely mess up a even the first (!) attempt to open a document.
					 
					//20131011js:
					//Theoretically, opening the next document should automatically smoothly replace a possibly open document in an OpenOffice TextView = View Briefe window.
					//In reality - I can either have the functionality keeping track of opened docs NOT WORKING, so soffice.bin and soffice.exe are NEVER terminated.
					//OR, I can have fixed this - but thereupon, I must either manually close the TextView Window before opening the next document -
					//or get into trouble (of not precisely known mechanics, though I tried to track that down and avoid the problems literally for days by now!!!),
					//when the last OO is closed - at some time during the load process for the next one, but NOT clearly before anything else starts.
					//SO, I will now take care of closing the old document CLEARLY AND COMPLETELY BEFORE opening a new one, myself. Full stop.
					//For testing: Open Elexis - Open a doc in briefauswahl - close it - open another doc in briefauswahl - close it - close Elexis: (Works without this taking care)
					//In my Elexis 20131011js ff: When no doc open, soffice.bin and soffice.exe should NOT be in task manager, and when openig a doc, it should start, and doc should appear in editable way.
					//In my Elexis e.g. 20130605 and before: soffice.bin, soffice.exe would not be terminated at any time,
					//because NOAText removeMe(), noa.remove() would never be called, because doc.closeLisener() would never be called, for whatever reason.
					//Testing 2: Open Elexis - Open a doc in briefauswahl, directly open another doc in briefauswahl.
					//In Elexis 20131011js ff: with the fix for removeMe() - that would now cause a problem. Depending upon details elsewhere,
					//chaos would either cause the result to appear white in the pre-existing frame, without started office at all;
					//or this + office doc would appear in separate Windows plus additional dialogs, or whatever etc. pp.
					//What should happen now with this additional care-taking-fix: Old document should close and office terminate (if that was last client). New doc should open in new frame where the old one had been.
					//This would add stability in contrast to leaving oo run forever (and become more and more unreliable, finally requiring a machine restart/killing soffice.bin via taskman etc.).
					System.out.println("js ch.elexis.views/BriefAuswahl.java: -------------------------------------------------------------");
					System.out.println("js ch.elexis.views/BriefAuswahl.java: TODO: ENSURE THAT bload and briefLadenAction DO THE SAME HERE - close DocumentCompletelyIncludingFrame before opening the next one.");
					System.out.println("js ch.elexis.views/BriefAuswahl.java: -------------------------------------------------------------");
					try {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: If an old TextView exists, then let's dispose of it.");
						TextView tvOld = (TextView) getViewSite().getPage().showView(TextView.ID);
						if (tvOld != null)	tvOld.dispose();
						//This does the same:
						//getViewSite().getPage().showView(TextView.ID).dispose();
						
						//Das macht dann gleich mal das GANZE Elexis zu - getWorkbenchWindow(), no matter from where, liefert offenbar immer das ganze.
						//getViewSite().getPage().showView(TextView.ID).getViewSite().getWorkbenchWindow().close();
				        				
					} catch (Exception ex) {
						//Nop. Most probably: Just nothing to dispose of; i.e. we're directly after program start,
						//no View Briefe = TextView.java Window open yet, or manually closed again before the next one is being opened here. 
					}
					*/
					
					
					//20131026js: First check if a View for documents of the same type is already open.
					//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
					closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
					
					//20131026js: Mal dieses hier von oben bei briefNeuAction übernommen... vielleicht merkt Java, dass ich ein ggf. schon existierendes
					//altes tv loswerden möchte und entsorgt es komplett korrekt???
					//Dürfte aber keinen Unterschied machen, da tv nach lokaler Variable ausschaut...
					System.out.println("js ch.elexis.views/BriefAuswahl.java: about to TextView tv=null;");
					TextView tv = null;
					
					try {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: try tv = (TextView) getViewSite().getPage().showView(TextView.ID) (via DblCklick or Menu)...");
						tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						
						//20131026js: Wenn ich hier so etwas bringe, um ein neues Fenster zu erreichen, dann endet das in einem grauen Fensterinhalt,
						//weil beim Dispose sowohl frame als auch irgendwelche Plugin-Strukturen/Zustände entsorgt werden, und nicht automatisch
						//wiederhergestellt.
						//Leider leider kommt das Plugin aber auch selbst nicht mit einem openDocument() zurecht,
						//wenn zuvor schon ein Dokument offen ist. Also, ich kann seinen Zustand hier nicht von aussen zurücksetzen,
						//auf denjenigen nach dem Startup oder nach Frame close via [x]; woraufhin nämlich ein Laden komplett korrekt funktioniert -
						//und es kann sich selbst offenbar auch nicht in diesen Zustand (zurück)bringen, bevor es mit dem Ausführen des
						//Ladebefehls beginnt, wenn es schon etwas geladen hat. Tolle Objektorientierte und wunderschön gekapselte Welt.
						//
						//if (tv.TextContainer != null) tv.TextContainer.dispose();
						//if (tv.txt != null) tv.txt.dispose();
						//
						//System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: try tv dispose()");
						//tv.txt.dispose();
						//System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: try tv = (TextView) getViewSite().getPage().showView(TextView.ID)");
						//tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: CTabItem sel = ctab.getSelection();...");

						CTabItem sel = ctab.getSelection();

						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: if (sel != null)...");
						//Check for a selection and process it if applicable.
						if (sel != null) {
							System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() (sel != null). -> about to: CommonViewer cv = (ComonViewer) sel.getData()...");
							CommonViewer cv = (CommonViewer) sel.getData();
							Object[] o = cv.getSelection();
							if ((o != null) && (o.length > 0)) {
								Brief brief = (Brief) o[0];
								
								System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to tv.openDocument(brief) (via DblClick or Menu)...");
								if (tv.openDocument(brief) == false) {
									System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() WARNING: tv.openDocument(brief) failed.");
									SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
										Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
								} else {
									System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() OK: tv.openDocument(brief) returned true.");

									/*
									 * An dieser Stelle sehe ich keinen Effekt der ausprobierten Listeners.
									 * Ich probiere es in TextView, BriefAuswahl...
									
									//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored (begin).

									System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to tv.addPropertyListener()...");
									tv.addPropertyListener(new IPropertyListener() {

										@Override
										public void propertyChanged(Object source, int propId) {
											System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() tv.addPropertyListener() begin nop end");
											
										}
											
									});

									System.out.println("\njs ch.elexis.views/BriefAuswahl.java createPartControl(): about to tv.textContainer.addKeyListener()...");
									tv.textContainer.addKeyListener(new KeyListener() {
										@Override
									    public void keyPressed(KeyEvent e) {
									        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().KeyListener(): " + e.keyCode + " pressed");
									    }
										@Override
									    public void keyReleased(KeyEvent e) {
									        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().KeyListener(): " + e.keyCode + " released");
									    }
									});
									
									System.out.println("\njs ch.elexis.views/BriefAuswahl.java createPartControl(): about to tv.textContainer.addMouseListener()...");
									tv.textContainer.addMouseListener(new MouseListener() {
										@Override
										public void mouseDoubleClick(MouseEvent e) {
									        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().MouseListener(): " + e.button + " DoubleClick");					
										}
										@Override
										public void mouseDown(MouseEvent e) {
									        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().MouseListener(): " + e.button + " Down");
										}
										@Override
										public void mouseUp(MouseEvent e) {
									        System.out.println("js ch.elexis.views/BriefAuswahl.java createPartControl().MouseListener(): " + e.button + " Up");
										}
									});
									*/
									//201306150113js - ensure edits in text documents are noted by Elexis and ultimately stored (end).

								
								}
															
							} else {
								System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to tv.createDocument(null,null)...");
								tv.createDocument(null, null);
							}
							System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to cv.notify(CommonViewer.Message.update)...");
							cv.notify(CommonViewer.Message.update);
							
						}
					} catch (PartInitException e) {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() WARNING: caught PartInitException!");
						ExHandler.handle(e);
					}
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() end");
				}
			};
		
			//20140421js: added stress test feature.
			System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() stressTest1Action = new Action...");
			stressTest1Action= new Action(Messages.getString("BriefAuswahlStressTestButtonText1")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println();
					System.out.println("****************************************************************");
					System.out.println("js ch.elexis.views/BriefAuswahl.java: Initiating stress test 1.");
					System.out.println("****************************************************************");
					System.out.println();
					System.out.println("This stress test will open the selected document repeatedly until you close the program or an error occurs.");
					System.out.println();
					Integer stressTestPasses=0;
					Boolean continueStressTest=true;
					while (continueStressTest) {
					
					stressTestPasses=stressTestPasses+1;
					
					System.out.println("stress test pass: "+stressTestPasses+" - about to closePreExistingView...");
					//20131026js: First check if a View for documents of the same type is already open.
					//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
					closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
					
					System.out.println("stress test pass: "+stressTestPasses+" - about to load document...");
					try {
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						CTabItem sel = ctab.getSelection();
						if (sel != null) {
							System.out.println("stress test pass: "+stressTestPasses+" - sel != null; sel.getText()=<"+sel.getText().toString()+">");
							CommonViewer cv = (CommonViewer) sel.getData();
							Object[] o = cv.getSelection();
							if ((o != null) && (o.length > 0)) {
								Brief brief = (Brief) o[0];
								System.out.println("stress test pass: "+stressTestPasses+" - o !!= null; (Brief) o[0.getLabel()]=<"+brief.getLabel().toString()+">");
								System.out.println("stress test pass: "+stressTestPasses+" - try {} section o != null; about to tv.openDocument(brief)....");
								
								//20131026js: Der Name wird nicht in das Dokumenten-Editor-Tab übernommen.
								//Ursache gerade unbekannt - nach dem letzten geladenen Dokument erscheint er dann.
								//Auch ein Setzen im Stresstest nach jedem Laden via tv.setName(); reicht nicht, um das früher zu aktualisieren.
								//Vielleicht wird - Grund ebenfalls unbekannt - tv...actBrief <- pat nicht rechtzeitig aktualisiert, welches setName() auswertet.  
								if (tv.openDocument(brief) == false) {
									System.out.println("stress test pass: "+stressTestPasses+" - try {} section tv.openDocument(brief) returned false. Setting continueStressTest=false.");
									continueStressTest=false;
									SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
										Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
								}
								else {									
									System.out.println("stress test pass: "+stressTestPasses+" - try {} section tv.openDocument(brief) worked; document should have been loaded.");
								}
							} else {
								System.out.println("stress test pass: "+stressTestPasses+" - try {} section o == null; about to tv.createDocument(null,null). Setting continueStressTest=false.");
								continueStressTest=false;
								tv.createDocument(null, null);
							}
							System.out.println("stress test pass: "+stressTestPasses+" - try {} section; about to cv.notify(CommonViewer.Message.update);...");
							cv.notify(CommonViewer.Message.update);
							System.out.println("stress test pass: "+stressTestPasses+" - try {} section completed.");
						}
					} catch (PartInitException e) {
						System.out.println("stress test pass: "+stressTestPasses+" - catch {} section handling exception. Setting continueStressTest=false.");
						continueStressTest=false;
						ExHandler.handle(e);
						System.out.println("stress test pass: "+stressTestPasses+" - catch {} section completed.");
					}
					System.out.println("stress test pass: "+stressTestPasses+" - try/catch completed.");

					if (stressTestPasses>10) {
						System.out.println("stress test pass: "+stressTestPasses+" - Setting continueStressTest=false after "+stressTestPasses+" passes have completed.");						
						continueStressTest=false;
					}

					try {
						System.out.println("stress test pass: "+stressTestPasses+" - about to Thread.sleep()...(Otherwise the Briefe view content would not be visibly updated.)");
						//20131026js: In Elexis 20131026js nach Korrekturen und der Einführung des hideView() vor dem nächsten Laden
						//reicht (auf think3; i7) auch Thread.sleep(100) um ein vollständiges sichtbares Update des Editor-Inhalts nach jedem Laden zu erreichen.
						//Ein Thread.sleep(10) ist aber zu wenig; da erscheinen dann nur noch die OO Bedienelemente zuverlässig. 
						//Nicht nur wie vorher, nach etwa den ersten 2..4 Stresstest-Zyklen.
						//Nur der Titel des Tabs (via tv.setName() ) wird nicht aktualisiert, mögliche Gründe weiter oben genannte.
						//Thread.sleep(10);
						//Thread.sleep(100);
						Thread.sleep(500);
						//Thread.sleep(1000);
					} catch (Throwable throwable) {
						//handle the interrupt that will happen after the sleep 
						System.out.println("stress test pass: "+stressTestPasses+" - caught throwable; most probably the Thread.sleep() wakeup interrupt signal.");
					}
					
					System.out.println("****************************************************************");				
				
				}	//while true for stress test js
				System.out.println("stress test pass: "+stressTestPasses+" - stress test ends.");
					
				}
			};

			//20140421js: added stress test feature.
			System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() stressTest2Action = new Action...");
			stressTest2Action = new Action(Messages.getString("BriefAuswahlStressTestButtonText2")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println();
					System.out.println("****************************************************************");
					System.out.println("js ch.elexis.views/BriefAuswahl.java: Initiating stress test 2.");
					System.out.println("****************************************************************");
					System.out.println();
					System.out.println("This stress test will open all Briefe of the selected patient one after another, repeatedly, until you close the program or an error occurs.");
					System.out.println();
					
					Integer stressTestPasses=0;
					Boolean continueStressTest=true;
					
					//obtain a list of all documents for the current patient
					Patient actPat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
					if (actPat != null) {
						Query<Brief> qbe = new Query<Brief>(Brief.class);
						qbe.add(Brief.FLD_PATIENT_ID, Query.EQUALS, actPat.getId());
						qbe.add(Brief.FLD_TYPE, Query.NOT_EQUAL, Brief.TEMPLATE);
									
						List<Brief> list = qbe.execute();
						//list.toArray()
						System.out.println("Liste der Briefe des Patienten: "+list);
					
						//das noch hinzugefügt nach erster Fassung, die archiviert wurde...
						while (continueStressTest) {
							
							//open one document after annother; each adds another pass to the stress test pass count
							for (Brief brief : list) {
	
								if ( brief != null ) {
									
									stressTestPasses=stressTestPasses+1;
									
									System.out.println("stress test pass: "+stressTestPasses+" - about to closePreExistingView...");
									
									//20131026js: First check if a View for documents of the same type is already open.
									//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
									closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
								
									System.out.println("stress test pass: "+stressTestPasses+" - about to load document...");
									try {
										TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
	
										System.out.println("stress test pass: "+stressTestPasses+" - o !!= null; (Brief) brief[0.getLabel()]=<"+brief.getLabel().toString()+">");
										System.out.println("stress test pass: "+stressTestPasses+" - try {} section o != null; about to tv.openDocument(brief)....");
																	
										//20131026js: Der Name wird nicht in das Dokumenten-Editor-Tab übernommen.
										//Ursache gerade unbekannt - nach dem letzten geladenen Dokument erscheint er dann.
										//Auch ein Setzen im Stresstest nach jedem Laden via tv.setName(); reicht nicht, um das früher zu aktualisieren.
										//Vielleicht wird - Grund ebenfalls unbekannt - tv...actBrief <- pat nicht rechtzeitig aktualisiert, welches setName() auswertet.  
										if (tv.openDocument(brief) == false) {
											System.out.println("stress test pass: "+stressTestPasses+" - try {} section tv.openDocument(brief) returned false. Setting continueStressTest=false.");
											SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
														Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
											continueStressTest=false;
											break;
										}	else {
											
											//Das ist jedenfalls kontraindiziert: Wirft eine unhandled exception, weil der Thread ja nicht darauf gewartet hat:
											//tv.notify();
											//Die folgenden verbessern nichts am Verhalten: Die ersten wenigen Dokumente  werden aktualisiert angezeigt, danach keines ausser dem letzten:
											//tv.txt.setFocus();
											
											//tv.textContainer.update();
											
											//tv.textContainer.redraw();
	
											//tv.textContainer.update();
											//tv.textContainer.redraw();
											
											//tv.textContainer.redraw();
											//tv.textContainer.update();
											
											/*
											while (tv.getViewSite()==null ) {
												System.out.println("stress test pass: "+stressTestPasses+" - try {} section waiting for view to complete initialization...");
	
												try {
													System.out.println("stress test pass: "+stressTestPasses+" - about to Thread.sleep(10)...");
													Thread.sleep(10);
												} catch (Throwable throwable) {
													//handle the interrupt that will happen after the sleep 
													System.out.println("stress test pass: "+stressTestPasses+" - caught throwable; most probably the Thread.sleep() wakeup interrupt signal.");
												}
											}
											*/
	
											//tv.dispose();

											System.out.println("stress test pass: "+stressTestPasses+" - try {} section tv.openDocument(brief) worked; document should have been loaded.");
											}
									} catch (PartInitException e) {
										System.out.println("stress test pass: "+stressTestPasses+" - catch {} section handling exception. Setting continueStressTest=false.");
										ExHandler.handle(e);
										System.out.println("stress test pass: "+stressTestPasses+" - catch {} section completed.");
										continueStressTest=false;
										break;
									}
									System.out.println("stress test pass: "+stressTestPasses+" - try/catch completed.");
		
									if (stressTestPasses>100) {
										System.out.println("stress test pass: "+stressTestPasses+" - Setting continueStressTest=false after "+stressTestPasses+" passes have completed.");						
										continueStressTest=false;
										break;
									}
									
									
									try {
										System.out.println("stress test pass: "+stressTestPasses+" - about to Thread.sleep()...(Otherwise the Briefe view content would not be visibly updated.)");
										//Nichts von den folgenden hilft tatsächlich gut gegen das mangelnde Updaten im LibreOffice Frame nach dem ca. 4. Dokument:
										//20131026js:
										//Doch, in meiner verbesserten Elexis/NOAText_jsl Version ab 20131026js hilft es dann. Dort erfolgt ein Update der Anzeige nach jedem Laden,
										//allerdings auch (soweit keine anderen OO benutzenden Dokumente offen sind) auch ein Schliessen/Neuverbinden von OO nach jedem Dokument.
										//Und DA macht es dann einen Unterschied: Ohne Thread sleep sieht man nur, dass das Tab+Inhalt neu gezeichnet wird,
										//insbesondere erscheinend die OO Bedienelemente innerhalb des textContainer Bereichs;
										//mit etwas Thread.sleep() reicht es dann auch noch, um den Inhalt des Dokuments darzustellen.
										//Stabil durchlaufen UND OO nach dem Schliessen des letzten Dokuments entladen tut's mit oder ohne Thread.sleep(),
										//allerdings seit Einführung der korrekten createMe() removeMe() noas.isEmpty Trackings mit disconnect bei 0 offenen Docs
										//in der Fassung 20131026js nur mit einem Elexis gleichzeitig.
										//Thread.sleep(100);
										Thread.sleep(500);
										//Thread.sleep(1000);
										//Thread.sleep(10000);
										//Thread.yield();
									} catch (Throwable throwable) {
										//handle the interrupt that will happen after the sleep 
										System.out.println("stress test pass: "+stressTestPasses+" - caught throwable; most probably the Thread.sleep() wakeup interrupt signal.");
									}
									
								
									System.out.println("****************************************************************");
	
								} //if ( brief != null)
								
							} //for ( brief : list )
						} //while (continueStressTest)
					} //if (actPat != null )
				System.out.println("stress test pass: "+stressTestPasses+" - stress test ends.");
					
				}
			};
			
		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() deleteAction = new Action...");
		deleteAction = new Action(Messages.getString("BriefAuswahlDeleteButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() deleteAction run() begin");

					CTabItem sel = ctab.getSelection();
					if ((sel != null)
						&& SWTHelper.askYesNo(
							Messages.getString("BriefAuswahlDeleteConfirmHeading"), //$NON-NLS-1$
							Messages.getString("BriefAuswahlDeleteConfirmText"))) { //$NON-NLS-1$
						CommonViewer cv = (CommonViewer) sel.getData();
						Object[] o = cv.getSelection();
						if ((o != null) && (o.length > 0)) {
							Brief brief = (Brief) o[0];

							//20131026js: While we're at it: hideView() if it contains the document we're about to delete.
							System.out.println("js ch.elexis.views/BriefAuswahl.java: TODO: ------------------------------------------------------------------------------------------------------------");
							System.out.println("js ch.elexis.views/BriefAuswahl.java: TODO: makeActions() deleteAction: About to delete the currently edited document; so closing the editor beforehand.");
							//if ... Brief = actBrief (oder ähnlich, wo auch immer es die Info gibt) then closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();
							System.out.println("js ch.elexis.views/BriefAuswahl.java: TODO: ------------------------------------------------------------------------------------------------------------");
									
							brief.delete();
						}
						cv.notify(CommonViewer.Message.update);
					}
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() deleteAction run() end");
				}
			};
			
		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() editNameAction = new Action...");
		editNameAction = new Action(Messages.getString("BriefAuswahlRenameButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() editNameAction run() end");

					CTabItem sel = ctab.getSelection();
					if (sel != null) {
						CommonViewer cv = (CommonViewer) sel.getData();
						Object[] o = cv.getSelection();
						if ((o != null) && (o.length > 0)) {
							Brief brief = (Brief) o[0];
							InputDialog id =
								new InputDialog(getViewSite().getShell(),
									Messages.getString("BriefAuswahlNewSubjectHeading"), //$NON-NLS-1$
									Messages.getString("BriefAuswahlNewSubjectText"), //$NON-NLS-1$
									brief.getBetreff(), null);
							if (id.open() == Dialog.OK) {
								brief.setBetreff(id.getValue());
							}
						}
						cv.notify(CommonViewer.Message.update);
					}

					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() editNameAction run() end");
				}
			};
		/*
		 * importAction=new Action("Importieren..."){ public void run(){
		 * 
		 * } };
		 */
		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions(): about to setImageDescriptor() and setToolTipText() multiple times...");
		briefLadenAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document_text.png")); //$NON-NLS-1$
		briefLadenAction.setToolTipText(Messages.getString("BriefAuswahlOpenLetterForEdit")); //$NON-NLS-1$
		briefNeuAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__plus.png")); //$NON-NLS-1$
		briefNeuAction.setToolTipText(Messages.getString("BriefAuswahlCreateNewDocument")); //$NON-NLS-1$
		editNameAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__pencil.png")); //$NON-NLS-1$
		editNameAction.setToolTipText(Messages.getString("BriefAuswahlRenameDocument")); //$NON-NLS-1$
		deleteAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__minus.png")); //$NON-NLS-1$
		deleteAction.setToolTipText(Messages.getString("BriefAuswahlDeleteDocument")); //$NON-NLS-1$

		System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions(): end");
	}
	
	public void activation(final boolean mode){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: activation() begin nop end");
		// TODO Auto-generated method stub
		
	}
	
	public void visible(final boolean mode){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: visible() begin");
		if (mode == true) {
			System.out.println("js ch.elexis.views/BriefAuswahl.java: visible() mode == true -> about to ElexisEventDispatcher.getInstance().addListeners(this); relabel()...");
			ElexisEventDispatcher.getInstance().addListeners(this);
			relabel();
		} else {
			System.out.println("js ch.elexis.views/BriefAuswahl.java: visible() mode == false -> about to ElexisEventDispatcher.getInstance().removeListeners(this)...");
			ElexisEventDispatcher.getInstance().removeListeners(this);
		}
		System.out.println("js ch.elexis.views/BriefAuswahl.java: visible() end");
		
	}
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: promptToSaveOnClose() begin ... about to (complex) and return results...");
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(final IProgressMonitor monitor){ /* leer */
		System.out.println("js ch.elexis.views/BriefAuswahl.java: doSave() begin nop end");
	}
	
	public void doSaveAs(){ /* leer */
		System.out.println("js ch.elexis.views/BriefAuswahl.java: doSaveAs() begin nop end");
	}
	
	public boolean isDirty(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: isDirty() WARNING: *** will always return true ***"); 
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: isSaveAsAllowed() WARNING: *** will always return false ***"); 
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: isSaveOnCloseNeeded() WARNING: *** will always return true ***"); 
		return true;
	}
	
	public void catchElexisEvent(ElexisEvent ev){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: catchElexisEvent() begin about to relabel(); end"); 
		relabel();
	}
	
	private static ElexisEvent template = new ElexisEvent(null, Patient.class,
		ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_DESELECTED);
	
	public ElexisEvent getElexisEventFilter(){
		System.out.println("js ch.elexis.views/BriefAuswahl.java: ElexisEvent() getElexisEventFilter() begin about to return template..."); 
		return template;
	}
}
