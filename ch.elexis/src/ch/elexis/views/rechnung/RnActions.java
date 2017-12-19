/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    J. Sigle   - Multiple improvements in 2013
 *    			 - 201510, 201512: added Liste exportieren, um eine Rechnungsliste zu exportieren
 *    			 - 201512: Rechnungen nicht mehrfach im Verarbeitungsergebnis aufführen, wenn zuvor aufgeklappt wurde,
 *    			   und eine Rechnung auf Ebene von Patient/Fall/Rechnung effektiv bis zu 3 x markiert ist. 
 * 
 *******************************************************************************/

package ch.elexis.views.rechnung;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import ch.elexis.Desk;
import ch.elexis.ElexisException;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.RestrictedAction;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.commands.Handler;
import ch.elexis.commands.MahnlaufCommand;
import ch.elexis.data.AccountTransaction;
import ch.elexis.data.Brief;
import ch.elexis.data.Konsultation;	//201512210125js
import ch.elexis.data.Fall;
import ch.elexis.data.Patient;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.data.Zahlung;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;
import ch.elexis.views.FallDetailView;
import ch.elexis.views.PatientDetailView2;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.Tree;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;	//201512210125js
import java.util.Date;				//201512210125js

/**
 * Collection of bill-related actions
 * 
 * @author gerry
 * 
 */
public class RnActions {
	Action rnExportAction, editCaseAction, delRnAction, reactivateRnAction, patDetailAction;
	Action expandAllAction, collapseAllAction, reloadAction, mahnWizardAction;
	Action addPaymentAction, addExpenseAction, changeStatusAction, stornoAction;
	Action increaseLevelAction, printListeAction, exportListAction, rnFilterAction;							//20151013js added
	Action addAccountExcessAction;
	
	RnActions(final RechnungsListeView view){
		
		printListeAction = new Action(Messages.getString("RnActions.printListAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_PRINTER));
					setToolTipText(Messages.getString("RnActions.printListTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					Object[] sel = view.cv.getSelection();
					new RnListeDruckDialog(view.getViewSite().getShell(), sel).open();
				}
			};
 		exportListAction = new Action(Messages.getString("RnActions.exportListAction")) { //$NON-NLS-1$		//20151013js added
 				{
 					setToolTipText(Messages.getString("RnActions.exportListTooltip")); //$NON-NLS-1$
 				}
 				
 				@Override
 				public void run(){
 					Object[] sel = view.cv.getSelection();
 					new RnListeExportDialog(view.getViewSite().getShell(), sel).open();
 				}
 			};			
		mahnWizardAction = new Action(Messages.getString("RnActions.remindersAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.remindersTooltip")); //$NON-NLS-1$
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_WIZARD));
				}
				
