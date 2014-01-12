/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis; Portions (c) 2013, Joerg Sigle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle	 - added close() to class SelectionPrintDialog, calling text.getPlugin().dispose() so that OO/LO connection/server is disconnected/unloaded after use where appropriate.
 *    J. Sigle   - added filtering to suppress Patients/Faelle/Behandlungen with Abrechnungssystem like "keine Rechnung", "keine PraxisRechnung"
 *    G. Weirich - initial implementation
 *    
 *  $Id: KonsZumVerrechnenView.java 6229 2010-03-18 14:03:16Z michael_imhof $
 *******************************************************************************/

package ch.elexis.views.rechnung;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.RestrictedAction;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.commands.ErstelleRnnCommand;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.dialogs.KonsZumVerrechnenWizardDialog;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.text.TextContainer;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.BasicTreeContentProvider;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.CommonViewer.DoubleClickListener;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.views.FallDetailView;
import ch.elexis.views.KonsDetailView;
import ch.elexis.views.PatientDetailView2;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.IFilter;		//20130530js
import ch.rgw.tools.RegexpFilter;	//20130530js
import ch.rgw.tools.JdbcLink.Stm;
import ch.rgw.tools.LazyTree;
import ch.rgw.tools.LazyTree.LazyTreeListener;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.Tree;

import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * Anzeige aller Behandlungen, für die noch keine Rechnung erstellt wurde. Die Behandlungen werden
 * nach Patient und Fall gruppiert. Patienten, Fälle und Behandlungen können einzeln oder in Gruppen
 * in eine Auswahl übertragen werden, aus der später Rechnungen erstellt werden können.
 * 
 * @author Gerry
 * 
 */
public class KonsZumVerrechnenView extends ViewPart implements ISaveablePart2 {
	public static final String ID = "ch.elexis.BehandlungenVerrechnenView"; //$NON-NLS-1$
	CommonViewer cv;
	ViewerConfigurer vc;
	FormToolkit tk = Desk.getToolkit();
	Form left, right;
	@SuppressWarnings("unchecked")
	LazyTree tAll;
	@SuppressWarnings("unchecked")
	Tree tSelection;
	TreeViewer tvSel;
	LazyTreeListener ltl;
	ViewMenus menu;
	
	
	private IAction billAction, printAction, clearAction, wizardAction, refreshAction,
			detailAction;
	private IAction removeAction;
	private IAction expandSelAction;
	private IAction expandSelAllAction;
	private IAction selectByDateAction;
	KonsZumVerrechnenView self;

	//20130530js
	////IFilter filter;

	public KonsZumVerrechnenView(){
		////System.out.println("js KonsZumVerrechnenView constructor: about to instantiate: cv, ltl, tSelection, tAll");
		
		cv = new CommonViewer();
		ltl = new RLazyTreeListener();
		tSelection = new Tree<PersistentObject>(null, null);
		
		tAll = new LazyTree<PersistentObject>(null, null, ltl);
		////tAll = new LazyTree<PersistentObject>(null, null, (IFilter) filter, ltl);
		
		self = this;				
		
		//20130530js
		////System.out.println("js KonsZumVerrechnenView constructor: about to: js filter = new hatZumAbrechnen()");
		//filter = new HatZumAbrechnen();
		//Filter erst mal ausschalten - denn der wird für den Versuch des Ausblendens von Patienten/Fällen
		//die nur Inhalte im Abrechnungssystem keine-Rechnung etc. haben, ohnehin durch tree.java getChildren()
		//überstimmt, welches sowohl für filter.select=true als auch für hasChildren() Objekte zurückliefert.
		//Deshalb habe ich stattdessen hier in Kons...java oben bei hasChildren() eine Modifikation gemacht,
		//die diese Filterfunktion übernimmt. - filter könnte dann die Einträge pauschal nach Eintragstext filtern oder ähnlich.
		////filter = null;
		////System.out.println("js KonsZumVerrechnenView constructor: about to: tAll.setfilter(filter); tSelection.setfilter(filter)");
		////tAll.setFilter(filter);
		////tSelection.setFilter(filter);		
	}
	
	@Override
	public void dispose(){
		// GlobalEvents.getInstance().removeActivationListener(this,this);
		super.dispose();
	}
	
	@Override
	public void createPartControl(final Composite parent){
		vc =
			new ViewerConfigurer(new BasicTreeContentProvider(),
				new ViewerConfigurer.TreeLabelProvider() {
					// extend the TreeLabelProvider by getImage()
					
					@SuppressWarnings("unchecked")
					@Override
					public Image getImage(final Object element){
						if (element instanceof Tree) {
							Tree tree = (Tree) element;
							PersistentObject po = (PersistentObject) tree.contents;
							if (po instanceof Fall) {
								if (po.isValid()) {
									return Desk.getImage(Desk.IMG_OK);
								} else {
									return Desk.getImage(Desk.IMG_FEHLER);
								}
							}
						}
						return null;
					}
				}, null, // new DefaultControlFieldProvider(cv, new
				// String[]{"Datum","Name","Vorname","Geb. Dat"}),
				new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
					SimpleWidgetProvider.TYPE_TREE, SWT.MULTI | SWT.V_SCROLL, cv));
		
