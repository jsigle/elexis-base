/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.developer.resources.view;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListenerImpl;
import ch.elexis.actions.FlatDataLoader;
import ch.elexis.actions.GlobalEventDispatcher;
import ch.elexis.actions.RestrictedAction;
import ch.elexis.actions.GlobalEventDispatcher.IActivationListener;
import ch.elexis.actions.PersistentObjectLoader.QueryFilter;
import ch.elexis.data.Anwender;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.developer.resources.model.ACLContributor;
import ch.elexis.developer.resources.model.SampleDataType;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.rgw.tools.TimeTool;

/**
 * This is a sample view to demonstrate how to connect to elexis's event scheduler and how to
 * display elexis's data types. We implement IActivationListener to be informed, wenn the user can
 * see our View. All UI funktions should only be active in that case.
 * 
 * @author gerry
 * 
 */

public class SampleView extends ViewPart implements IActivationListener {
	/** We make a String constant for the ID to reference it from the perspective */
	public static final String ID = "ch.elexis.developer.resources.views.sample";
	
	/**
	 * CommonViewer is a "golden hammer". Use it for fast prototyping. In many cases you probably
	 * want to develop more spezialiced viewers.
	 */
	CommonViewer cv;
	
	/**
	 * A Common Viewer is always controlled by a ViewerConfigurer. So you'll never have only one of
	 * them
	 */
	ViewerConfigurer vc;
	
	/**
	 * A FlatDataLoader reads data from a table into a list of Objects
	 */
	FlatDataLoader fdl;
	
	/**
	 * Sample actions to create and delete SampleDataTypes
	 */
	private RestrictedAction newSDTAction, deleteSDTAction;
	
	/**
	 * A menu manager for the viewer's context menu
	 */
	private MenuManager contextMenu;
	/**
	 * This is a default implemetation of an ElexisEventListener. It listens for SelectionEVents and
	 * UnselectionEvents on Instances of the Patient class.
	 */
	ElexisEventListenerImpl eeli_pat = new ElexisEventListenerImpl(Patient.class) {
		/**
		 * Wen can override runInUi (which runs in the UI thread) or catchElexisEvent which runs in
		 * a non-UI thread. In any case the method must return quickly. For lenghty operations,
		 * start a seperate thread.
		 */
		
		@Override
		public void catchElexisEvent(ElexisEvent ev){
			// Patient changed, so
			// tell the FlatDataLoader to reload (which will in turn reload
			// the Viewer)
			fdl.changed(null);
			deleteSDTAction.setEnabled(false);
		}
		
	};
	
	/**
	 * This is a default implemetation of an ElexisEventListener. It listens for several types of
	 * events on Instances of the SampleDataType class.
	 */
	ElexisEventListenerImpl eeli_sdt = new ElexisEventListenerImpl(SampleDataType.class,
		ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_RELOAD | ElexisEvent.EVENT_UPDATE
			| ElexisEvent.EVENT_CREATE | ElexisEvent.EVENT_DELETE) {
		
		@Override
		public void runInUi(ElexisEvent ev){
			// do something that must run in the UI thread
			switch (ev.getType()) {
			case ElexisEvent.EVENT_SELECTED:
				// do somenthing great with selection;
				deleteSDTAction.setEnabled(true);
				break;
			case ElexisEvent.EVENT_UPDATE:
				SampleDataType sdt = (SampleDataType) ev.getObject();
				// sdt has been modified. We do react on this action here
				break;
			case ElexisEvent.EVENT_RELOAD:
			case ElexisEvent.EVENT_CREATE:
			case ElexisEvent.EVENT_DELETE:
				// We keep things simple: On all these events, just reload the
				// viewer
				fdl.changed(null);
			}
			
		}
		
	};
	
	/**
	 * As the Elexis User changes, he or she has propably different righs to see and modify our
	 * data, than the previous user. So we catch the USER_CHANGED Event and react accordingly.
	 */
	private final ElexisEventListenerImpl eeli_user = new ElexisEventListenerImpl(Anwender.class,
		ElexisEvent.EVENT_USER_CHANGED) {
		@Override
		public void runInUi(ElexisEvent ev){
			newSDTAction.reflectRight();
			deleteSDTAction.reflectRight();
			cv.getViewerWidget().refresh(true);
		}
	};
	