				@Override
				public void run(){
					if (!MessageDialog.openConfirm(view.getViewSite().getShell(),
						Messages.getString("RnActions.reminderConfirmCaption"), //$NON-NLS-1$
						Messages.getString("RnActions.reminderConfirmMessage"))) { //$NON-NLS-1$
						return;
					}
					Handler.execute(view.getViewSite(), MahnlaufCommand.ID, null);
					view.cfp.clearValues();
					view.cfp.cbStat
						.setText(RnControlFieldProvider.stats[RnControlFieldProvider.stats.length - 3]);
					view.cfp.fireChangedEvent();
				}
			};
		rnExportAction = new Action(Messages.getString("RechnungsListeView.printAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RechnungsListeView.printToolTip")); //$NON-NLS-1$
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_GOFURTHER));
					/*
					 * GlobalEvents.getInstance().addSelectionListener(new
					 * GlobalEvents.SelectionListener() {
					 * 
					 * public void selectionEvent(PersistentObject obj){ if(obj instanceof Rechnung)
					 * 
					 * }
					 * 
					 * public void clearEvent(Class<? extends PersistentObject> template){ // TODO
					 * Auto-generated method stub
					 * 
					 * } })
					 */
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					new RnOutputDialog(view.getViewSite().getShell(), list).open();
				}
			};
		
		patDetailAction = new Action(Messages.getString("RnActions.patientDetailsAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					IWorkbenchPage rnPage =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						/* PatientDetailView fdv=(PatientDetailView) */rnPage
							.showView(PatientDetailView2.ID);
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
				}
				
			};
		editCaseAction = new Action(Messages.getString("RnActions.edirCaseAction")) { //$NON-NLS-1$
			
				@Override
				public void run(){
					IWorkbenchPage rnPage =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						/* FallDetailView fdv=(FallDetailView) */rnPage.showView(FallDetailView.ID);
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
				}
				
			};
		delRnAction = new Action(Messages.getString("RnActions.deleteBillAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					for (Rechnung rn : list) {
						rn.storno(true);
					}
				}
			};
		reactivateRnAction = new Action(Messages.getString("RnActions.reactivateBillAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					for (Rechnung rn : list) {
						rn.setStatus(RnStatus.OFFEN);
					}
				}
			};
		expandAllAction = new Action(Messages.getString("RnActions.expandAllAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					view.cv.getViewerWidget().getControl().setRedraw(false);
					((TreeViewer) view.cv.getViewerWidget()).expandAll();
					view.cv.getViewerWidget().getControl().setRedraw(true);
				}
			};
		collapseAllAction = new Action(Messages.getString("RnActions.collapseAllAction")) { //$NON-NLS-1$
				@Override
				public void run(){
					view.cv.getViewerWidget().getControl().setRedraw(false);
					((TreeViewer) view.cv.getViewerWidget()).collapseAll();
					view.cv.getViewerWidget().getControl().setRedraw(true);
				}
			};
		reloadAction = new Action(Messages.getString("RnActions.reloadAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.reloadTooltip")); //$NON-NLS-1$
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
				}
				
				@Override
				public void run(){
					view.cfp.fireChangedEvent();
				}
			};
		
		addPaymentAction = new Action(Messages.getString("RnActions.addBookingAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.addBookingTooltip")); //$NON-NLS-1$
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ADDITEM));
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						try {
							if (new RnDialogs.BuchungHinzuDialog(view.getViewSite().getShell(),
								actRn).open() == Dialog.OK) {
								ElexisEventDispatcher.update(actRn);
							}
						} catch (ElexisException e) {
							SWTHelper.showError("Zahlung hinzufügen ist nicht möglich",
								e.getLocalizedMessage());
						}
					}
				}
			};
		
		addExpenseAction = new Action(Messages.getString("RnActions.addFineAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REMOVEITEM));
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						try {
							if (new RnDialogs.GebuehrHinzuDialog(view.getViewSite().getShell(),
								actRn).open() == Dialog.OK) {
								ElexisEventDispatcher.update(actRn);
							}
						} catch (ElexisException e) {
							SWTHelper.showError("Zahlung hinzufügen ist nicht möglich",
								e.getLocalizedMessage());
						}
					}
				}
			};
		
		changeStatusAction =
			new RestrictedAction(AccessControlDefaults.ADMIN_CHANGE_BILLSTATUS_MANUALLY,
				Messages.getString("RnActions.changeStateAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.changeStateTooltip")); //$NON-NLS-1$
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
				}
				
				@Override
				public void doRun(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						if (new RnDialogs.StatusAendernDialog(view.getViewSite().getShell(), actRn)
							.open() == Dialog.OK) {
							ElexisEventDispatcher.update(actRn);
						}
					}
				}
			};
		stornoAction = new Action(Messages.getString("RnActions.stornoAction")) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
					setToolTipText(Messages.getString("RnActions.stornoActionTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						if (new RnDialogs.StornoDialog(view.getViewSite().getShell(), actRn).open() == Dialog.OK) {
							ElexisEventDispatcher.update(actRn);
						}
					}
				}
			};
		increaseLevelAction =
			new Action(Messages.getString("RnActions.increaseReminderLevelAction")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.increadeReminderLevelTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						switch (actRn.getStatus()) {
						case RnStatus.OFFEN_UND_GEDRUCKT:
							actRn.setStatus(RnStatus.MAHNUNG_1);
							break;
						case RnStatus.MAHNUNG_1_GEDRUCKT:
							actRn.setStatus(RnStatus.MAHNUNG_2);
							break;
						case RnStatus.MAHNUNG_2_GEDRUCKT:
							actRn.setStatus(RnStatus.MAHNUNG_3);
							break;
						default:
							SWTHelper.showInfo(
								Messages.getString("RnActions.changeStateErrorCaption"), //$NON-NLS-1$
								Messages.getString("RnActions.changeStateErrorMessage")); //$NON-NLS-1$
						}
					}
					
				}
			};
		addAccountExcessAction = new Action(Messages.getString("RnActions.addAccountGood")) { //$NON-NLS-1$
				{
					setToolTipText(Messages.getString("RnActions.addAccountGoodTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						
						// Allfaelliges Guthaben des Patienten der Rechnung als
						// Anzahlung hinzufuegen
						Fall fall = actRn.getFall();
						Patient patient = fall.getPatient();
						Money prepayment = patient.getAccountExcess();
						if (prepayment.getCents() > 0) {
							// make sure prepayment is not bigger than amount of
							// bill
							Money amount;
							if (prepayment.getCents() > actRn.getBetrag().getCents()) {
								amount = new Money(actRn.getBetrag());
							} else {
								amount = new Money(prepayment);
							}
							
							if (SWTHelper
								.askYesNo(
									Messages.getString("RnActions.transferMoneyCaption"), //$NON-NLS-1$
									"Das Konto von Patient \""
										+ patient.getLabel()
										+ "\" weist ein positives Kontoguthaben auf. Wollen Sie den Betrag von "
										+ amount.toString() + " dieser Rechnung \"" + actRn.getNr()
										+ ": " + fall.getLabel() + "\" zuweisen?")) {
								
								// remove amount from account and transfer it to the
								// bill
								Money accountAmount = new Money(amount);
								accountAmount.negate();
								new AccountTransaction(patient, null, accountAmount, null,
									"Anzahlung von Kontoguthaben auf Rechnung " + actRn.getNr());
								actRn.addZahlung(amount, "Anzahlung von Kontoguthaben", null);
							}
						}
					}
				}
			};
		rnFilterAction =
			new Action(Messages.getString("RnActions.filterListAction"), Action.AS_CHECK_BOX) { //$NON-NLS-1$
				{
					setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_FILTER));
					setToolTipText(Messages.getString("RnActions.filterLIstTooltip")); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					if (isChecked()) {
						RnFilterDialog rfd = new RnFilterDialog(view.getViewSite().getShell());
						if (rfd.open() == Dialog.OK) {
							view.cntp.setConstraints(rfd.ret);
							view.cfp.fireChangedEvent();
						}
					} else {
						view.cntp.setConstraints(null);
						view.cfp.fireChangedEvent();
					}
					
				}
			};
	}
	
	
	//201512211341js: Info: This dialog starts the generation of printed output immediately after its creation. 
	static class RnListeDruckDialog extends TitleAreaDialog implements ICallback {
		ArrayList<Rechnung> rnn;
		private TextContainer text;
		
		public RnListeDruckDialog(final Shell shell, final Object[] tree){
			super(shell);
			rnn = new ArrayList<Rechnung>(tree.length);
			for (Object o : tree) {
				if (o instanceof Tree) {
					Tree tr = (Tree) o;
					if (tr.contents instanceof Rechnung) {
						tr = tr.getParent();
					}
					if (tr.contents instanceof Fall) {
						tr = tr.getParent();
					}
					if (tr.contents instanceof Patient) {
						for (Tree tFall : (Tree[]) tr.getChildren().toArray(new Tree[0])) {
							Fall fall = (Fall) tFall.contents;
							for (Tree tRn : (Tree[]) tFall.getChildren().toArray(new Tree[0])) {
								Rechnung rn = (Rechnung) tRn.contents;
 								//201512211302js: Rechnungen sollten nicht doppelt im Verarbeitungsergebnis auftreten,
 								//nur weil aufgeklappt und dann bis zu 3x etwas vom gleichen Patienten/Fall/Rechnung markiert war.
 								if (!rnn.contains(rn)) {		//deshalb prüfen, ob die rechnung schon drin ist, bevor sie hinzugefügt wird.
 									rnn.add(rn);
 								}
							}
						}
					}
				}
			}
			
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
			text.createFromTemplateName(null,
				"Liste", Brief.UNKNOWN, Hub.actUser, Messages.getString("RnActions.bills")); //$NON-NLS-1$ //$NON-NLS-2$
			text.getPlugin()
				.insertText(
					"[Titel]", //$NON-NLS-1$
					Messages.getString("RnActions.billsListPrintetAt") + new TimeTool().toString(TimeTool.DATE_GER) + "\n", //$NON-NLS-1$ //$NON-NLS-2$
					SWT.CENTER);
			String[][] table = new String[rnn.size() + 1][];
			Money sum = new Money();
			int i;
			for (i = 0; i < rnn.size(); i++) {
				Rechnung rn = rnn.get(i);
				table[i] = new String[3];
				StringBuilder sb = new StringBuilder();
				Fall fall = rn.getFall();
				Patient p = fall.getPatient();
				table[i][0] = rn.getNr();
				sb.append(p.getLabel()).append(" - ").append(fall.getLabel()); //$NON-NLS-1$
				table[i][1] = sb.toString();
				Money betrag = rn.getBetrag();
				sum.addMoney(betrag);
				table[i][2] = betrag.getAmountAsString();
			}
			table[i] = new String[3];
			table[i][0] = ""; //$NON-NLS-1$
			table[i][1] = Messages.getString("RnActions.sum"); //$NON-NLS-1$
			table[i][2] = sum.getAmountAsString();
			text.getPlugin().setFont("Helvetica", SWT.NORMAL, 9); //$NON-NLS-1$
			text.getPlugin().insertTable("[Liste]", 0, table, new int[] { //$NON-NLS-1$
					10, 80, 10
				});
			text.getPlugin().showMenu(true);
			text.getPlugin().showToolbar(true);
			return ret;
		}
		
		@Override
		public void create(){
			super.create();
			getShell().setText(Messages.getString("RnActions.billsList")); //$NON-NLS-1$
			setTitle(Messages.getString("RnActions.printListCaption")); //$NON-NLS-1$
			setMessage(Messages.getString("RnActions.printListMessage")); //$NON-NLS-1$
			getShell().setSize(900, 700);
			SWTHelper.center(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell(),
				getShell());
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


 	
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	
 	//201512211341js: Info: This dialog starts the generation of output ONLY AFTER [OK] has been pressed. 
 	static class RnListeExportDialog extends TitleAreaDialog implements ICallback {
 		ArrayList<Rechnung> rnn;
 		private TextContainer text;
 		
 		//201512211459js: Siehe auch RechnungsDrucker.java - nach dortigem Vorbild modelliert.
 		//Zur Kontrolle es Ausgabeverzeichnisses, mit permanentem Speichern.
 		//ToDo: Durchgängig auf externe Konstanten umstellen, wie dort gezeigt, u.a. bei Hub.LocalCfg Zugriffen.
 		private Button bSaveFileAs;
 		String RnListExportDirname = Hub.localCfg.get("rechnung/RnListExportDirname", null);
 		Text tDirName;	
 		
 		public RnListeExportDialog(final Shell shell, final Object[] tree){
 			super(shell);
 			rnn = new ArrayList<Rechnung>(tree.length);
 			for (Object o : tree) {
 				if (o instanceof Tree) {
 					Tree tr = (Tree) o;
 					if (tr.contents instanceof Rechnung) {
 						tr = tr.getParent();
 					}
 					if (tr.contents instanceof Fall) {
 						tr = tr.getParent();
 					}
 					if (tr.contents instanceof Patient) {
 						for (Tree tFall : (Tree[]) tr.getChildren().toArray(new Tree[0])) {
 							Fall fall = (Fall) tFall.contents;
 							for (Tree tRn : (Tree[]) tFall.getChildren().toArray(new Tree[0])) {
 								Rechnung rn = (Rechnung) tRn.contents;
 								//201512211302js: Rechnungen sollten nicht doppelt im Verarbeitungsergebnis auftreten,
 								//nur weil aufgeklappt und dann bis zu 3x etwas vom gleichen Patienten/Fall/Rechnung markiert war.
 								if (!rnn.contains(rn)) {		//deshalb prüfen, ob die rechnung schon drin ist, bevor sie hinzugefügt wird.
 									rnn.add(rn);
 								}
 							}
 						}
 					}
 				}
 			}
 			
 		}		
 		
 		//ToDo: We probably don't need an overwriting close() method here, because we don't use the text plugin. !!!");
 		//20151013js: After copying RnListePrint to RnListeExport, removed most content from this close method.
 		//201512210059js: Improved exported fields / content, to reseble what's available in View Rechnungsdetails
 		//and meet the requirements for the exported table. 
 		@Override
 		public boolean close(){
 			//Call the original overwritten close method?
 			boolean ret = super.close();
 			
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: close(): begin");
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: !!!! ToDo: We probably don't need an overwriting close() method here, because we don't use the text plugin. !!!");
 			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: close(): about to return ret");
 			return ret;
 		}
 		
 		@SuppressWarnings("unchecked")
 		@Override
 		protected Control createDialogArea(final Composite parent){
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: createDialogArea(): begin");
 			Composite ret = new Composite(parent, SWT.NONE);
 			ret.setLayout(new FillLayout());
 			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
 	
 			
 			//201512211432js: Siehe auch Rechnungsdrucker.java public class RechnungsDrucker.createSettingsControl()
 			//TODO: Auf Konstante umstellen, dann braucht's allerdings den Austausch weiterer Module bei Installation!!!
 			System.out.println("TODO: RnActions.java.RnListeExportDialog.CreateSettingsControl(): MEHRFACH wie in RechnungsDrucker.java Auf Konstante für PreferenceConstants.RNLIST_EXPORTDIR für RnListExportDirname umstellen, dann braucht's allerdings den Austausch weiterer Module bei Installation!!!");
 			
 			//String RnListExportDirname = Hub.localCfg.get(PreferenceConstants.RNLIST_EXPORTDIR, null);
 			
 			Group cSaveCopy = new Group(ret, SWT.NONE);
 			//ToDo: Umstellen auf externe Konstante!
 			cSaveCopy.setText("Export als Tabelle in Textdatei: RnListExport-yyyyMMddhhmmss.txt, ColSep=TAB, LineSep=CR, \"um alle Felder\", Multiline-Inhalte in Feldern");
 			cSaveCopy.setLayout(new GridLayout(2, false));
 			bSaveFileAs = new Button(cSaveCopy, SWT.CHECK);
 			//ToDo: Umstellen auf externe Konstante!
 			bSaveFileAs.setText("Textdatei erstellen");
 			bSaveFileAs.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
 			//ToDo: Umstellen auf externe Konstante! - auch noch viel weiter unten
 			bSaveFileAs.setSelection(Hub.localCfg.get("rechnung/RnListExportDirname_bSaveFileAs", true));
 			bSaveFileAs.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent e){
 					Hub.localCfg.set("rechnung/RnListExportDirname_bSaveFileAs", bSaveFileAs.getSelection());
 				}
 				
 			});
 
 			Button bSelectFile = new Button(cSaveCopy, SWT.PUSH);
 			bSelectFile.setText(Messages.getString("RnActions.exportListDirName"));
 			bSelectFile.setLayoutData(SWTHelper.getFillGridData(2, false, 1, false));
 			bSelectFile.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent e){
 					DirectoryDialog ddlg = new DirectoryDialog(parent.getShell());
 					RnListExportDirname = ddlg.open();
 					if (RnListExportDirname == null) {
 						SWTHelper.alert(Messages.getString("RnActions.exportListDirNameMissingCaption"),
 								Messages.getString("RnActions.exportListDirNameMissingText"));
 					} else {
 						//ToDo: Umstellen auf externe Konstante!
 						Hub.localCfg.set("rechnung/RnListExportDirname", RnListExportDirname);
 						tDirName.setText(RnListExportDirname);
 					}
 				}
 			});
 			
 			tDirName = new Text(cSaveCopy, SWT.BORDER | SWT.READ_ONLY);
 			tDirName.setText(Hub.localCfg.get("rechnung/RnListExportDirname", "")); //$NON-NLS-1$
 			tDirName.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
 			
 			
 			
 			
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: createDialogArea(): about to return ret");
 			return ret;
 		}
 			
 		@Override
 		public void create() {
 			super.create();
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: create(): begin");
 			getShell().setText(Messages.getString("RnActions.billsList")); //$NON-NLS-1$
 			setTitle(Messages.getString("RnActions.exportListCaption")); //$NON-NLS-1$
 			setMessage(Messages.getString("RnActions.exportListMessage")); //$NON-NLS-1$
 			getShell().setSize(900, 700);
 			
 			SWTHelper.center(Hub.plugin.getWorkbench().getActiveWorkbenchWindow().getShell(),getShell());
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: create(): end");
 		}
 		
 		@Override
 		protected void okPressed(){
 			super.okPressed();
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: okPressed(): begun, post super");
 			if (Hub.localCfg.get("rechnung/RnListExportDirname_bSaveFileAs", true)) CSVWriteTable();
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: okPressed(): about to end");
 		}
 		
 		public void save(){
 			// TODO Auto-generated method stub
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: save(): begun, about to end");			
 		}
 		
 		public boolean saveAs(){
 			// TODO Auto-generated method stub
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: saveAs(): begun, about to return false");			
 			return false;
 		}
 		
 		
 		OutputStreamWriter CSVWriter;
 		
 		//201512210516js: Zur Ausgabe eines Strings mit gestrippten enthaltenen und hinzugefügten umgebenden Anführungszeichen für den CSV-Ouptut
 		private void CSVWriteStringSanitized(String s) {
 				String a=s;
 				a.replaceAll("['\"]", "_");		//Enthaltene Anführungszeichen wegersetzen
 				a.trim();
 				
 				try {
 					CSVWriter.write('"'+a+'"');	//mit umgebenden Anführungszeichen ausgeben	//Grmblfix. In Excel 2003 als ANSI interpretiert -> Umlautfehler.
 				}
 				catch ( IOException e)
 				{
 				}
 			}
 			
 		//201512211312js: Zur Ausgabe eines Spaltentrenners
 		private void CSVWriteColSep() {
 			try {
 				CSVWriter.write("\t");	//mit umgebenden Anführungszeichen ausgeben
 			}
 			catch ( IOException e)
 			{
 			}
 		}
 
 		//201512211312js: Zur Ausgabe eines Spaltentrenners
 		private void CSVWriteLineSep() {
 			try {
 				CSVWriter.write("\n");	//mit umgebenden Anführungszeichen ausgeben
 			}
 			catch ( IOException e)
 			{
 			}
 		}
 
 		//201510xxjs, 201512211312js: Produce the export table containing information about the selected bills
 		public void  CSVWriteTable() {
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: CSVWriteTable(): begin");
 
 			String RnListExportFileName = new SimpleDateFormat("'RnListExport-'yyyyMMddHHmmss'.txt'").format(new Date()); //kleines hh liefert 12h-Format...
 		
 			try {
 				System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: Trying to open File "+RnListExportFileName+" for output...");
 				
 				//Java speichert intern als UTF-16 und gibt in Elexis standardmässig UTF-8 aus.
 				//Excel (zumindest 2003) interpretiert standardmässig wohl als Windows/ANSI und liefert dann kaputte Umlaute.
 				//Das gilt für Excel 2003 via drag&drop. Beim Datei-Öffnen erscheint der Dialog mit Optinen zum Import, auch zum Zeichensatz -
 				//nur wird auf diesem Weg die Datei völlig zerhackt, weil etwas mit Tabs, Anführungszeichen, Newlines etc. gar nicht funktioniert.
 				//Also mache ich hier mal eine Umsetzung nach iso-8859-1.
 				//Wenn das NICHT nötig wäre, hätte hier gereicht: FileWriter CSVWriter; CSVWriter= new FileWriter( Dateiname );
 				//Tatsächlich liefert Excel aus einer so erzeugten Datei nun korrekte Umlatue; allerdings werden wohl andere Sonderzeichen verloren gehen.
 				//N.B.: Auch beim EINlesen sollte man sogleich eine Formatumsetzung auf diesem Wege mit einplanen.
 				CSVWriter = new OutputStreamWriter(new FileOutputStream( RnListExportDirname+"/"+RnListExportFileName),"Cp1252");
 			
 				//201512211328js: Output Table Headers
 				
 				CSVWriteStringSanitized("Aktion?"); CSVWriteColSep();				//201512210402js: Leere Spalte zum Eintragen der gewünschten Aktion.
 				CSVWriteStringSanitized("Re.Nr"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.DatumRn"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.DatumVon"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.DatumBis"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Garant"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Total"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Offen"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.StatusLastUpdate"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Status"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.StatusIsActive"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.StatusText"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.StatusChanges"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Rejecteds"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Outputs"); CSVWriteColSep();
 				CSVWriteStringSanitized("Re.Payments"); CSVWriteColSep();
 				CSVWriteStringSanitized("Fall.AbrSystem"); CSVWriteColSep();
 				CSVWriteStringSanitized("Fall.Bezeichnung"); CSVWriteColSep();
 				CSVWriteStringSanitized("Fall.Grund"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.Nr"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.Name"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.Vorname"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.GebDat"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.LztKonsDat"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.Balance"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.GetAccountExcess"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.BillSummary.Total."); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.BillSummary.Paid"); CSVWriteColSep();
 				CSVWriteStringSanitized("Pat.BillSummary.Open");
 				CSVWriteLineSep();
 								
 				//201512211340js: Produce one line for every rn in rnn
 				int i;
 				for (i = 0; i < rnn.size(); i++) {
 					Rechnung rn = rnn.get(i);
 					Fall fall = rn.getFall();
 					Patient p = fall.getPatient();
 					
 					//201512210402js: Leere Spalte zum Eintragen der gewünschten Aktion.
 					//Wenn die Aktion ganz vorne steht, reicht es später einmal, diese einzulesen, um zu wissen, ob man den Rest der Zeile verwerfen kann :-)
 					
 					System.out.print("");
 					CSVWriteColSep();
 					
 					//201512210348js: Erst alles zur betroffenen Rechnung...
 		
 					CSVWriteStringSanitized(rn.getNr());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(rn.getDatumRn());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(rn.getDatumVon());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(rn.getDatumBis());
 					CSVWriteColSep();
 									
 					//Siehe für die Quellen von Rechnungsempfaenger und Status-/-Changes auch RechnungsBlatt.java
 					//System.out.print("ToDo:RnEmpfaenger");
 					CSVWriteStringSanitized(fall.getGarant().getLabel());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(rn.getBetrag().toString());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(rn.getOffenerBetrag().toString());
 					CSVWriteColSep();				
 					
 						{
 						long luTime=rn.getLastUpdate();
 						Date date=new Date(luTime);
 						//ToDo: Support other date formats based upon location or configured settings
 				        SimpleDateFormat df2 = new SimpleDateFormat("dd.MM.yyyy");
 				        String dateText = df2.format(date);
 				        CSVWriteStringSanitized(dateText.toString());
 						CSVWriteColSep();
 						
 						
 						int st=rn.getStatus();
 						CSVWriteStringSanitized(Integer.toString(st));
 						CSVWriteColSep();
 						if (RnStatus.isActive(st)) {
 							CSVWriteStringSanitized("True");
 						}
 						else {
 							CSVWriteStringSanitized("False");
 						}
 						CSVWriteColSep();
 						CSVWriteStringSanitized(RnStatus.getStatusText(st));
 						CSVWriteColSep();
 						//System.out.print(rn.getStatusAtDate(now));
 						//CSVWriteColSep();
 						}	
 						
 						
 					// 201512210310js: New: produce 4 fields, each with multiline content.
 					
 					{
 						List<String> statuschgs = rn.getTrace(Rechnung.STATUS_CHANGED);
 						//Kann leer sein, oder Liefert Ergebnisse wie:
 						//[tt.mm.yyyy, hh:mm:ss: s, tt.mm.yy, hh:mm:ss: s, tt.mm.yy, hh:mm:ss: s]
 						
 						String a=statuschgs.toString();
 						if (a!=null && a.length()>1) {
 							//Die Uhrzeiten rauswerfen:
 							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
 							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
 							a=a.replaceAll(", ", "\n");
 							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
 							CSVWriteStringSanitized(a.substring(1,a.length()-1));
 						}
 						CSVWriteColSep();
 					}
 					
 					{
 						if (rn.getStatus() == RnStatus.FEHLERHAFT) {
 							List<String> rejects = rn.getTrace(Rechnung.REJECTED);
 				
 							String a=rejects.toString();
 							if (a!=null && a.length()>1) {
 								//Die Uhrzeiten rauswerfen:
 								a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
 								//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
 								a=a.replaceAll(", ", "\n");
 								//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
 								CSVWriteStringSanitized(a.substring(1,a.length()-1));
 							}
 						}
 						CSVWriteColSep();
 					}
 		
 					{
 						List<String> outputs = rn.getTrace(Rechnung.OUTPUT);
 							
 						String a=outputs.toString();
 						if (a!=null && a.length()>1) {
 							//Die Uhrzeiten rauswerfen:
 							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
 							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
 							a=a.replaceAll(", ", "\n");
 							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
 							CSVWriteStringSanitized(a.substring(1,a.length()-1));
 						}
 						CSVWriteColSep();
 					}
 					
 					{
 						List<String> payments = rn.getTrace(Rechnung.PAYMENT);
 						String a=payments.toString();
 						if (a!=null && a.length()>1) {
 							//Die Uhrzeiten rauswerfen:
 							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
 							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
 							a=a.replaceAll(", ", "\n");
 							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
 							CSVWriteStringSanitized(a.substring(1,a.length()-1));
 						}
 						CSVWriteColSep();
 					}
 					
 					//201512210348js: Jetzt alles zum betroffenen Fall:
 					CSVWriteStringSanitized(fall.getAbrechnungsSystem());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(fall.getBezeichnung());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(fall.getGrund());
 					CSVWriteColSep();
 					
 
 					//201512210348js: Jetzt alles zum betroffenen Patienten:
 					
 					//System.out.print(p.getId());
 					//CSVWriteColSep();
 					CSVWriteStringSanitized(p.getKuerzel());	//Das liefert die "Patientennummer, da sie frei eingebbar ist, gebe ich sie sanitized aus.
 					CSVWriteColSep();
 					CSVWriteStringSanitized(p.getName());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(p.getVorname());
 					CSVWriteColSep();
 					CSVWriteStringSanitized(p.getGeburtsdatum());
 					CSVWriteColSep();
 					
 					{
 						//ToDo: allenfalls wieder: auf n.a. oder so setzen...
 						//ToDo: Ich möcht aber wissen, ob p (dürfte eigentlich nie der Fall sein) oder nk schuld sind, wenn nichts rauskommt.
 						//ToDo: Na ja, eigentlich würd ich noch lieber wissen, WARUM da manchmal nichts rauskommt, obwohl eine kons sicher vhd ist.
 						String lkDatum = "p==null";
 						if (p!=null)	{
 							Konsultation lk=p.getLetzteKons(false);
 							if (lk!=null) {lkDatum=(lk.getDatum());} else {lkDatum="lk==null";}
 							//201512210211js: Offenbar manchmal n.a. - vielleicht heisst das: Kein offener Fall mit Kons? Denn berechnet wurde ja etwas!
 						}
 						CSVWriteStringSanitized(lkDatum);
 						CSVWriteColSep();
 					}
 					
 					//201512210134js: Money p.getKontostand() und String p.getBalance() liefern (bis auf den Variablentyp) das gleiche Ergebnis
 					//System.out.print(p.getKontostand());
 					//CSVWriteColSep();
 					CSVWriteStringSanitized(p.getBalance());		//returns: String
 					CSVWriteColSep();
 					CSVWriteStringSanitized(p.getAccountExcess().toString());	//returns: Money
 					CSVWriteColSep();
 					
 					//201512210146js: Das Folgende ist aus BillSummary - dort wird dafür keine Funktion bereitgestellt,
 					//ToDo: Prüfen, ob das eine Redundanz DORT und HIER ist vs. obenn erwähnter getKontostand(), getAccountExcess() etc.
 					// maybe called from foreign thread
 					{
 						String totalText = ""; //$NON-NLS-1$
 						String paidText = ""; //$NON-NLS-1$
 						String openText = ""; //$NON-NLS-1$
 						
 						//Davon, dass p != null ist, darf man eigentlich ausgehen, da ja Rechnungen zu p gehören etc.
 						if (p!= null) {
 							Money total = new Money(0);
 							Money paid = new Money(0);
 							
 							List<Rechnung> rechnungen = p.getRechnungen();
 							for (Rechnung rechnung : rechnungen) {
 								// don't consider canceled bills
 								if (rechnung.getStatus() != RnStatus.STORNIERT) {
 									total.addMoney(rechnung.getBetrag());
 									for (Zahlung zahlung : rechnung.getZahlungen()) {
 										paid.addMoney(zahlung.getBetrag());
 									}
 								}
 							}
 							
 							Money open = new Money(total);
 							open.subtractMoney(paid);
 							
 							totalText = total.toString();
 							paidText = paid.toString();
 							openText = open.toString();
 						}
 						
 						CSVWriteStringSanitized(totalText);
 						CSVWriteColSep();
 						CSVWriteStringSanitized(paidText);
 						CSVWriteColSep();
 						CSVWriteStringSanitized(openText);
 						//CSVWriteColSep();		//Nach der letzten Spalte: bitte auch kein TAB mehr ausgeben.
 					}
 					
 					//Alle Felder zu dieser Rechnung wurden geschrieben - Zeile ist fertig.
 					CSVWriteLineSep();
 				}
 
 			}
 			catch ( IOException e)
 			{
 			}
 			finally
 			{
 			    try
 			    {
 			        if ( CSVWriter != null) {
 			        	System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: Trying to close File "+RnListExportFileName+".");
 						CSVWriter.close( );
 			        }
 			    }
 			    catch ( IOException e)
 			    {
 			    }
 			}
 			
 			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: CSVWriteTable(): begin");
 		}
 	}
 	
}
