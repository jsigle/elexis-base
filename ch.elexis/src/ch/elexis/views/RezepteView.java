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
 *  $Id$
 *******************************************************************************/

package ch.elexis.views;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.StringConstants;
import ch.elexis.actions.CodeSelectorHandler;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.RestrictedAction;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.data.Artikel;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.ICodeElement;
import ch.elexis.data.Konsultation;
import ch.elexis.data.OutputLog;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.data.Query;
import ch.elexis.data.Rezept;
import ch.elexis.dialogs.MediDetailDialog;
import ch.elexis.exchange.IOutputter;
import ch.elexis.util.Extensions;
import ch.elexis.util.PersistentObjectDragSource;
import ch.elexis.util.PersistentObjectDropTarget;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.elexis.views.codesystems.LeistungenView;
import ch.rgw.tools.ExHandler;

/**
 * Eine View zum Anzeigen von Rezepten. Links wird eine Liste mit allen Rezepten des aktuellen
 * Patienten angezeigt, rechts die Prescriptions des aktuellen Rezepts.
 * 
 * @author Gerry
 */
public class RezepteView extends ViewPart implements IActivationListener, ISaveablePart2 {
	public static final String ID = "ch.elexis.Rezepte"; //$NON-NLS-1$
	static final String ICON = "rezept_view"; //$NON-NLS-1$
	private final FormToolkit tk = Desk.getToolkit();
	private Form master;
	TableViewer tv;
	// Label ausgestellt;
	ListViewer lvRpLines;
	private Action newRpAction, deleteRpAction;
	private Action addLineAction, removeLineAction, changeMedicationAction;
	private ViewMenus menus;
	private Action printAction;
	private Patient actPatient;
	private PersistentObjectDropTarget dropTarget;
	private final ElexisEventListenerImpl eeli_pat = new ElexisEventListenerImpl(Patient.class) {
		
		public void runInUi(ElexisEvent ev){
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
				Patient newPatient = (Patient) ev.getObject();
				if ((actPatient == null) || (!actPatient.equals(newPatient))) {
					actPatient = newPatient;
					
					ElexisEventDispatcher.getInstance().fire(
						new ElexisEvent(null, Rezept.class, ElexisEvent.EVENT_DESELECTED));
					
					addLineAction.setEnabled(false);
					printAction.setEnabled(false);
					tv.refresh(true);
					refresh();
					master.setText(actPatient.getLabel());
				}
			} else if (ev.getType() == ElexisEvent.EVENT_DESELECTED) {
				actPatient = null;
				ElexisEventDispatcher.clearSelection(Rezept.class);
				refresh();
			}
		}
	};
	
	private final ElexisEventListenerImpl eeli_rp = new ElexisEventListenerImpl(Rezept.class,
		ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_UPDATE) {
		
		public void runInUi(ElexisEvent ev){
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
				actPatient = ((Rezept) ev.getObject()).getPatient();
				refresh();
			} else if (ev.getType() == ElexisEvent.EVENT_UPDATE) {
				actPatient = ((Rezept) ev.getObject()).getPatient();
				tv.refresh(true);
			}
			
		}
	};
	
	@Override
	public void createPartControl(final Composite parent){
		Image icon = Desk.getImage(ICON);
		if (icon != null) {
			setTitleImage(icon);
		}
		parent.setLayout(new GridLayout());
		master = tk.createForm(parent);
		master.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		master.getBody().setLayout(new FillLayout());
		SashForm sash = new SashForm(master.getBody(), SWT.NONE);
		tv = new TableViewer(sash, SWT.V_SCROLL | SWT.FULL_SELECTION);
		tv.setContentProvider(new IStructuredContentProvider() {
			
			public Object[] getElements(final Object inputElement){
				Query<Rezept> qbe = new Query<Rezept>(Rezept.class);
				/*
				 * Patient act = (Patient) ElexisEventDispatcher .getSelected(Patient.class);
				 */
				if (actPatient != null) {
					qbe.add(Rezept.PATIENT_ID, Query.EQUALS, actPatient.getId());
					qbe.orderBy(true, new String[] {
						Rezept.DATE, PersistentObject.FLD_LASTUPDATE
					});
					List<Rezept> list = qbe.execute();
					return list.toArray();
				} else {
					return new Object[0];
				}
			}
			
			public void dispose(){ /* leer */
			}
			
			public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput){ /* leer */
			}
			
		});
		tv.setLabelProvider(new LabelProvider() {
			
			@Override
			public String getText(final Object element){
				if (element instanceof Rezept) {
					Rezept rp = (Rezept) element;
					return rp.getLabel();
				}
				return element.toString();
			}
			
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object )
			 */
			@Override
			public Image getImage(Object element){
				List<OutputLog> outputs = OutputLog.getOutputs((PersistentObject) element);
				if (outputs != null && outputs.size() > 0) {
					OutputLog o = outputs.get(0);
					String outputterID = o.getOutputterID();
					IOutputter io = OutputLog.getOutputter(outputterID);
					if (io != null) {
						return io.getSymbol();
					}
				}
				return null;
			}
			
		});
		tv.addSelectionChangedListener(GlobalEventDispatcher.getInstance().getDefaultListener());
		lvRpLines = new ListViewer(sash);
		makeActions();
		menus = new ViewMenus(getViewSite());
		// menus.createToolbar(newRpAction, addLineAction, printAction );
		menus.createMenu(newRpAction, addLineAction, printAction, deleteRpAction);
		menus.createViewerContextMenu(lvRpLines, removeLineAction, changeMedicationAction);
		// make selection of prescription viewer available for commands of context menu
		getSite().setSelectionProvider(lvRpLines);
		IToolBarManager tm = getViewSite().getActionBars().getToolBarManager();
		List<IAction> importers =
			Extensions.getClasses(Extensions.getExtensions("ch.elexis.RezeptHook"), //$NON-NLS-1$
				"RpToolbarAction", false); //$NON-NLS-1$
		for (IAction ac : importers) {
			tm.add(ac);
		}
		if (importers.size() > 0) {
			tm.add(new Separator());
		}
		tm.add(newRpAction);
		tm.add(addLineAction);
		tm.add(printAction);
		tv.setInput(getViewSite());
		
		/* Implementation Drag&Drop */
		PersistentObjectDropTarget.IReceiver dtr = new PersistentObjectDropTarget.IReceiver() {
			
			public boolean accept(PersistentObject o){
				// TODO Auto-generated method stub
				return true;
			}
			
			public void dropped(PersistentObject o, DropTargetEvent ev){
				Rezept actR = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
				if (actR == null) {
					SWTHelper.showError(Messages.getString("RezepteView.NoPrescriptionSelected"), //$NON-NLS-1$
						Messages.getString("RezepteView.PleaseChoosaAPrescription")); //$NON-NLS-1$
					return;
				}
				if (o instanceof Artikel) {
					Artikel art = (Artikel) o;
					
					Prescription p =
						new Prescription(art, actR.getPatient(), StringConstants.EMPTY,
							StringConstants.EMPTY);
					p.setBeginDate(null);
					actR.addPrescription(p);
					refresh();
				} else if (o instanceof Prescription) {
					Prescription pre = (Prescription) o;
					Prescription now =
						new Prescription(pre.getArtikel(), actR.getPatient(), pre.getDosis(),
							pre.getBemerkung());
					now.setBeginDate(null);
					actR.addPrescription(now);
					refresh();
				}
				
			}
		};
		
		// final TextTransfer textTransfer = TextTransfer.getInstance();
		// Transfer[] types = new Transfer[] {textTransfer};
		dropTarget = new PersistentObjectDropTarget("Rezept", lvRpLines.getControl(), dtr); //$NON-NLS-1$
		
		lvRpLines.setContentProvider(new RezeptContentProvider());
		lvRpLines.setLabelProvider(new RezeptLabelProvider());
		lvRpLines.getControl()
			.setToolTipText(Messages.getString("RezepteView.DragMedicamentsHere")); //$NON-NLS-1$
		/* lvRpLines.addDragSupport(DND.DROP_COPY,types, */
		new PersistentObjectDragSource(lvRpLines);
		lvRpLines.setInput(getViewSite());
		addLineAction.setEnabled(false);
		printAction.setEnabled(false);
		GlobalEventDispatcher.addActivationListener(this, this);
		tv.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event){
				try {
					RezeptBlatt rp = (RezeptBlatt) getViewSite().getPage().showView(RezeptBlatt.ID);
					Rezept actR = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
					Brief rpBrief = actR.getBrief();
					if (rpBrief != null) {
						// existing - just reads prescriptiom and opens RezeptBlatt
						rp.loadRezeptFromDatabase(actR, rpBrief);
					} else {
						// not existing - create prescription and opens RezeptBlatt
						rp.createRezept(actR);
					}
				} catch (Throwable ex) {
					ExHandler.handle(ex);
				}
			}
			
		});
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		tv.removeSelectionChangedListener(GlobalEventDispatcher.getInstance().getDefaultListener());
	}
	
	public void refresh(){
		Rezept rp = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
		if (rp == null) {
			lvRpLines.refresh(true);
			addLineAction.setEnabled(false);
			printAction.setEnabled(false);
		} else {
			lvRpLines.refresh(true);
			addLineAction.setEnabled(true);
			printAction.setEnabled(true);
			master.setText(rp.getPatient().getLabel());
		}
	}
	
	private void makeActions(){
		newRpAction = new Action(Messages.getString("RezepteView.newPrescriptionAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
					setToolTipText(Messages.getString("RezepteView.newPrescriptonTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					Patient act = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
					if (act == null) {
						MessageBox mb =
							new MessageBox(getViewSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
						mb.setText(Messages.getString("RezepteView.newPrescriptionError")); //$NON-NLS-1$
						mb.setMessage(Messages.getString("RezepteView.noPatientSelected")); //$NON-NLS-1$
						mb.open();
						return;
					}
					Fall fall = (Fall) ElexisEventDispatcher.getSelected(Fall.class);
					if (fall == null) {
						Konsultation k = act.getLetzteKons(false);
						if (k == null) {
							SWTHelper.alert(Messages.getString("RezepteView.noCaseSelected"), //$NON-NLS-1$
								Messages.getString("RezepteView.pleaseCreateOrChooseCase")); //$NON-NLS-1$
							return;
						} else {
							fall = k.getFall();
						}
					}
					new Rezept(act);
					tv.refresh();
				}
			};
		deleteRpAction = new Action(Messages.getString("RezepteView.deletePrescriptionActiom")) { //$NON-NLS-1$
				@Override
				public void run(){
					Rezept rp = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
					if (MessageDialog.openConfirm(
						getViewSite().getShell(),
						Messages.getString("RezepteView.deletePrescriptionActiom"), //$NON-NLS-1$
						MessageFormat.format(
							Messages.getString("RezepteView.deletePrescriptionConfirm"), rp //$NON-NLS-1$
								.getDate()))) {
						rp.delete();
						tv.refresh();
					}
				}
			};
		removeLineAction = new Action(Messages.getString("RezepteView.deleteLineAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					Rezept rp = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
					IStructuredSelection sel = (IStructuredSelection) lvRpLines.getSelection();
					Prescription p = (Prescription) sel.getFirstElement();
					if ((rp != null) && (p != null)) {
						rp.removePrescription(p);
						lvRpLines.refresh();
					}
					/*
					 * RpZeile z=(RpZeile)sel.getFirstElement(); if((rp!=null) && (z!=null)){
					 * rp.removeLine(z); lvRpLines.refresh(); }
					 */
				}
			};
		addLineAction = new Action(Messages.getString("RezepteView.newLineAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					try {
						LeistungenView lv1 =
							(LeistungenView) getViewSite().getPage().showView(LeistungenView.ID);
						CodeSelectorHandler.getInstance().setCodeSelectorTarget(dropTarget);
						CTabItem[] tabItems = lv1.ctab.getItems();
						for (CTabItem tab : tabItems) {
							ICodeElement ics = (ICodeElement) tab.getData();
							if (ics instanceof Artikel) {
								lv1.ctab.setSelection(tab);
								break;
							}
						}
					} catch (PartInitException ex) {
						ExHandler.handle(ex);
					}
				}
			};
		printAction = new Action(Messages.getString("RezepteView.printAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					try {
						RezeptBlatt rp =
							(RezeptBlatt) getViewSite().getPage().showView(RezeptBlatt.ID);
						Rezept actR = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
						Brief rpBrief = actR.getBrief();
						if (rpBrief == null)
							// not yet created - just create a new Rezept
							rp.createRezept(actR);
						else {
							// Brief for Rezept already exists:
							// ask if it should be recreated or just shown
							String[] dialogButtonLabels =
								{
									Messages.getString("RezepteView.RecreatePrescription"), Messages.getString("RezepteView.ShowPrescription"), Messages.getString("RezepteView.PrescriptionCancel") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								};
							MessageDialog msg =
								new MessageDialog(null,
									Messages.getString("RezepteView.CreatePrescription"), //$NON-NLS-1$
									null,
									Messages
										.getString("RezepteView.ReallyWantToRecreatePrescription"), //$NON-NLS-1$
									MessageDialog.WARNING, dialogButtonLabels, 2);
							int result = msg.open();
							switch (result) {
							case 0: // recreate rezept
								rp.createRezept(actR);
								break;
							case 1: // open rezept
								rp.loadRezeptFromDatabase(actR, rpBrief);
								break;
							case 2: // cancel or closebox - do nothing
								break;
							}
						}
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
				}
			};
		changeMedicationAction =
			new RestrictedAction(AccessControlDefaults.MEDICATION_MODIFY,
				Messages.getString("RezepteView.ChangeLink")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
					setToolTipText(Messages.getString("RezepteView.ChangeTooltip")); //$NON-NLS-1$
				}
				
				public void doRun(){
					Rezept rp = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
					IStructuredSelection sel = (IStructuredSelection) lvRpLines.getSelection();
					Prescription pr = (Prescription) sel.getFirstElement();
					if (pr != null) {
						new MediDetailDialog(getViewSite().getShell(), pr).open();
						lvRpLines.refresh();
					}
				}
			};
		addLineAction.setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ADDITEM));
		printAction.setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
		deleteRpAction.setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
	}
	
	public void activation(final boolean mode){
		// TODO Auto-generated method stub
		
	}
	
	public void visible(final boolean mode){
		if (mode == true) {
			ElexisEventDispatcher.getInstance().addListeners(eeli_pat, eeli_rp);
			Rezept actRezept = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
			Patient global = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
			if (global != null) {
				if ((actRezept == null) || (!actRezept.getPatient().getId().equals(global.getId()))) {
					eeli_pat.catchElexisEvent(new ElexisEvent(global, Patient.class,
						ElexisEvent.EVENT_SELECTED));
				} else {
					eeli_rp.catchElexisEvent(new ElexisEvent(actRezept, Rezept.class,
						ElexisEvent.EVENT_SELECTED));
				}
				addLineAction.setEnabled(actRezept != null);
			}
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_pat, eeli_rp);
		}
	}
	
	private static class RezeptContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(final Object inputElement){
			Rezept rp = (Rezept) ElexisEventDispatcher.getSelected(Rezept.class);
			if (rp == null) {
				return new Prescription[0];
			}
			List<Prescription> list = rp.getLines();
			return list.toArray();
		}
		
		public void dispose(){ /* leer */
		}
		
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput){ /* leer */
		}
	}
	
	private static class RezeptLabelProvider extends LabelProvider {
		
		@Override
		public String getText(final Object element){
			if (element instanceof Prescription) {
				Prescription z = (Prescription) element;
				return z.getLabel();
			}
			return "?"; //$NON-NLS-1$
		}
		
	}
	
	public void clearEvent(final Class<? extends PersistentObject> template){
		lvRpLines.refresh();
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
}
