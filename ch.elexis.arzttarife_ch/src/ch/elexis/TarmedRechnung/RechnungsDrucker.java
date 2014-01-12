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
 *******************************************************************************/

package ch.elexis.TarmedRechnung;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import ch.elexis.Hub;
import ch.elexis.data.Fall;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.tarmedprefs.PreferenceConstants;
import ch.elexis.util.IRnOutputter;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.elexis.views.BestellBlatt;
import ch.elexis.views.RnPrintView2;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;

public class RechnungsDrucker implements IRnOutputter {
	// Mandant actMandant;
	TarmedACL ta = TarmedACL.getInstance();
	//20131030js: Initialize as null.
	RnPrintView2 rnp = null;
	IWorkbenchPage rnPage;
	// IProgressMonitor monitor;
	private Button bESR, bForms, bIgnoreFaults, bSaveFileAs;
	String dirname = Hub.localCfg.get(PreferenceConstants.RNN_EXPORTDIR, null);
	Text tName;
	
	private boolean bESRSelected, bFormsSelected, bIgnoreFaultsSelected, bSaveFileAsSelected;
	
	public Result<Rechnung> doOutput(final IRnOutputter.TYPE type,
		final Collection<Rechnung> rechnungen, Properties props){
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): begin");
				
		System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to rnPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();");		
		rnPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		final Result<Rechnung> res = new Result<Rechnung>();
		// ElexisEventCascade.getInstance().stop();
		try {
			//20131030js: Trying to move this further down, so that each generated Rechnung gets its own new rnp
			//This is needed as otherwise NOAtext_jsl 1.4.17/1.4.18 would not be able to load a new doc into a previously used frame/whatevercontainer.
			//rnp = (RnPrintView2) rnPage.showView(RnPrintView2.ID);
			
			//20131030js: From the javadoc of progressService.runInUI:
			//[...]
			//Note: Running long operations in the UI thread is generally not recommended.
			//This can result in the UI becoming unresponsive for the duration of the operation.
			//Where possible, busyCursorWhile should be used instead.
			//[...]
			//Exactly. The UI becomes very unresponsive while the following would run.
			//Even the progress monitor is not updated; not even it's window title string.
			//
			
			//However, both progressService.busyCursorWhile() and .run() have different parameters.
			//tried to use busyCursorWhile() but got invalid thread access errors first for showView(), hide(view), even for doPrint(),
			//and both Display..syncExec..run() threading and doPrint() need try-catch; the latter wants to report status info through that,
			//so it get's really complicated to interveave all this. Nope - I undid the changes.
			
			//And yes, external doc says: If you run stuff in runInUI() then GUI Updates will be halted while stuff is running, and resumed thereafter.
			
			progressService.runInUI(PlatformUI.getWorkbench().getProgressService(),
				new IRunnableWithProgress() {
					public void run(final IProgressMonitor monitor){
		
						//20131030js: Einen Hinweis anzeigen, mit Anzahl der zu druckenden Rechnungen,
						//und dass die Rechnungserzeugung die GUI unresponsive belassen wird,
						//dass das jedoch ok ist.
						if (rechnungen.size()>1) {
							SWTHelper.showInfo(Messages.RechnungsDrucker_PrintingBills,
									"n = "+Integer.toString(rechnungen.size())+"\n\n"+Messages.RechnungsDrucker_UIUpdatesMayStopNoWorries);
						}

						//Initialize the progress monitor window
						monitor.beginTask(Messages.RechnungsDrucker_PrintingBills,rechnungen.size() * 10);
		    
						int completedRn = 0;		//20131030js: keep track of completed number of invoices
						int errors = 0;
						
						System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to process "+rechnungen.size()+" invoices: for (Rechnung rn : rechnungen)...");		
						for (Rechnung rn : rechnungen) {						
							try {
								//20131030js: First check if a View for documents of the same type is already open.
								//If yes, close it in (ALMOST, BUT SUFFICIENTLY SIMILAR) the same way it would be closed if a user clicks on its [x] close button.
								//We get an similar effect as I got by introducing
								//my closePreExistingViewToEnsureOfficeCanHandleNewContentProperly(); in other modules
								//from the following hideView():
								System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to if (rnp!=null) { rnPage.hideView(rnp); rnp.dispose(); rnp=null; }");		
								if (rnp!=null) { rnPage.hideView(rnp); rnp.dispose(); rnp=null; }
								//In this module, it was originally used unconditionally
								//only once AFTER the all invoices generation loop was completed;
								//but I do NOW use it additionally once before each produced invoice - of course complemented
								//by one showView() - which was originaly used once before the invoice generation loop was started.
								//SO EVERY invoice (including 1 to 3 documents, actually) will get it's own instance of Office or at least its own connection.
								//This is needed for NOAText_jsl 1.4.17/1.4.18 because for some reason I've not yet completely understood,
								//good connection tracking and disconnection of unused connections, and closing office after the last document using it,
								//cost me the ability to load other documents into previously used frames/containers/whatevers.
								//
								//20131030js: moved here from further above, see there for explanations.
								System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to if (rnp==null) rnp = (RnPrintView2) rnPage.showView(RnPrintView2.ID);");		
								if (rnp==null) rnp = (RnPrintView2) rnPage.showView(RnPrintView2.ID);
								
								System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to if doPrint(...)...");		
								if (rnp.doPrint(rn, type, bSaveFileAsSelected ? dirname
									+ File.separator + rn.getNr() + ".xml" : null, bESRSelected, //$NON-NLS-1$
									bFormsSelected, !bIgnoreFaultsSelected, null /* monitor */) == false) {
									String errms =
										Messages.RechnungsDrucker_TheBill + rn.getNr()
											+ Messages.RechnungsDrucker_Couldntbeprintef;
									res.add(Result.SEVERITY.ERROR, 1, errms, rn, true);
									errors++;
									continue;
								}
								int status_vorher = rn.getStatus();
								if ((status_vorher == RnStatus.OFFEN)
									|| (status_vorher == RnStatus.MAHNUNG_1)
									|| (status_vorher == RnStatus.MAHNUNG_2)
									|| (status_vorher == RnStatus.MAHNUNG_3)) {
									rn.setStatus(status_vorher + 1);
								}
								rn.addTrace(Rechnung.OUTPUT, getDescription() + ": " //$NON-NLS-1$
									+ RnStatus.getStatusText(rn.getStatus()));
							} catch (Exception ex) {
								ExHandler.handle(ex);
								String msg = ex.getMessage();
								if (msg == null) {
									msg = Messages.RechnungsDrucker_MessageErrorInternal;
								}
								SWTHelper.showError(
									Messages.RechnungsDrucker_MessageErrorWhilePrinting
										+ rn.getNr(), msg);
								errors++;
							} //try-catch process one Rechnung rn

							System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): returned from doPrint(...)");
							
							//20131030js: Update the progress monitor
							completedRn++;
							System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): completed processing of "+completedRn+"/"+rechnungen.size()+" invoices.");		

							//20131030js: Even adding this does NOT give me a visible progress monitor with current NOAText and RnPrinView2.java implementations.
							//by the way, monitor is passed to doPrint() above, and doPrint should also update the monitor. But it's just not visible. 
							//Or should doPrint() only display progress for a subtask - which is created there - and here, calls to monitor.... had been forgotten?
							monitor.worked(completedRn*10);
							
							//Das hier sieht man auch nicht...
							monitor.setTaskName("Progress Information: Rechnung "+Integer.toString(completedRn)+" verarbeitet.");
							
							rnPage.findView(RnPrintView2.ID).setFocus();

							/* 20131030js: Sorry, NOTHING of these attempts gets beyond the display of the cover page of the 1st processed rn.
							 * Nor does the added monitor.worked() display above get any visible change. So leave it all away.

							rnp.setFocus();
							Thread.yield();
							
							//20131030js: Add a small sleep. For function, none would be required,
							//but if we want to see the content of all invoices past the 1st one, it is.
							System.out.println("js ch.elexis.arzttarife_ch/ch.elexis.TarmedRechnung/RechnungsDrucker.java: doOutput(): about to sleep...");
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								//sleep has ended, nothing else needed to do.
							}
							*/
							
							
						} //for (Rechnung rn : rechnungen) 
						monitor.done();
						
						//20131030js: Sound a beep when all invoices are completed.
						Display.getCurrent().beep();
												
						if (errors == 0) {
							SWTHelper.showInfo(Messages.RechnungsDrucker_PrintingFinished,
								Messages.RechnungsDrucker_AllFinishedNoErrors);
						} else {
							SWTHelper.showError(Messages.RechnungsDrucker_ErrorsWhilePrinting,
								Integer.toString(errors)
									+ Messages.RechnungsDrucker_ErrorsWhiilePrintingAdvice);
						}
					}
					
				 }, null);		//progress monitor related; runInUI with scheduling rule null

			//20131030js: added comment and if rnp!=null
			//One final hideview to close the RnPrintView2 office document window with the last created invoice portion.
			//This has originally been in this module at this place.
			if (rnp!=null) rnPage.hideView(rnp);
			
		} catch (Exception ex) {
			ExHandler.handle(ex);
			res.add(Result.SEVERITY.ERROR, 2, ex.getMessage(), null, true);
			ErrorDialog.openError(null, Messages.RechnungsDrucker_ErrorsWhilePrinting,
				Messages.RechnungsDrucker_CouldntOpenPrintView,
				ResultAdapter.getResultAsStatus(res));
			return res;
		} finally {
			// ElexisEventCascade.getInstance().start();
		}
		return res;
	}
	
	public String getDescription(){
		return Messages.RechnungsDrucker_PrintAsTarmed;
	}
	
	public Control createSettingsControl(final Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		bESR = new Button(ret, SWT.CHECK);
		bForms = new Button(ret, SWT.CHECK);
		bESR.setText(Messages.RechnungsDrucker_WithESR);
		bESR.setSelection(true);
		bForms.setText(Messages.RechnungsDrucker_WithForm);
		bForms.setSelection(true);
		bIgnoreFaults = new Button(ret, SWT.CHECK);
		bIgnoreFaults.setText(Messages.RechnungsDrucker_IgnoreFaults);
		bIgnoreFaults.setSelection(Hub.localCfg.get(PreferenceConstants.RNN_RELAXED, true));
		bIgnoreFaults.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				Hub.localCfg.set(PreferenceConstants.RNN_RELAXED, bIgnoreFaults.getSelection());
			}
			
		});
		Group cSaveCopy = new Group(ret, SWT.NONE);
		cSaveCopy.setText(Messages.RechnungsDrucker_FileForTrustCenter);
		cSaveCopy.setLayout(new GridLayout(2, false));
		bSaveFileAs = new Button(cSaveCopy, SWT.CHECK);
		bSaveFileAs.setText(Messages.RechnungsDrucker_AskSaveForTrustCenter);
		bSaveFileAs.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		bSaveFileAs.setSelection(Hub.localCfg.get(PreferenceConstants.RNN_SAVECOPY, false));
		bSaveFileAs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				Hub.localCfg.set(PreferenceConstants.RNN_SAVECOPY, bSaveFileAs.getSelection());
			}
			
		});
		
		Button bSelectFile = new Button(cSaveCopy, SWT.PUSH);
		bSelectFile.setText(Messages.RechnungsDrucker_Directory);
		bSelectFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				DirectoryDialog ddlg = new DirectoryDialog(parent.getShell());
				dirname = ddlg.open();
				if (dirname == null) {
					SWTHelper.alert(Messages.RechnungsDrucker_DirNameMissingCaption,
						Messages.RechnungsDrucker_DirnameMissingText);
				} else {
					Hub.localCfg.set(PreferenceConstants.RNN_EXPORTDIR, dirname);
					tName.setText(dirname);
				}
			}
		});
		tName = new Text(cSaveCopy, SWT.BORDER | SWT.READ_ONLY);
		tName.setText(Hub.localCfg.get(PreferenceConstants.RNN_EXPORTDIR, "")); //$NON-NLS-1$
		return ret;
	}
	
	public boolean canStorno(final Rechnung rn){
		// We do not need to react on cancel messages
		return false;
	}
	
	public boolean canBill(final Fall fall){
		return true;
	}
	
	public void saveComposite(){
		bESRSelected = bESR.getSelection();
		bFormsSelected = bForms.getSelection();
		bIgnoreFaultsSelected = bIgnoreFaults.getSelection();
		bSaveFileAsSelected = bSaveFileAs.getSelection();
	}
	
}
