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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
		tk = Desk.getToolkit();
	}
	
	@Override
	public void createPartControl(final Composite parent){
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("BriefAuswahlAllLetters")).append(Brief.UNKNOWN).append(",").append(Brief.AUZ) //$NON-NLS-1$
			.append(",").append(Brief.RP).append(",").append(Brief.LABOR);
		String cats = Hub.globalCfg.get(PreferenceConstants.DOC_CATEGORY, sb.toString());
		parent.setLayout(new GridLayout());
		
		form = tk.createForm(parent);
		form.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		form.setBackground(parent.getBackground());
		
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
	}
	
	@Override
	public void dispose(){
		ElexisEventDispatcher.getInstance().removeListeners(this);
		GlobalEventDispatcher.removeActivationListener(this, this);
		
		for (sPage page : pages) {
			page.getCommonViewer().getConfigurer().getContentProvider().stopListening();
		}
	}
	
	@Override
	public void setFocus(){
		
	}
	
	public void relabel(){
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
		
	}
	
	class sPage extends Composite {
		private final CommonViewer cv;
		private final ViewerConfigurer vc;
		
		public CommonViewer getCommonViewer(){
			return cv;
		}
		
		sPage(final Composite parent, final String cat){
			super(parent, SWT.NONE);
			setLayout(new GridLayout());
			cv = new CommonViewer();
			vc =
				new ViewerConfigurer(new DefaultContentProvider(cv, Brief.class, new String[] {
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
					return 0;
				}
			});
			vc.getContentProvider().startListening();
			Button bLoad =
				tk.createButton(this, Messages.getString("BriefAuswahlLoadButtonText"), SWT.PUSH); //$NON-NLS-1$
			bLoad.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e){
					try {
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						Object[] o = cv.getSelection();
						if ((o != null) && (o.length > 0)) {
							Brief brief = (Brief) o[0];
							if (tv.openDocument(brief) == false) {
								SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
									Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
							}
						} else {
							tv.createDocument(null, null);
						}
					} catch (Throwable ex) {
						ExHandler.handle(ex);
					}
				}
				
			});
			bLoad.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			
		}
	}
	
	private void makeActions(){
		briefNeuAction = new Action(Messages.getString("BriefAuswahlNewButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
					Patient pat = ElexisEventDispatcher.getSelectedPatient();
					if (pat == null) {
						MessageDialog.openInformation(Desk.getTopShell(),
							Messages.getString("BriefAuswahlNoPatientSelected"),
							Messages.getString("BriefAuswahlNoPatientSelected"));
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
					
					TextView tv = null;
					try {
						tv = (TextView) getSite().getPage().showView(TextView.ID /*
																				 * ,StringTool.unique
																				 * ("textView")
																				 * ,IWorkbenchPage
																				 * .VIEW_ACTIVATE
																				 */);
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
				}
			};

			briefLadenAction = new Action(Messages.getString("BriefAuswahlOpenButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
					try {
						TextView tv = (TextView) getViewSite().getPage().showView(TextView.ID);
						CTabItem sel = ctab.getSelection();
						if (sel != null) {
							CommonViewer cv = (CommonViewer) sel.getData();
							Object[] o = cv.getSelection();
							if ((o != null) && (o.length > 0)) {
								Brief brief = (Brief) o[0];
								if (tv.openDocument(brief) == false) {
									SWTHelper.alert(Messages.getString("BriefAuswahlErrorHeading"), //$NON-NLS-1$
										Messages.getString("BriefAuswahlCouldNotLoadText")); //$NON-NLS-1$
								}
							} else {
								tv.createDocument(null, null);
							}
							cv.notify(CommonViewer.Message.update);
						}
					} catch (PartInitException e) {
						ExHandler.handle(e);
					}
					
				}
			};
		
			//20140421js: added stress test feature.
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
										//Die folgenden verbessern nichts am Verhalten: Die ersten beiden werden aktualisiert angezeigt, danach keines ausser dem letzten:
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
	
								if (stressTestPasses>10) {
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
								
					} //if (actPat != null )
				System.out.println("stress test pass: "+stressTestPasses+" - stress test ends.");
					
				}
			};
			
		deleteAction = new Action(Messages.getString("BriefAuswahlDeleteButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
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
					
				}
			};
		editNameAction = new Action(Messages.getString("BriefAuswahlRenameButtonText")) { //$NON-NLS-1$
				@Override
				public void run(){
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
				}
			};
		/*
		 * importAction=new Action("Importieren..."){ public void run(){
		 * 
		 * } };
		 */
		briefLadenAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document_text.png")); //$NON-NLS-1$
		briefLadenAction.setToolTipText(Messages.getString("BriefAuswahlOpenLetterForEdit")); //$NON-NLS-1$
		briefNeuAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__plus.png")); //$NON-NLS-1$
		briefNeuAction.setToolTipText(Messages.getString("BriefAuswahlCreateNewDocument")); //$NON-NLS-1$
		editNameAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__pencil.png")); //$NON-NLS-1$
		editNameAction.setToolTipText(Messages.getString("BriefAuswahlRenameDocument")); //$NON-NLS-1$
		deleteAction.setImageDescriptor(Hub.getImageDescriptor("rsc/document__minus.png")); //$NON-NLS-1$
		deleteAction.setToolTipText(Messages.getString("BriefAuswahlDeleteDocument")); //$NON-NLS-1$
	}
	
	public void activation(final boolean mode){
		// TODO Auto-generated method stub
		
	}
	
	public void visible(final boolean mode){
		if (mode == true) {
			ElexisEventDispatcher.getInstance().addListeners(this);
			relabel();
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(this);
		}
		
	}
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(final IProgressMonitor monitor){ /* leer */
	}
	
	public void doSaveAs(){ /* leer */
	}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
	public void catchElexisEvent(ElexisEvent ev){
		relabel();
	}
	
	private static ElexisEvent template = new ElexisEvent(null, Patient.class,
		ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_DESELECTED);
	
	public ElexisEvent getElexisEventFilter(){
		return template;
	}
}