		//20130530js Doc attempt:
		//Die linke Hälfte der View "Konsultationen zum Verrechnen" aufbauen:
		//Hier wird der LazyTree tAll angezeigt.
		SashForm sash = new SashForm(parent, SWT.NULL);
		left = tk.createForm(sash);
		Composite cLeft = left.getBody();
		left.setText(Messages.getString("KonsZumVerrechnenView.allOpenCons")); //$NON-NLS-1$
		cLeft.setLayout(new GridLayout());
//System.out.println("js KonsZumVerrechnenView createPartControl LEFT: cv.cretae(...tAll)...");				
		cv.create(vc, cLeft, SWT.NONE, tAll);
		cv.getViewerWidget().setComparator(new KonsZumVerrechnenViewViewerComparator());
		
		cv.addDoubleClickListener(new DoubleClickListener() {
			@Override
			public void doubleClicked(PersistentObject obj, CommonViewer cv){
				if (obj instanceof Patient) {
					try {
						ElexisEventDispatcher.fireSelectionEvent((Patient) obj);
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(PatientDetailView2.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (obj instanceof Fall) {
					try {
						ElexisEventDispatcher.fireSelectionEvent((Fall) obj);
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(FallDetailView.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				} else if (obj instanceof Konsultation) {
					try {
						ElexisEventDispatcher.fireSelectionEvent((Konsultation) obj);
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.showView(KonsDetailView.ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		
		//20130530js Doc attempt:
		//Die rechte Hälfte der View "Konsultationen zum Verrechnen" aufbauen.
		//Hier wird später im TreeViewer tvSel der Tree tSelection angezeigt.
		right = tk.createForm(sash);
		Composite cRight = right.getBody();
		right.setText(Messages.getString("KonsZumVerrechnenView.selected")); //$NON-NLS-1$
		cRight.setLayout(new GridLayout());
		tvSel = new TreeViewer(cRight, SWT.V_SCROLL | SWT.MULTI);
		// tvSel.getControl().setLayoutData(SWTHelper.getFillGridData(1,true,t,true));
		tvSel.getControl().setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tvSel.setContentProvider(new BasicTreeContentProvider());
		tvSel.setLabelProvider(new LabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(final Object element){
//System.out.println("js KonsZumVerrechnenView createPartControl: RIGHT: tvSel.setLabelProvider.getText(): about to return ((Tree).element).contents.getlabel");				
				return ((PersistentObject) ((Tree) element).contents).getLabel();
			}
			
		});
		tvSel.setComparator(new KonsZumVerrechnenViewViewerComparator());
		tvSel.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] {
			TextTransfer.getInstance()
		}, new DropTargetAdapter() {
			
			@Override
			public void dragEnter(final DropTargetEvent event){
				event.detail = DND.DROP_COPY;
			}
			
			@Override
			public void drop(final DropTargetEvent event){
				String drp = (String) event.data;
				String[] dl = drp.split(","); //$NON-NLS-1$
				for (String obj : dl) {
					PersistentObject dropped = Hub.poFactory.createFromString(obj);
					if (dropped instanceof Patient) {
						selectPatient((Patient) dropped, tAll, tSelection);
					} else if (dropped instanceof Fall) {
						selectFall((Fall) dropped, tAll, tSelection);
					} else if (dropped instanceof Konsultation) {
						selectBehandlung((Konsultation) dropped, tAll, tSelection);
					}
					
				}
				tvSel.refresh(true);
				
			}
			
		});
		tvSel.addSelectionChangedListener(GlobalEventDispatcher.getInstance().getDefaultListener());
		tvSel.setInput(tSelection);
		// GlobalEvents.getInstance().addActivationListener(this,this);
		sash.setWeights(new int[] {
			60, 40
		});
		makeActions();
		MenuManager selMenu = new MenuManager();
		selMenu.setRemoveAllWhenShown(true);
		selMenu.addMenuListener(new IMenuListener() {
			
			public void menuAboutToShow(final IMenuManager manager){
				manager.add(removeAction);
				manager.add(expandSelAction);
				manager.add(expandSelAllAction);
				
			}
			
		});
		tvSel.getControl().setMenu(selMenu.createContextMenu(tvSel.getControl()));
		
		tvSel.getControl().addListener(SWT.MouseDoubleClick, new Listener() {
			@Override
			public void handleEvent(Event event){
				org.eclipse.swt.widgets.Tree theWidget =
					(org.eclipse.swt.widgets.Tree) (event.widget);
				TreeItem obj = theWidget.getSelection()[0];
				TreeItem parent = obj.getParentItem();
				String viewID = "";
				if (parent == null) {
					// no parent at all -> must be patient
					viewID = PatientDetailView2.ID;
				} else {
					// may be case or cons
					TreeItem grandpa = parent.getParentItem();
					if (grandpa == null) {
						// must be case
						viewID = FallDetailView.ID;
					} else {
						// must be cons
						viewID = KonsDetailView.ID;
					}
				}
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.showView(viewID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
		
		menu = new ViewMenus(getViewSite());
		menu.createToolbar(refreshAction, wizardAction, printAction, clearAction, null, billAction);
		menu.createMenu(wizardAction, selectByDateAction);
		menu.createViewerContextMenu(cv.getViewerWidget(), detailAction);
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	
	class RLazyTreeListener implements LazyTreeListener {
		final LazyTreeListener self = this;
						
		@SuppressWarnings("unchecked")
		// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
		/**
		 * 20130530js - Ein Dokumentationsversuch zur Orientierung:
		 * 
		 * Beim Anzeigen der View "Konsultationen zum Verrechnen" werden erst einmal
		 * per SQL-Abfrage alle Patienten gesucht, für die FAELLE eingetragen sind,
		 * für die nicht gelöschte BEHANDLUNGEN eingetragen sind, deren RECHNUNGSID null ist.
		 * Diese werden als eine Serie von geschlossenen (Sub)Trees angezeigt.
		 * 
		 * Erst wenn man später einen (Sub)Tree "Patient" per Mausklick öffnet, werden zu dem jeweiligen Patienten auch die Fälle gesucht
		 * per eigener SQL-Abfrage, und wiederum jeder davon als geschlossener (Sub)Tree unterhalb des Patienten gezeigt.
		 * Erst wenn man noch später einen (Sub)Tree "Fall" per Mausklick öffnet, werden zu dem jeweiligen Fall auch die Behandlungen (=Konsultationen) gesucht
		 * per eigener SQL-Abfrage, und wiederum jede davon als geschlossener (Sub)Tree unterhalb des Falls gezeigt.
		 * 
		 * Die Tabelle FAELLE enthält zwar Felder wie GESETZ etc., da finde ich aber immer nur NULL drin.
		 * Vermutlich werden viele Informationen tatsächlich in extinfo longblob gespeichert. :-(
		 */
		public boolean fetchChildren(final LazyTree l){
			//System.out.println("js KonsZumVerrechnen.java: RLazyTreeListener.fetchChildren() begin");
			// TODO: Die Methode wird laut log beim Anzeigen der View etwa 4x aufgerufen, und die ersten zwei mal gibt's sofort eine rote Exception. Schöner wäre Aufruf nur wenn nötig, und wenn alle Voraussetzungen erfüllt sind. 
			//System.out.println("js TO DO: KonsZumVerrechnenView.fetchChildren() Wird vermutlich unnötig und unschön oft aufgerufen.");

			PersistentObject cont = (PersistentObject) l.contents;
			final Stm stm = PersistentObject.getConnection().getStatement();
			if (cont == null) {
	//System.out.println("js: KonsZumVerrechnen.java: cont == null...");
				IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
				try {
					progressService.runInUI(PlatformUI.getWorkbench().getProgressService(),
						new IRunnableWithProgress() {
							public void run(final IProgressMonitor monitor){
								monitor.beginTask(
									Messages.getString("KonsZumVerrechnenView.findCons"), 100); //$NON-NLS-1$
								monitor.subTask(Messages
									.getString("KonsZumVerrechnenView.databaseRequest")); //$NON-NLS-1$

	// TODO: WARNUNG: KonsZumVerrechnen.java: fetchChildren() Müsste/Könnte hier auch noch and FAELLE.deleted='0' rein?
	//System.out.println("js TODO WARNUNG: KonsZumVerrechnen.java: fetchChildren() Müsste/Könnte hier auch noch and FAELLE.deleted='0' rein?");
								
								String sql =
									"SELECT distinct PATIENTID FROM FAELLE " + //$NON-NLS-1$
										"JOIN BEHANDLUNGEN ON BEHANDLUNGEN.FALLID=FAELLE.ID WHERE BEHANDLUNGEN.deleted='0' AND BEHANDLUNGEN.RECHNUNGSID is null "; //$NON-NLS-1$
								if (Hub.acl.request(AccessControlDefaults.ACCOUNTING_GLOBAL) == false) {
									sql += "AND BEHANDLUNGEN.MANDANTID=" //$NON-NLS-1$
										+ Hub.actMandant.getWrappedId();								
								}
								
	// TODO: Fälle mit dem Typ "Lindenhof - keine Rechnung" und "keine Rechnung" nicht zurückliefern.
	// Das könnte man über  if ... sq+= "AND Gesetz like "%keine Rechnung%" lösen, wenn das Feld "Gesetz" hier nicht dauernd NULL wäre.
	// Möglicherweise stehen die relevanten Informationen in ExtInfo drin, und das wiederum ist ein longblob. :-(
	//System.out.println("js TODO: Fälle mit dem Typ \"Lindenhof - keine Rechnung\" und \"keine Rechnung\" nicht zurückliefern.");
						
	System.out.println("js: KonsZumVerrechnen.java: fetchChildren() FAELLE.PATIENTID mit Behandlungen ohne RechnungsID, ggf. für einen bestimmten Mandanten: sql=\n"+sql);
						
								ResultSet rs = stm.query(sql);
								monitor.worked(10);
								monitor.subTask(Messages.getString("KonsZumVerrechnenView.readIn")); //$NON-NLS-1$
								try {
									while ((rs != null) && rs.next()) {
										String s = rs.getString(1);
										Patient p = Patient.load(s);
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() PATIENTID: s="+s+", "+p.getName()+" "+p.getVorname());
										//201305301445js: Dokumentationsversuch:
										//wenn es der Patient mit der via SQL gefundenen PATIENTID als Objekt ladbar ist (i.e.: existiert, nicht geloescht),
										//dieses Objekt jedoch noch nicht in tSelection (!!!) drin ist,
										//dann es in einen (!!!) Tree aufnehmen.
										
										//ICH BIN VÖLLIG UNSICHER, warum hier mit tSelection() verglichen wird,
										//und wie der Zieltree übergeben wird. Vermutlich ist das nämlich tAll,
										//vielleicht aber auch nicht.
										//Wenn ich den tSelection.find() Test auskommentiere,
										//bekomme ich kein anderes Ergebnis, jedenfalls so lange in der rechten Spalte noch kein Auswahltree angelegt ist.
		
										//Ah, wenn ich einen Patienten von links nach rechts ziehe,
										//d.h. von tAll nach tSelection, dann soll er in tAll wohl NICHT mehr erscheinen.
										//Das ist vermutlich alles - dasselbe dann auch für Faelle und Behandlungen weiter unten.
		
		
										
										//Ich ergänze nun mal code, der Patienten dann nicht mehr anzeigt, wenn alle Fälle dieser Patienten
										//nur zu Abrechnungssystemen gehören, die den String "...keine Rechnung" enthalten.

										// TO DO: Da das Abrechnungssystem leider nicht in der SQL-Tabelle gespeichert ist, sondern offenbar im blob,
										//muss ich dafür erst eine Java-Funktion schreiben.
										// Viel eleganter wäre es, wenn das DBMS solche Dinge erledigen dürfte.
			
										// TO DO: Nachdem oben ja distinct in der SQL Abfrage steht, wäre der tSelection.find() Test nur dann nötig,
										// wenn die Selection nicht jedesmal gelöscht würde.
		
										//20130530js: Nur Patienten anzeigen, wenn auch hatFaelleZumAbrechnen(); dafür reichen 
										//Fälle nicht, deren Abrechnungssystemen.toLower.contains("keine rechnung"|"keine praxisrechnung").
										if (p.exists() 
											&& p.hatFaelleZumAbrechnen()  
											&& (tSelection.find(p, false) == null) )  {
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() About to new LazyTree(l, p, (IFilter) filter, self)...");
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() where (filter == null) is "+(filter==null));
											
											new LazyTree(l, p, self);
											////new LazyTree(l, p, (IFilter) filter, self);
										}
										monitor.worked(1);
									}
									monitor.done();
								} catch (SQLException e) {
									ExHandler.handle(e);
								}
							}
						}, null);
				} catch (Throwable ex) {
					ExHandler.handle(ex);
				} //if cont == null
				
			} else {
		//System.out.println("js: KonsZumVerrechnen.java: cont != null...");

				ResultSet rs = null;
				String sql;
				try {
					if (cont instanceof Patient) {
		//System.out.println("js: KonsZumVerrechnen.java: cont instanceof Patient...");

						sql =
							"SELECT distinct FAELLE.ID FROM FAELLE join BEHANDLUNGEN ON BEHANDLUNGEN.FALLID=FAELLE.ID " + //$NON-NLS-1$
								"WHERE BEHANDLUNGEN.RECHNUNGSID is null AND BEHANDLUNGEN.DELETED='0' AND FAELLE.PATIENTID=" //$NON-NLS-1$
								+ cont.getWrappedId(); //$NON-NLS-1$
						if (Hub.acl.request(AccessControlDefaults.ACCOUNTING_GLOBAL) == false) {
							sql += " AND BEHANDLUNGEN.MANDANTID=" + Hub.actMandant.getWrappedId(); //$NON-NLS-1$

	// TODO/DONE: Fälle mit dem Typ "Lindenhof - keine Rechnung" und "keine Rechnung" nicht zurückliefern.
	// Das könnte man über  if ... sq+= "AND Gesetz like "%keine Rechnung%" lösen, wenn das Feld "Gesetz" hier nicht dauernd NULL wäre.
	// Möglicherweise stehen die relevanten Informationen in ExtInfo drin, und das wiederum ist ein longblob. :-(
	//System.out.println("js TODO: Fälle mit dem Typ \"Lindenhof - keine Rechnung\" und \"keine Rechnung\" nicht zurückliefern.");

						}

	System.out.println("js: KonsZumVerrechnen.java: fetchChildren() FAELLE.ID zu einer PatientID mit Behandlungen ohne RechnungsID: sql=\n"+sql);

						rs = stm.query(sql);
						while ((rs != null) && rs.next()) {
							String s = rs.getString(1);
							Fall f = Fall.load(s);
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() FAELLE.ID: s="+s+", FAELLE.PATIENTID...="+f.getPatient().getName()+" "+f.getPatient().getVorname());
	
						//20130530js: Nur Fälle anzeigen, wenn auch hatBehandlungenZumAbrechnen(); dafür reichen 
						//Fälle nicht, deren Abrechnungssystemen.toLower.contains("keine rechnung"|"keine praxisrechnung").
	
						//TO DO: Auch hier wiederum: WARUM die Suche in tSelection? Warum nicht in tAll? Oder in einem übergebenen Baum?
							if (f.exists()
								// && f.getPatient().exists()
								// && f.getPatient().hatFaelleZumAbrechnen() //vielleicht redundant, bei false wäre das nächste wohl auch false
								&& f.hatBehandlungenZumAbrechnen()
								&& (tSelection.find(f, true) == null)) {
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() About to new LazyTree(l, f, (IFilter) filter, this)...");
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() where (filter == null) is "+(filter==null));
	
								new LazyTree(l, f, this);
								////new LazyTree(l, f, (IFilter) filter, this);
							}
						}
					} else if (cont instanceof Fall) {
	//System.out.println("js: KonsZumVerrechnen.java: cont instanceof Fall...");

						sql =
							"SELECT ID FROM BEHANDLUNGEN WHERE RECHNUNGSID is null AND deleted='0' AND FALLID=" + cont.getWrappedId(); //$NON-NLS-1$
						if (Hub.acl.request(AccessControlDefaults.ACCOUNTING_GLOBAL) == false) {
							sql += " AND MANDANTID=" + Hub.actMandant.getWrappedId(); //$NON-NLS-1$
						}

	System.out.println("js: KonsZumVerrechnen.java: fetchChildren() BEHANDLUNGEN.ID mit FALLID=cont.getWrappedId(), ggf. für einen Mandanten: sql=\n"+sql);
						rs = stm.query(sql);
						while ((rs != null) && rs.next()) {
							String s = rs.getString(1);
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() BEHANDLUNGEN.ID: s="+s);
							Konsultation b = Konsultation.load(s);

							//TO DO: Auch hier wiederum: WARUM die Suche in tSelection? Warum nicht in tAll? Oder in einem übergebenen Baum?
							if (b.exists()
								&& (tSelection.find(b, true) == null)) {
	
	//System.out.println("js: KonsZumVerrechnen.java: fetchChildren() About to new LazyTree(l, b, this)...");
								new LazyTree(l, b, this);
							}
						}
					}
					if (rs != null) {
						rs.close();
					}
				} catch (Exception e) {
					ExHandler.handle(e);
				} finally {
					PersistentObject.getConnection().releaseStatement(stm);
				}
			}
			
	//System.out.println("js KonsZumVerrechnen.java: fetchChildren(): Nach Durchlauf die Filter nochmals nachträglich setzen mit l.setFilter(filter)...");
	//		l.setFilter(filter);
			
	//System.out.println("js KonsZumVerrechnen.java: fetchChildren() end - about to return false");
			return false;
		}
		
		@SuppressWarnings("unchecked")
		// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
		public boolean hasChildren(final LazyTree l){
			//System.out.println("js hasChildren(l) begin");

			Object po = l.contents;
			
			//js: Standard wäre: Für Patienten und Fälle zurückgeben, dass diese Children haben. Für Behandlungen: Nicht.
			//js: Als nächsten Versuch zur Implementation eines Filters geb ich jetzt für erstere mal zurück,
			//js: ob sie Fälle/Behandlungen zum Verrechnen haben. 
		
			/**
			 * 20130530js:
			 * I should rather avoid too many calls to the hat...() functions, because they cause some large overhead.
			 * After having discovered the typo in Fall.hatBehandlungenZumAbrechnen(), responsible for my initial implementation
			 * having ended in unexpected results, the original implementation works again. I think it should be rather efficient,
			 * given the lack of ability to use a direct SQL query because the SQL db fields are not properly used.
			 * So I will NOT keep the same filtering around here active for now.
			////Maybe we could still need/use it when adding another level of text filtering; so I add a comment with four //// to make this visible.   
			if (po instanceof Patient) {
				//System.out.println("js hasChildren(l): l instanceof Patient: ...="+((Patient) po).getName()+" "+((Patient) po).getVorname());
				return ((Patient) po).hatFaelleZumAbrechnen();
			} 
			else if (po instanceof Fall) {
					//System.out.println("js hasChildren(l): l instanceof Fall: ...="+((Fall) po).getAbrechnungsSystem());
					return ((Fall) po).hatBehandlungenZumAbrechnen();
			}
			else
			*/
			
			if (po instanceof Konsultation) {
				//System.out.println("js hasChildren(l): l instanceof Konsultation: ...verrechnet?="+(((Konsultation) po).getRechnung() == null));
				//System.out.println("js hasChildren(l) end - returning true");
				return false;
			}
			//System.out.println("js hasChildren(l) end - returning true");
			return true;
		}
		
	}
	
	// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
	public void selectKonsultation(final Konsultation k){
		//System.out.println("js KonsZumVerrechnenView.java selectKonsultation() begin");
		//System.out.println("js TO DO: KonsZumVerrechnenView.java selectKonsultation() Jegliche Dokumentation der Methode fehlt.");

		selectBehandlung(k, tAll, tSelection);

		//System.out.println("js KonsZumVerrechnenView.java selectKonsultation() end - hier steht kein return drin");
	}
	
	/**
	 * Patienten in von tAll nach tSelection verschieben bzw. falls noch nicht vorhanden, neu
	 * anlegen.
	 */
	@SuppressWarnings("unchecked")
	// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
	private Tree selectPatient(final Patient pat, final Tree tSource, final Tree tDest){
		//System.out.println("js KonsZumVerrechnenView.java selectPatient() begin");
		//System.out.println("js TO DO: KonsZumVerrechnenView.java selectPatient() Jegliche Dokumentation der Methode fehlt.");

		Tree pSource = tSource.find(pat, false);
		Tree pDest = tDest.find(pat, false);
		if (pDest == null) {
			if (pSource == null) {
				pDest = tDest.add(pat);
			} else {
				pDest = pSource.move(tDest);
			}
		} else {
			if (pSource != null) {
				List<Tree> fs = (List<Tree>) pSource.getChildren();
				for (Tree t : fs) {
					selectFall((Fall) t.contents, tSource, tDest);
				}
			}
		}
		cv.getViewerWidget().refresh(tSource);

		//System.out.println("js KonsZumVerrechnenView.java selectPatient() end - about to return tFall");
		return pDest;
	}
	
	@SuppressWarnings("unchecked")
	// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
	private Tree selectFall(final Fall f, final Tree tSource, final Tree tDest){
	//System.out.println("js KonsZumVerrechnenView.java selectFall() begin");
	//System.out.println("js TO DO: KonsZumVerrechnenView.java selectFall() Jegliche Dokumentation der Methode fehlt.");
	
		Patient pat = f.getPatient();
		Tree tPat = tDest.find(pat, false);
		if (tPat == null) {
			tPat = tDest.add(pat);
		}
		Tree tFall = tSource.find(f, true);
		if (tFall == null) {
			tFall = tPat.add(f);
		} else {
			Tree tOld = tFall.getParent();
			tPat.merge(tFall);
			if (tOld.getFirstChild() == null) {
				tSource.remove(tOld);
			}
			cv.getViewerWidget().refresh(tOld);
		}

		//System.out.println("js KonsZumVerrechnenView.java selectFall() end - about to return tFall");
		return tFall;
	}
	
	@SuppressWarnings("unchecked")
	// TO DO: An jemanden der es weiss: Bitte mal in deutsch oder englisch dokumentieren, was diese Funktion machen soll. Ich kann das nur vermuten. js 
	private Tree selectBehandlung(final Konsultation bh, final Tree tSource, final Tree tDest){
		//System.out.println("js KonsZumVerrechnenView.java selectBehandlung() begin");
		//System.out.println("js TO DO: KonsZumVerrechnenView.selectBehandlung() Jegliche Dokumentation der Methode fehlt.");

		Fall f = bh.getFall();
		Patient pat = f.getPatient();
		Tree tPat = tDest.find(pat, false);
		if (tPat == null) {
			tPat = tDest.add(pat);
		}
		Tree tFall = tPat.find(f, false);
		if (tFall == null) {
			tFall = tPat.add(f);
		}
		Tree tBeh = tFall.find(bh, false);
		if (tBeh == null) {
			tBeh = tFall.add(bh);
		}
		
		Tree tps = tSource.find(pat, false);
		if (tps != null) {
			Tree tfs = tps.find(f, false);
			if (tfs != null) {
				Tree tbs = tfs.find(bh, false);
				if (tbs != null) {
					tfs.remove(tbs);
					cv.getViewerWidget().refresh(tfs);
				}
				if (tfs.hasChildren() == false) {
					tps.remove(tfs);
					cv.getViewerWidget().refresh(tps);
				}
			}
			if (tps.hasChildren() == false) {
				tSource.remove(tps);
				cv.getViewerWidget().refresh(tSource);
			}
		}

		//System.out.println("js KonsZumVerrechnenView.java selectBehandlung() end - about to return tFall");
		return tBeh;
	}
	
	private void makeActions(){
		billAction = new Action(Messages.getString("KonsZumVerrechnenView.createInvoices")) { //$NON-NLS-1$
				{
					setImageDescriptor(Hub.getImageDescriptor("rsc/rechnung.gif")); //$NON-NLS-1$
					setToolTipText(Messages.getString("KonsZumVerrechnenView.createInvoices")); //$NON-NLS-1$
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public void run(){
					if (((StructuredSelection) tvSel.getSelection()).size() > 0) {
						if (!SWTHelper.askYesNo(
							Messages.getString("KonsZumVerrechnenView.RealleCreateBillsCaption"), //$NON-NLS-1$
							Messages.getString("KonsZumVerrechnenView.ReallyCreateBillsBody"))) { //$NON-NLS-1$
							return;
						}
					}
					// Handler.execute(getViewSite(), "bill.create", tSelection);
					ErstelleRnnCommand.ExecuteWithParams(getViewSite(), tSelection);
					/*
					 * IHandlerService handlerService = (IHandlerService)
					 * getViewSite().getService(IHandlerService.class); ICommandService cmdService =
					 * (ICommandService) getViewSite().getService(ICommandService.class); try {
					 * Command command = cmdService.getCommand("bill.create"); Parameterization px =
					 * new Parameterization(command.getParameter
					 * ("ch.elexis.RechnungErstellen.parameter" ), new
					 * TreeToStringConverter().convertToString(tSelection)); ParameterizedCommand
					 * parmCommand = new ParameterizedCommand(command, new Parameterization[] { px
					 * });
					 * 
					 * handlerService.executeCommand(parmCommand, null);
					 * 
					 * } catch (Exception ex) { throw new RuntimeException("add.command not found");
					 * }
					 */
					
					tvSel.refresh();
				}
			};
		clearAction = new Action(Messages.getString("KonsZumVerrechnenView.clearSelection")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor("delete")); //$NON-NLS-1$
					setToolTipText(Messages.getString("KonsZumVerrechnenView.deleteList")); //$NON-NLS-1$
					
				}
				
				@Override
				public void run(){
					tSelection.clear();
					tvSel.refresh();
				}
			};
		refreshAction = new Action(Messages.getString("KonsZumVerrechnenView.reloadAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
					setToolTipText(Messages.getString("KonsZumVerrechnenView.reloadToolTip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
System.out.println("js KonsZumVerrechnenView ... refreshAction begin");					
System.out.println("js KonsZumVerrechnenView ... refreshAction about to tAll.clear...");					
					tAll.clear();
System.out.println("js KonsZumVerrechnenView ... refreshAction about to cv.notify(CommonViewer.Message.update)...");					
					cv.notify(CommonViewer.Message.update);
System.out.println("js KonsZumVerrechnenView ... refreshAction about to tvSel.refresh(true)...");					
					tvSel.refresh(true);
System.out.println("js KonsZumVerrechnenView ... refreshAction end");					
				}
			};
		wizardAction = new Action(Messages.getString("KonsZumVerrechnenView.autoAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_WIZARD));
					setToolTipText(Messages.getString("KonsZumVerrechnenView.autoToolTip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					KonsZumVerrechnenWizardDialog kzvd =
						new KonsZumVerrechnenWizardDialog(getViewSite().getShell());
					if (kzvd.open() == Dialog.OK) {
						IProgressService progressService =
							PlatformUI.getWorkbench().getProgressService();
						try {
							progressService.runInUI(progressService, new Rechnungslauf(self,
								kzvd.bMarked, kzvd.ttFirstBefore, kzvd.ttLastBefore, kzvd.mAmount,
								kzvd.bQuartal, kzvd.bSkip), null);
						} catch (Throwable ex) {
							ExHandler.handle(ex);
						}
						tvSel.refresh();
						cv.notify(CommonViewer.Message.update);
					}
				}
			};
		printAction = new Action(Messages.getString("KonsZumVerrechnenView.printSelection")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
					setToolTipText(Messages.getString("KonsZumVerrechnenView.printToolTip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					new SelectionPrintDialog(getViewSite().getShell()).open();
					
				}
			};
		removeAction = new Action(Messages.getString("KonsZumVerrechnenView.removeFromSelection")) { //$NON-NLS-1$
				@SuppressWarnings("unchecked")
				@Override
				public void run(){
					IStructuredSelection sel = (IStructuredSelection) tvSel.getSelection();
					if (!sel.isEmpty()) {
						for (Object o : sel.toList()) {
							if (o instanceof Tree) {
								Tree t = (Tree) o;
								if (t.contents instanceof Patient) {
									selectPatient((Patient) t.contents, tSelection, tAll);
								} else if (t.contents instanceof Fall) {
									selectFall((Fall) t.contents, tSelection, tAll);
								} else if (t.contents instanceof Konsultation) {
									selectBehandlung((Konsultation) t.contents, tSelection, tAll);
								}
							}
						}
						tvSel.refresh();
						cv.notify(CommonViewer.Message.update);
					}
				}
			};
		
		// expand action for tvSel
		expandSelAction = new Action(Messages.getString("KonsZumVerrechnenView.expand")) { //$NON-NLS-1$
				@SuppressWarnings("unchecked")
				@Override
				public void run(){
					IStructuredSelection sel = (IStructuredSelection) tvSel.getSelection();
					if (!sel.isEmpty()) {
						for (Object o : sel.toList()) {
							if (o instanceof Tree) {
								Tree t = (Tree) o;
								tvSel.expandToLevel(t, TreeViewer.ALL_LEVELS);
							}
						}
					}
				}
			};
		// expandAll action for tvSel
		expandSelAllAction = new Action(Messages.getString("KonsZumVerrechnenView.expandAll")) { //$NON-NLS-1$
				@Override
				public void run(){
					tvSel.expandAll();
				}
			};
		
		selectByDateAction =
			new Action(Messages.getString("KonsZumVerrechnenView.selectByDateAction")) { //$NON-NLS-1$
				TimeTool fromDate;
				TimeTool toDate;
				
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_WIZARD));
					setToolTipText(Messages
						.getString("KonsZumVerrechnenView.selectByDateActionToolTip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					// select date
					SelectDateDialog dialog = new SelectDateDialog(getViewSite().getShell());
					if (dialog.open() == TitleAreaDialog.OK) {
						fromDate = dialog.getFromDate();
						toDate = dialog.getToDate();
						
						IProgressService progressService =
							PlatformUI.getWorkbench().getProgressService();
						try {
							progressService.runInUI(PlatformUI.getWorkbench().getProgressService(),
								new IRunnableWithProgress() {
									public void run(final IProgressMonitor monitor){
										doSelectByDate(monitor, fromDate, toDate);
									}
								}, null);
						} catch (Throwable ex) {
							ExHandler.handle(ex);
						}
						tvSel.refresh();
						cv.notify(CommonViewer.Message.update);
					}
				}
				
			};
		detailAction =
			new RestrictedAction(AccessControlDefaults.LSTG_VERRECHNEN,
				Messages.getString("KonsZumVerrechnenView.billingDetails")) { //$NON-NLS-1$
				@SuppressWarnings("unchecked")
				@Override
				public void doRun(){
					Object[] sel = cv.getSelection();
					if ((sel != null) && (sel.length > 0)) {
						new VerrDetailDialog(getViewSite().getShell(), (Tree) sel[0]).open();
					}
				}
			};
	}
	
	/**
	 * Auwahl der Konsultationen, die verrechnet werden sollen, nach Datum. Es erscheint ein Dialog,
	 * wo man den gewünschten Bereich wählen kann.
	 */
	@SuppressWarnings("unchecked")
	private void doSelectByDate(final IProgressMonitor monitor, final TimeTool fromDate,
		final TimeTool toDate){
		TimeTool actDate = new TimeTool();
		
		// set dates to midnight
		TimeTool date1 = new TimeTool(fromDate);
		TimeTool date2 = new TimeTool(toDate);
		date1.chop(3);
		date2.add(TimeTool.DAY_OF_MONTH, 1);
		date2.chop(3);
		
		List<Tree> lAll = (List<Tree>) tAll.getChildren();
		monitor.beginTask(
			Messages.getString("KonsZumVerrechnenView.selectByDateTask"), lAll.size() + 1); //$NON-NLS-1$
		for (Tree tP : lAll) {
			monitor.worked(1);
			for (Tree tF : (List<Tree>) tP.getChildren()) {
				List<Tree> tK = (List<Tree>) tF.getChildren();
				for (Tree tk : tK) {
					Konsultation k = (Konsultation) tk.contents;
					actDate.set(k.getDatum());
					if (actDate.isAfterOrEqual(date1) && actDate.isBefore(date2)) {
						selectBehandlung((Konsultation) tk.contents, tAll, tSelection);
					}
				}
				if (monitor.isCanceled()) {
					monitor.done();
					return;
				}
			}
		}
		monitor.done();
	}
	
	/***********************************************************************************************
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
	
	/**
	 * SelectDateDialog
	 * 
	 * @author danlutz
	 */
	public class SelectDateDialog extends TitleAreaDialog {
		DatePickerCombo dpFromDate;
		DatePickerCombo dpToDate;
		
		TimeTool fromDate = null;
		TimeTool toDate = null;
		
		public SelectDateDialog(final Shell parentShell){
			super(parentShell);
		}
		
		@Override
		public void create(){
			super.create();
			setTitle(Messages.getString("SelectDateDialog.choosePeriodTitle")); //$NON-NLS-1$
			setMessage(Messages.getString("SelectDateDialog.choosePeriodMessage")); //$NON-NLS-1$
			getShell().setText(Messages.getString("SelectDateDialog.description")); //$NON-NLS-1$
		}
		
		@Override
		protected Control createDialogArea(final Composite parent){
			Composite com = new Composite(parent, SWT.NONE);
			com.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			com.setLayout(new GridLayout(2, false));
			
			new Label(com, SWT.NONE).setText(Messages.getString("SelectDateDialog.from")); //$NON-NLS-1$
			new Label(com, SWT.NONE).setText(Messages.getString("SelectDateDialog.to")); //$NON-NLS-1$
			
			dpFromDate = new DatePickerCombo(com, SWT.NONE);
			dpToDate = new DatePickerCombo(com, SWT.NONE);
			
			return com;
		}
		
		/*
		 * (Kein Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		@Override
		protected void okPressed(){
			Date date = dpFromDate.getDate();
			if (date == null) {
				fromDate = new TimeTool(TimeTool.BEGINNING_OF_UNIX_EPOCH);
			} else {
				fromDate = new TimeTool(date.getTime());
			}
			date = dpToDate.getDate();
			if (date == null) {
				toDate = new TimeTool(TimeTool.END_OF_UNIX_EPOCH);
			} else {
				toDate = new TimeTool(date.getTime());
			}
			super.okPressed();
		}
		
		public TimeTool getFromDate(){
			return fromDate;
		}
		
		public TimeTool getToDate(){
			return toDate;
		}
		
	}
	
	class SelectionPrintDialog extends TitleAreaDialog implements ICallback {
		private TextContainer text;
		
		public SelectionPrintDialog(final Shell shell){
			super(shell);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Control createDialogArea(final Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			text = new TextContainer(getShell());
			ret.setLayout(new FillLayout());
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
			text.getPlugin().createContainer(ret, this);
			text.getPlugin().showMenu(false);
			text.getPlugin().showToolbar(false);
			text.createFromTemplateName(
				null,
				"Liste", Brief.UNKNOWN, Hub.actUser, Messages.getString("KonsZumVerrechnenView.billsTitle")); //$NON-NLS-1$ //$NON-NLS-2$
			Tree[] all = (Tree[]) tSelection.getChildren().toArray(new Tree[0]);
			String[][] table = new String[all.length][];
			
			for (int i = 0; i < all.length; i++) {
				table[i] = new String[2];
				Tree tr = all[i];
				if (tr.contents instanceof Konsultation) {
					tr = tr.getParent();
				}
				if (tr.contents instanceof Fall) {
					tr = tr.getParent();
				}
				Patient p = (Patient) tr.contents;
				StringBuilder sb = new StringBuilder();
				sb.append(p.getLabel());
				for (Tree tFall : (Tree[]) tr.getChildren().toArray(new Tree[0])) {
					Fall fall = (Fall) tFall.contents;
					sb.append(Messages.getString("KonsZumVerrechnenView.case")).append(fall.getLabel()); //$NON-NLS-1$
					for (Tree tRn : (Tree[]) tFall.getChildren().toArray(new Tree[0])) {
						Konsultation k = (Konsultation) tRn.contents;
						sb.append(Messages.getString("KonsZumVerrechnenView.kons")).append(k.getLabel()); //$NON-NLS-1$
					}
				}
				table[i][0] = sb.toString();
			}
			text.getPlugin().setFont("Helvetica", SWT.NORMAL, 9); //$NON-NLS-1$
			text.getPlugin().insertTable("[Liste]", 0, table, new int[] { //$NON-NLS-1$
					90, 10
				});
			return ret;
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText(Messages.getString("KonsZumVerrechnenView.billsList")); //$NON-NLS-1$
			setTitle(Messages.getString("KonsZumVerrechnenView.printListCaption")); //$NON-NLS-1$
			setMessage(Messages.getString("KonsZumVerrechnenView.printListMessage")); //$NON-NLS-1$
			getShell().setSize(900, 700);
			SWTHelper.center(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell(),
				getShell());
		}
		
		//20131028js: Added this method completely to KonsZumVerrechnen-SelectionPrintDialog it does parts of what dispose() does in RezeptBlatt.java etc.
		//A similar edit has been made to EtiketteDruckenDialog.java et al.
		//This works as expected for Kontakte: Liste ausgewählter Konsultationen zum Verrechnen
		@Override
		public boolean close(){
			//Call the original overwritten close method?
			boolean ret = super.close();
			
			System.out.println("\njs ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): begin");

			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: Added a close function with at least the text.getPlugin().dispose(); functionality");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: as also added to other TextContainer clients, to ensure that TextPlugin connection/server");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: is closed again after usage. Please review other commented out content of this dispose():");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: Does GenericPrintDialog need more functionality as well?");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

			/*
			 * Other content usually found in other TextContainer using clients: commented out, but please review if we should need anything. 
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

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
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): about to txt.getPlugin().dispose()");
			text.getPlugin().dispose();		
			
			/*
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): about to GlobalEventDispatcher.removeActivationListener()...");
			GlobalEventDispatcher.removeActivationListener(this, this);
			*/

			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): about to super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
			System.out.println("js ch.elexis.views.rechnung/KonsZumVerrechnenView.java close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
		protected void okPressed(){
			super.okPressed();
		}
		
		public void save(){
			// TODO Auto-generated method stub
			
		}
		
		public boolean saveAs(){
			// TODO Auto-generated method stub
			return false;
		}
	}
}
