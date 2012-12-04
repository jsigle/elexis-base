/*******************************************************************************
 * Copyright (c) 2007-2011, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/
package ch.elexis.agenda.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.StringConstants;
import ch.elexis.actions.Activator;
import ch.elexis.actions.AgendaActions;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.actions.Heartbeat.HeartListener;
import ch.elexis.actions.IBereichSelectionEvent;
import ch.elexis.agenda.BereichSelectionHandler;
import ch.elexis.agenda.Messages;
import ch.elexis.agenda.acl.ACLContributor;
import ch.elexis.agenda.data.ICalTransfer;
import ch.elexis.agenda.data.IPlannable;
import ch.elexis.agenda.data.Termin;
import ch.elexis.agenda.preferences.PreferenceConstants;
import ch.elexis.agenda.ui.BereichMenuCreator;
import ch.elexis.agenda.util.Plannables;
import ch.elexis.data.Anwender;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktSelektor;
import ch.elexis.dialogs.TagesgrenzenDialog;
import ch.elexis.dialogs.TerminDialog;
import ch.elexis.dialogs.TerminListeDruckenDialog;
import ch.elexis.dialogs.TermineDruckenDialog;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

public abstract class BaseAgendaView extends ViewPart implements HeartListener,
		IActivationListener, IBereichSelectionEvent {
	
	// protected Synchronizer pinger;
	protected SelectionListener sListen = new SelectionListener();
	TableViewer tv;
	BaseAgendaView self;
	protected IAction newTerminAction, blockAction, terminKuerzenAction, terminVerlaengernAction,
			terminAendernAction;
	protected IAction dayLimitsAction, newViewAction, printAction, exportAction, importAction,
			newTerminForAction;
	protected IAction printPatientAction;
	private BereichMenuCreator bmc = new BereichMenuCreator();
	MenuManager menu = new MenuManager();
	protected Log log = Log.get("Agenda"); //$NON-NLS-1$
	Activator agenda = Activator.getDefault();
	
	private final ElexisEventListenerImpl eeli_termin = new ElexisEventListenerImpl(Termin.class,
		ElexisEvent.EVENT_RELOAD) {
		public void runInUi(ElexisEvent ev){
			if (!tv.getControl().isDisposed()) {
				tv.refresh(true);
			}
			
		}
	};
	
	private final ElexisEventListenerImpl eeli_user = new ElexisEventListenerImpl(Anwender.class,
		ElexisEvent.EVENT_USER_CHANGED) {
		public void runInUi(ElexisEvent ev){
			updateActions();
			if (tv != null) {
				if (!tv.getControl().isDisposed()) {
					tv.getControl().setFont(
						Desk.getFont(ch.elexis.preferences.PreferenceConstants.USR_DEFAULTFONT));
				}
			}
			setBereich(Hub.userCfg.get(PreferenceConstants.AG_BEREICH, agenda.getActResource()));
			
		}
	};
	private IMenuManager mgr;
	private IAction bereichMenu;
	
	protected BaseAgendaView(){
		self = this;
		BereichSelectionHandler.addBereichSelectionListener(this);
	}
	
	abstract public void create(Composite parent);
	
	@Override
	public void createPartControl(Composite parent){
		setBereich(agenda.getActResource());
		create(parent);
		makeActions();
		tv.setContentProvider(new AgendaContentProvider());
		tv.setUseHashlookup(true);
		tv.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				IPlannable pl = getSelection();
				if (pl == null) {
					newTerminAction.run();
				} else {
					TerminDialog dlg = new TerminDialog(pl);
					dlg.open();
					tv.refresh(true);
				}
			}
			
		});
		
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager){
				if (ElexisEventDispatcher.getSelected(Termin.class) == null) {
					manager.add(newTerminAction);
					manager.add(blockAction);
				} else {
					manager.add(AgendaActions.terminStatusAction);
					manager.add(terminKuerzenAction);
					manager.add(terminVerlaengernAction);
					manager.add(terminAendernAction);
					manager.add(AgendaActions.delTerminAction);
				}
				updateActions();
			}
			
		});
		
		Menu cMenu = menu.createContextMenu(tv.getControl());
		tv.getControl().setMenu(cMenu);
		
		// GlobalEvents.getInstance().addBackingStoreListener(this);
		GlobalEventDispatcher.addActivationListener(this, getViewSite().getPart());
		tv.setInput(getViewSite());
		// pinger=new ch.elexis.actions.Synchronizer();
		updateActions();
	}
	
	public IPlannable getSelection(){
		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
		if ((sel == null || (sel.isEmpty()))) {
			return null;
		} else {
			IPlannable pl = (IPlannable) sel.getFirstElement();
			return pl;
		}
	}
	
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, getViewSite().getPart());
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	public void heartbeat(){
		log.log("Heartbeat", Log.DEBUGMSG); //$NON-NLS-1$
		eeli_termin.catchElexisEvent(new ElexisEvent(null, Termin.class, ElexisEvent.EVENT_RELOAD));
		// GlobalEvents.getInstance().fireUpdateEvent(Termin.class);
		// pinger.doSync();
	}
	
	public void activation(boolean mode){/* leer */
	}
	
	public void visible(boolean mode){
		if (mode == true) {
			Hub.heart.addListener(this);
			tv.addSelectionChangedListener(sListen);
			ElexisEventDispatcher.getInstance().addListeners(eeli_termin, eeli_user);
			heartbeat();
			updateActions();
		} else {
			Hub.heart.removeListener(this);
			tv.removeSelectionChangedListener(sListen);
			ElexisEventDispatcher.getInstance().removeListeners(eeli_termin, eeli_user);
		}
		
	};
	
	public void setBereich(String b){
		agenda.setActResource(b);
	}
	
	public abstract void setTermin(Termin t);
	
	class AgendaContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement){
			if (Hub.acl.request(ACLContributor.DISPLAY_APPOINTMENTS)) {
				return Plannables.loadDay(agenda.getActResource(), agenda.getActDate());
			} else {
				return new Object[0];
			}
			
		}
		
		public void dispose(){ /* leer */
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput){/* leer */
		}
		
	};
	
	class SelectionListener implements ISelectionChangedListener {
		
		StructuredViewer sv;
		
		public void selectionChanged(SelectionChangedEvent event){
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			if ((sel == null) || sel.isEmpty()) {
				ElexisEventDispatcher.clearSelection(Termin.class);
			} else {
				Object o = sel.getFirstElement();
				if (o instanceof Termin) {
					setTermin((Termin) o);
				} else if (o instanceof Termin.Free) {
					ElexisEventDispatcher.clearSelection(Termin.class);
				}
			}
		}
	}
	
	protected void updateActions(){
		dayLimitsAction.setEnabled(Hub.acl.request(ACLContributor.CHANGE_DAYSETTINGS));
		boolean canChangeAppointments = Hub.acl.request(ACLContributor.CHANGE_APPOINTMENTS);
		newTerminAction.setEnabled(canChangeAppointments);
		terminKuerzenAction.setEnabled(canChangeAppointments);
		terminVerlaengernAction.setEnabled(canChangeAppointments);
		terminAendernAction.setEnabled(canChangeAppointments);
		AgendaActions.updateActions();
	}
	
	protected void makeActions(){
		dayLimitsAction = new Action(Messages.BaseAgendaView_dayLimits) {
			@Override
			public void run(){
				new TagesgrenzenDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell(), agenda.getActDate().toString(TimeTool.DATE_COMPACT),
					agenda.getActResource()).open();
				tv.refresh(true);
			}
		};
		dayLimitsAction.setId("ch.elexis.agenda.actions.dayLimitsAction");
		
		blockAction = new Action(Messages.TagesView_lockPeriod) {
			@Override
			public void run(){
				IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
				if (sel != null && !sel.isEmpty()) {
					IPlannable p = (IPlannable) sel.getFirstElement();
					if (p instanceof Termin.Free) {
						new Termin(agenda.getActResource(), agenda.getActDate().toString(
							TimeTool.DATE_COMPACT), p.getStartMinute(), p.getDurationInMinutes()
							+ p.getStartMinute(), Termin.typReserviert(), Termin.statusLeer());
						ElexisEventDispatcher.reload(Termin.class);
					}
				}
				
			}
		};
		terminAendernAction = new Action(Messages.TagesView_changeTermin) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
				setToolTipText(Messages.TagesView_changeThisTermin);
			}
			
			@Override
			public void run(){
				TerminDialog dlg =
					new TerminDialog((Termin) ElexisEventDispatcher.getSelected(Termin.class));
				dlg.open();
				if (tv != null) {
					tv.refresh(true);
				}
			}
		};
		terminKuerzenAction = new Action(Messages.TagesView_shortenTermin) {
			@Override
			public void run(){
				Termin t = (Termin) ElexisEventDispatcher.getSelected(Termin.class);
				if (t != null) {
					t.setDurationInMinutes(t.getDurationInMinutes() >> 1);
					ElexisEventDispatcher.reload(Termin.class);
				}
			}
		};
		terminVerlaengernAction = new Action(Messages.TagesView_enlargeTermin) {
			@Override
			public void run(){
				Termin t = (Termin) ElexisEventDispatcher.getSelected(Termin.class);
				if (t != null) {
					agenda.setActDate(t.getDay());
					Termin n =
						Plannables.getFollowingTermin(agenda.getActResource(), agenda.getActDate(),
							t);
					if (n != null) {
						t.setEndTime(n.getStartTime());
						// t.setDurationInMinutes(t.getDurationInMinutes()+15);
						ElexisEventDispatcher.reload(Termin.class);
					}
				}
			}
		};
		newTerminAction = new Action(Messages.TagesView_newTermin) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				setToolTipText(Messages.TagesView_createNewTermin);
			}
			
			@Override
			public void run(){
				TerminDialog dlg = new TerminDialog(null);
				dlg.open();
				if (tv != null) {
					tv.refresh(true);
				}
			}
		};
		
		newTerminForAction = new Action("Neuer Termin für...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				setToolTipText("Dialog zum Auswählen eines Kontakts für den Termin öffnen");
			}
			
			@Override
			public void run(){
				KontaktSelektor ksl =
					new KontaktSelektor(getSite().getShell(), Kontakt.class, "Terminvergabe",
						"Bitte wählen Sie aus, wer einen Termin braucht", Kontakt.DEFAULT_SORT);
				IPlannable sel = getSelection();
				TerminDialog dlg = new TerminDialog(null);
				dlg.open();
				if (tv != null) {
					tv.refresh(true);
				}
			}
			
		};
		printAction = new Action(Messages.BaseAgendaView_printDayList) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
				setToolTipText(Messages.BaseAgendaView_printListOfDay);
			}
			
			@Override
			public void run(){
				IPlannable[] liste =
					Plannables.loadDay(agenda.getActResource(), agenda.getActDate());
				TerminListeDruckenDialog dlg =
					new TerminListeDruckenDialog(getViewSite().getShell(), liste);
				dlg.open();
				if (tv != null) {
					tv.refresh(true);
				}
			}
		};
		printPatientAction = new Action(Messages.BaseAgendaView_printPatAppointments) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
				setToolTipText(Messages.BaseAgendaView_printFutureAppsOfSelectedPatient);
			}
			
			@Override
			public void run(){
				Patient patient = ElexisEventDispatcher.getSelectedPatient();
				if (patient != null) {
					Query<Termin> qbe = new Query<Termin>(Termin.class);
					qbe.add(Termin.FLD_PATIENT, Query.EQUALS, patient.getId());
					qbe.add(PersistentObject.FLD_DELETED, Query.NOT_EQUAL, StringConstants.ONE);
					qbe.add(Termin.FLD_TAG, Query.GREATER_OR_EQUAL,
						new TimeTool().toString(TimeTool.DATE_COMPACT));
					qbe.orderBy(false, Termin.FLD_TAG, Termin.FLD_BEGINN);
					java.util.List<Termin> list = qbe.execute();
					if (list != null) {
						boolean directPrint =
							Hub.localCfg.get(
								PreferenceConstants.AG_PRINT_APPOINTMENTCARD_DIRECTPRINT,
								PreferenceConstants.AG_PRINT_APPOINTMENTCARD_DIRECTPRINT_DEFAULT);
						
						TermineDruckenDialog dlg =
							new TermineDruckenDialog(getViewSite().getShell(),
								list.toArray(new Termin[0]));
						if (directPrint) {
							dlg.setBlockOnOpen(false);
							dlg.open();
							if (dlg.doPrint()) {
								dlg.close();
							} else {
								SWTHelper.alert(Messages.BaseAgendaView_errorWhileprinting,
									Messages.BaseAgendaView_errorHappendPrinting);
							}
						} else {
							dlg.setBlockOnOpen(true);
							dlg.open();
						}
					}
				}
			}
		};
		exportAction = new Action(Messages.BaseAgendaView_exportAgenda) {
			{
				setToolTipText(Messages.BaseAgendaView_exportAppointsments);
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_GOFURTHER));
			}
			
			@Override
			public void run(){
				ICalTransfer ict = new ICalTransfer();
				ict.doExport(agenda.getActDate(), agenda.getActDate(), agenda.getActResource());
			}
		};
		
		importAction = new Action(Messages.BaseAgendaView_importAgenda) {
			{
				setToolTipText(Messages.BaseAgendaView_importFromIcal);
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
			}
			
			@Override
			public void run(){
				ICalTransfer ict = new ICalTransfer();
				ict.doImport(agenda.getActResource());
			}
		};
		bereichMenu = new Action(Messages.TagesView_bereich, Action.AS_DROP_DOWN_MENU) {
			Menu mine;
			{
				setToolTipText(Messages.TagesView_selectBereich);
				setMenuCreator(bmc);
			}
			
		};
		
		mgr = getViewSite().getActionBars().getMenuManager();
		mgr.add(bereichMenu);
		mgr.add(dayLimitsAction);
		mgr.add(newViewAction);
		mgr.add(exportAction);
		mgr.add(importAction);
		mgr.add(printAction);
		mgr.add(printPatientAction);
	}
	
	@Override
	public void bereichSelectionEvent(String bereich){
		setPartName("Agenda " + bereich);
		eeli_termin.catchElexisEvent(new ElexisEvent(null, Termin.class, ElexisEvent.EVENT_RELOAD));
	}
}
