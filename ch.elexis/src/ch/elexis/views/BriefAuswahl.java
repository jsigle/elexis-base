/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * Portions Copyright (c) 2013, Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    J. Sigle - added stress test feature to aid stabilizing NOAText_jsl and/or other text plugins.
 *    
 *******************************************************************************/

package ch.elexis.views;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.ui.IPropertyListener;	//201306150113js - ensure edits in text documents are noted by Elexis and ultimately store
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
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
			Button bLoad =
				tk.createButton(this, Messages.getString("BriefAuswahlLoadButtonText"), SWT.PUSH); //$NON-NLS-1$
			bLoad.addSelectionListener(new SelectionAdapter() {
				@Override
				//ToDo: 201306170133js: This might be refactored into a separate method
				//to ensure that clicking on the "bLoad" button does quite the same stuff
				//as a double click on an entry of the document list - "briefLadenAction". 
				public void widgetSelected(final SelectionEvent e){
					System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() begin");
					try {
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						Object[] o = cv.getSelection();
						if ((o != null) && (o.length > 0)) {
							Brief brief = (Brief) o[0];
							System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() about to tv.openDocument(brief)...");
							if (tv.openDocument(brief) == false) {
								SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
									Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
							}
						} else {
							System.out.println("js ch.elexis.views/BriefAuswahl.java: sPage.sPage() bLoad.widgetSelected() about to tv.createDocument()...");
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
					
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefNeuAction run() about to instantiate new TextView tv...");
					TextView tv = null;
					try {
						tv = (TextView) getSite().getPage().showView(TextView.ID /*
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
					System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() begin");

					try {
						System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to: Textview tv = (TextView) getViewSite().getPage().showView(TextView.ID)...");
						
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						CTabItem sel = ctab.getSelection();
						if (sel != null) {
							CommonViewer cv = (CommonViewer) sel.getData();
							Object[] o = cv.getSelection();
							if ((o != null) && (o.length > 0)) {
								Brief brief = (Brief) o[0];

								System.out.println("js ch.elexis.views/BriefAuswahl.java: makeActions() briefLadenAction run() about to tv.openDocument(brief)...");
								
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
						Thread.sleep(1000);
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
									System.out.println("stress test pass: "+stressTestPasses+" - about to load document...");
								
									try {
										TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
	
										System.out.println("stress test pass: "+stressTestPasses+" - o !!= null; (Brief) brief[0.getLabel()]=<"+brief.getLabel().toString()+">");
										System.out.println("stress test pass: "+stressTestPasses+" - try {} section o != null; about to tv.openDocument(brief)....");
																	
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
										//Thread.sleep(10000);
										//Thread.sleep(1000);
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