	/**
	 * This is the right place to create all UI elements. The parent composite already has a
	 * GridLayout.
	 */
	@Override
	public void createPartControl(Composite parent){
		makeActions();
		cv = new CommonViewer();
		Query<SampleDataType> qbe = new Query<SampleDataType>(SampleDataType.class);
		fdl = new FlatDataLoader(cv, qbe);
		fdl.addQueryFilter(new QueryFilter() {
			public void apply(Query<? extends PersistentObject> qbe){
				// We load all SampleDataTypes for the selected patient,
				// but only, if the user currently logged-in has the right to do
				// so
				if (Hub.acl.request(ACLContributor.ReadSDT)) {
					Patient pat = ElexisEventDispatcher.getSelectedPatient();
					if (pat != null) {
						qbe.add(SampleDataType.FLD_PATIENT_ID, Query.EQUALS, pat.getId());
					} else {
						qbe.insertFalse();
					}
				} else {
					qbe.insertFalse(); // so the Query will always return zero
					// objects
				}
			}
		});
		vc =
			new ViewerConfigurer(fdl, new SampleDataLabelProvider(), new SimpleWidgetProvider(
				SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE, cv));
		cv.setObjectCreateAction(getViewSite(), newSDTAction);
		cv.setContextMenu(contextMenu);
		ViewMenus viewMenus = new ViewMenus(getViewSite());
		viewMenus.createMenu(deleteSDTAction);
		cv.create(vc, parent, SWT.NONE, getViewSite());
		// At this point the viewer is created
		// We want to be informed, when the part becomes visible
		GlobalEventDispatcher.addActivationListener(this, this);
		deleteSDTAction.setEnabled(false);
	}
	
	/**
	 * On disposal,the IActivationListener MUST be unregistered. Also, our RestrictedAction must be
	 * unregistered from the AutoAdapt queue to prevent memory leaks.
	 */
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		newSDTAction.disableAutoAdapt();
		deleteSDTAction.disableAutoAdapt();
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		// Don't mind
		
	}
	
	/**
	 * From IActivationListener: the view was activated or inactivated
	 * 
	 * @param mode
	 */
	public void activation(boolean mode){
		// don't mind
		
	}
	
	/**
	 * From IActivationListener: The View changes visibility Our listeners need only to be active,if
	 * gthe view is visible. So we untregister them if the view disappears to save resources. If the
	 * view becomes visible again, we must send an event, because the listeners don't know what
	 * happend, before the start listening
	 * 
	 * @param mode
	 *            true: the view becomes visible. false: the view becomes invisible
	 */
	public void visible(boolean mode){
		if (mode) {
			eeli_pat.catchElexisEvent(ElexisEvent.createPatientEvent());
			eeli_sdt.catchElexisEvent(new ElexisEvent(null, SampleDataType.class,
				ElexisEvent.EVENT_RELOAD));
			ElexisEventDispatcher.getInstance().addListeners(eeli_pat, eeli_sdt, eeli_user);
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_pat, eeli_sdt, eeli_user);
		}
		
	}
	
	private void makeActions(){
		/**
		 * The Create Action (the green button on the upper right side of the view) can be pressed
		 * only if the user has the right to create SDT's
		 */
		newSDTAction =
			new RestrictedAction(ACLContributor.CreateSDT, Messages.SampleView_newSampleDataType) {
				
				@Override
				public void doRun(){
					Patient p = ElexisEventDispatcher.getSelectedPatient();
					if (p == null) {
						SWTHelper.showError(Messages.SampleView_PleaseSelectPatient,
							Messages.SampleView_OnlyCreateObjectsIfPatIsSelected);
					} else {
						int fun = (int) Math.round(100000 * Math.random());
						int bore = (int) Math.round(100000 * Math.random());
						/* SampleDataType sdt= */new SampleDataType(p,
							Messages.SampleView_SDTCreated
								+ new TimeTool().toString(TimeTool.FULL_GER), fun, bore);
					}
					
				}
				
			};
		newSDTAction.enableAutoAdapt();
		/**
		 * The delete Action will be placed in the contextMenu and in the View Menu.
		 */
		deleteSDTAction =
			new RestrictedAction(ACLContributor.DeleteSDT, Messages.SampleView_deleteItem) {
				
				@Override
				public void doRun(){
					Object[] ob = cv.getSelection();
					if (ob != null && ob.length > 0) {
						SampleDataType sdt = (SampleDataType) ob[0];
						sdt.delete();
					}
				}
			};
		deleteSDTAction.enableAutoAdapt();
		contextMenu = new MenuManager();
		contextMenu.add(deleteSDTAction);
	}
	
}
