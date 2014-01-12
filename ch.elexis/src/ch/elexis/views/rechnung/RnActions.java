/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.views.rechnung;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
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
import ch.elexis.data.Fall;
import ch.elexis.data.Patient;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.text.TextContainer;
import ch.elexis.text.ITextPlugin.ICallback;
import ch.elexis.util.SWTHelper;
import ch.elexis.views.FallDetailView;
import ch.elexis.views.PatientDetailView2;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.Tree;

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
	Action increaseLevelAction, printListeAction, rnFilterAction;
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
								rnn.add(rn);
							}
						}
					}
				}
			}
			
		}
		
		//20131028js: Added this method completely to EtiketteDruckenDialog, it does parts of what dispose() does in RezeptBlatt.java etc.
		//A similar edit has been made to GenericPrintDialog.java and EtiketteDruckenDialog.java; however, there, the affected class is in its own module file.
		//This works as expected for PatientDetail / verschiedene Etiketten drucken; Kontakt Detail: Adressetikette drucken
		@Override
		public boolean close(){
			//Call the original overwritten close method?
			boolean ret = super.close();
			
			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): begin");

			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: Added a close function with at least the text.getPlugin().dispose(); functionality");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: as also added to other TextContainer clients, to ensure that TextPlugin connection/server");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: is closed again after usage. Please review other commented out content of this dispose():");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: Does EtiketteDruckenDialog need more functionality as well?");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

			/*
			 * Other content usually found in other TextContainer using clients: commented out, but please review if we should need anything. 
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: Bitte Prüfen: ist das gespeichert mit save() oder ähnlich, vor dem dispose?");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: Bitte Prüfen: Siehe info re added closeListener in TextView.java und below");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

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
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): about to txt.getPlugin().dispose()");
			text.getPlugin().dispose();		
			
			/*
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): about to GlobalEventDispatcher.removeActivationListener()...");
			GlobalEventDispatcher.removeActivationListener(this, this);
			*/

			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): about to super.dispose()... - warum hier im Ggs. zu TextView NICHT actBrief = null?");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): about PLEASE NOTE: ein paar Zeilen weiter oben bei removeMonitorEntry() hab ich actBrief übergeben, wie in TextView.java.dispose() auch. Korrekt?");
			System.out.println("js ch.elexis.views.rechnung.RnActions.java: RnListeDruckDialog: close(): close(): TODO: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			//20131027js:
			//Was nun das super.dispose genau macht, kann (will) ich im Moment nicht nachvollziehen. in Texteview.java steht das nicht / stand das wohl nie.
			//Vielleicht soll es tatsächlich den View-Rahmen (Composite, xyz, Tab mit Titel "Rezept") entsorgen, nachdem das Rezept fertig gedruckt ist?
			//Wofür ich extra das closePreviouslyOpen... in RezepteView bzw. in Briefauswahl.java erfunden habe?
			//Ich hoffe einmal, dass das nicht stört.
			//TODO: 20131027js: review: Was macht in RezeptBlatt.dispose() das super.dispose()? Braucht's das auch in TextView.java? Cave: Modulnamen-Verwirrung beachten, siehe intro comment von js.
			//super.dispose();
			return ret;
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
}
