/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis; Portions (c) 2013 Joerg M. Sigle www.jsigle.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    J. Sigle   - added closePreExistingViewToEnsureOfficeCanHandleNewContentProperly and TerminateListener via Bootstrap.java;  
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.views;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import ch.elexis.Desk;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.data.AUF;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.dialogs.EditAUFDialog;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.rgw.tools.ExHandler;

/**
 * Arbeitsunfähigkeitszeugnisse erstellen und verwalten.
 * 
 * @author gerry
 * 
 */
public class AUF2 extends ViewPart implements IActivationListener {
	public static final String ID = "ch.elexis.auf"; //$NON-NLS-1$
	private static final String ICON = "auf_view"; //$NON-NLS-1$
	TableViewer tv;
	private Action newAUF, delAUF, modAUF, printAUF;
	private ElexisEventListener eli_auf = new ElexisEventListenerImpl(AUF.class) {
		
		public void runInUi(ElexisEvent ev){
			boolean bSelect = (ev.getType() == ElexisEvent.EVENT_SELECTED);
			modAUF.setEnabled(bSelect);
			delAUF.setEnabled(bSelect);
		}
	};
	private ElexisEventListener eli_pat = new ElexisEventListenerImpl(Patient.class) {
		
		public void runInUi(ElexisEvent ev){
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
				tv.refresh();
				ElexisEventDispatcher.clearSelection(AUF.class);
				newAUF.setEnabled(true);
			} else {
				newAUF.setEnabled(false);
				modAUF.setEnabled(false);
				delAUF.setEnabled(false);
				
			}
		}
	};
	
	public AUF2(){
		setTitleImage(Desk.getImage(ICON));
	}
	
	/*
	 * 20131026js: First check if a View for documents of the same type (e.g. "Briefe") is already open.
	 * If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
	 * See BriefAuswahl.java/TextView.java for additional information.
	 */			
	private void  closePreExistingViewToEnsureOfficeCanHandleNewContentProperly() {
	//Import org.eclipse.ui.IWorkbenchPage and ...PlatformUI and ...IViewPart only for this: 
	System.out.println("js ch.elexis.views/AUF2.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): begin");
	IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	IViewPart wbpAUFZeugnisViewPart = wbp.findView(AUFZeugnis.ID); 
	if (wbpAUFZeugnisViewPart != null) {
		System.out.println("js ch.elexis.views/AUF2.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): About to wbp.hideView(wbpAUFZeugnisViewPart)");
		wbp.hideView(wbpAUFZeugnisViewPart); 
		System.out.println("js ch.elexis.views/AUF2.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): Returned from wbp.hideView(wbpAUFZeugnisViewPart)");
	} else {
		//No preexisting populated viewPart (="View AUFZeugnis" in Elexis manual terminology) needed closing. Do nothing.
		System.out.println("js ch.elexis.views/AUF2.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): NO matching wbpAUFZeugnisViewPart found - Nothing to do.");
	} //if wbpAUFZeugnisView != null ; oder if tv!=null
	
	System.out.println("js ch.elexis.views/AUF2.java: closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(): end");
	}
	
	@Override
	public void createPartControl(Composite parent){
		// setTitleImage(Desk.getImage(ICON));
		setPartName(Messages.getString("AUF2.certificate")); //$NON-NLS-1$
		tv = new TableViewer(parent);
		tv.setLabelProvider(new DefaultLabelProvider());
		tv.setContentProvider(new AUFContentProvider());
		makeActions();
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createMenu(newAUF, delAUF, modAUF, printAUF);
		menus.createToolbar(newAUF, delAUF, printAUF);
		tv.setUseHashlookup(true);
		GlobalEventDispatcher.addActivationListener(this, this);
		tv.addSelectionChangedListener(GlobalEventDispatcher.getInstance().getDefaultListener());
		tv.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				modAUF.run();
			}
		});
		tv.setInput(getViewSite());
	}
	
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
	}
	
	@Override
	public void setFocus(){
		// TODO Auto-generated method stub
		
	}
	
	private void makeActions(){
		newAUF = new Action(Messages.getString("AUF2.new")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
					setToolTipText(Messages.getString("AUF2.createNewCert")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					Patient pat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
					if (pat == null) {
						SWTHelper.showError(Messages.getString("AUF2.NoPatientSelected"), //$NON-NLS-1$
							Messages.getString("AUF2.PleaseDoSelectPatient")); //$NON-NLS-1$
						return;
					}
					Konsultation kons =
						(Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
					Fall fall = null;
					if (kons != null) {
						fall = kons.getFall();
						if (fall == null) {
							SWTHelper
								.showError(
									Messages.getString("AUF2.noCaseSelected"), Messages.getString("AUF2.selectCase")); //$NON-NLS-1$ //$NON-NLS-2$
							return;
							
						}
						if (!fall.getPatient().equals(pat)) {
							kons = null;
						}
					}
					if (kons == null) {
						kons = pat.getLetzteKons(false);
						if (kons == null) {
							SWTHelper
								.showError(
									Messages.getString("AUF2.noCaseSelected"), Messages.getString("AUF2.selectCase")); //$NON-NLS-1$ //$NON-NLS-2$
							return;
						}
						fall = kons.getFall();
					}
					new EditAUFDialog(getViewSite().getShell(), null, fall).open();
					tv.refresh(false);
				}
			};
		delAUF = new Action(Messages.getString("AUF2.delete")) { //$NON-NLS-1$
				//TODO: 20131027js: ggf. dafür sorgen, dass eine Textplugin-Editor-View, die zu entfernendes Dokument zeigt, auch geschlossen wird.
				//TODO: 20131027js: ggf. über alle Textplugin clients homogenisieren: Mal heisst es deleteRpAction, mal removeAction...

				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
					setToolTipText(Messages.getString("AUF2.deleteCertificate")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					AUF sel = getSelectedAUF();
					if (sel != null) {
						if (MessageDialog
							.openConfirm(
								getViewSite().getShell(),
								Messages.getString("AUF2.deleteReally"), Messages.getString("AUF2.doyoywantdeletereally"))) { //$NON-NLS-1$ //$NON-NLS-2$
							sel.delete();
							tv.refresh(false);
						}
					}
				}
			};
		modAUF = new Action(Messages.getString("AUF2.edit")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
					setToolTipText(Messages.getString("AUF2.editCertificate")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					AUF sel = getSelectedAUF();
					if (sel != null) {
						new EditAUFDialog(getViewSite().getShell(), sel, sel.getFall()).open();
						tv.refresh(true);
					}
				}
			};
		printAUF = new Action(Messages.getString("AUF2.print")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
					setToolTipText(Messages.getString("AUF2.createPrint")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					//20131026js: First check if a View for documents of the same type is already open.
					//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
					closePreExistingViewToEnsureOfficeCanHandleNewContentProperly();

					try {
						AUFZeugnis az =
							(AUFZeugnis) getViewSite().getPage().showView(AUFZeugnis.ID);
						AUF actAUF =
							(ch.elexis.data.AUF) ElexisEventDispatcher.getSelected(AUF.class);
						az.createAUZ(actAUF);
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
					
				}
			};
	}
	
	private ch.elexis.data.AUF getSelectedAUF(){
		IStructuredSelection sel = (IStructuredSelection) tv.getSelection();
		if ((sel == null) || (sel.isEmpty())) {
			return null;
		}
		return (AUF) sel.getFirstElement();
	}
	
	class AUFContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement){
			Patient pat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
			if (pat == null) {
				return new Object[0];
			}
			Query<AUF> qbe = new Query<AUF>(AUF.class);
			qbe.add(AUF.FLD_PATIENT_ID, Query.EQUALS, pat.getId());
			qbe.orderBy(true, AUF.FLD_DATE_FROM, AUF.FLD_DATE_UNTIL);
			List<AUF> list = qbe.execute();
			return list.toArray();
		}
		
		public void dispose(){ /* leer */
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
			/* leer */
		}
		
	}
	
	public void activation(boolean mode){ /* egal */
	}
	
	public void visible(boolean mode){
		if (mode) {
			ElexisEventDispatcher.getInstance().addListeners(eli_auf, eli_pat);
			eli_pat.catchElexisEvent(new ElexisEvent(ElexisEventDispatcher
				.getSelected(Patient.class), null, ElexisEvent.EVENT_SELECTED));
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eli_auf, eli_pat);
		}
	}
	
}
