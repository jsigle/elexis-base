/*******************************************************************************
 * Copyright (c) 2006-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.trustx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ch.elexis.Hub;
import ch.elexis.TarmedRechnung.XMLExporter;
import ch.elexis.data.Fall;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.data.TrustCenters;
import ch.elexis.tarmedprefs.PreferenceConstants;
import ch.elexis.util.IRnOutputter;
import ch.elexis.util.Log;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Transmit bills directly to a 'TrustCenter' via TrustX-Module. Note: This works only on Windows,
 * because TrustX and ASAS are Closed-Source COM-DLL's Prerequisites: ASAS installed and configured,
 * Trustx installed and configured. Dependencies: elexis-arzttarife-schweiz, jdomwrapper
 * 
 * @author Gerry
 * 
 */
public class TrustXTransmit implements IRnOutputter {
	Combo cbTC, cbASAS;
	ITrustx trustx;
	ICode icode;
	String inputdir;
	String tc, asas;
	
	TrustXLog xlog;
	
	// private Log log=Log.get("TrustX");
	
	/**
	 * Export and transmit all bills contained in rnn
	 * 
	 * @param rnn
	 *            Collection of bills to transmit
	 * @param asCopy
	 *            true to mar bills as "copy" or "resend"
	 * 
	 * @return Result containing all erroneous bills.
	 */
	public Result<Rechnung> doOutput(final IRnOutputter.TYPE type, final Collection<Rechnung> rnn,
		Properties props){
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		final Result<Rechnung> res = new Result<Rechnung>();
		
		try {
			progressService.runInUI(PlatformUI.getWorkbench().getProgressService(),
				new IRunnableWithProgress() {
					public void run(final IProgressMonitor monitor){
						monitor.beginTask("Exportiere Rechnungen...", rnn.size() * 10);
						int errors = 0;
						for (Rechnung rn : rnn) {
							if (tc == null) {
								if (cbTC == null) {
									if (!initTrustX()) {
										SWTHelper.showError("Fehler mit TrustX",
											"Konnte TrustX oder ASAS nicht starten");
										return;
									}
									if (type.equals(IRnOutputter.TYPE.STORNO)) {
										List<String> msgs = rn.getTrace(Rechnung.OUTPUT);
										for (String msg : msgs) {
											if (msg.indexOf(getDescription()) != -1) {
												String[] fields = msg.split("\\s*:\\s*");
												asas = fields[fields.length - 1];
												tc = fields[fields.length - 2];
											}
										}
									}
								} else {
									tc = cbTC.getText();
									asas = cbASAS.getText();
									if (StringTool.isNothing(tc)) {
										SWTHelper
											.alert("Kein Trustcenter",
												"Bitte wählen Sie ein TrustCenter aus der Liste (TC Test für Tests)");
										return;
									}
								}
							}
							trustx.trustCenter(tc);
							trustx.asasLogin(asas);
							Hub.mandantCfg.set(PreferenceCostants.TRUSTX_ASASLOGIN, asas);
							
							xlog.init();
							monitor.worked(1);
							XMLExporter ex = new XMLExporter();
							Document dRn = ex.doExport(rn, null, type, true);
							// ex.doExport(rn, inputdir+File.separator+rn.getNr()+".xml",
							// type,true);
							monitor.worked(2);
							if (rn.getStatus() == RnStatus.FEHLERHAFT) {
								errors++;
								continue;
							}
							String fname = inputdir + File.separator + rn.getNr() + ".xml";
							try {
								FileOutputStream fout = new FileOutputStream(fname);
								OutputStreamWriter cout = new OutputStreamWriter(fout, "UTF-8");
								XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
								xout.output(dRn, cout);
								cout.close();
								fout.close();
								int status_vorher = rn.getStatus();
								if ((status_vorher == RnStatus.OFFEN)
									|| (status_vorher == RnStatus.MAHNUNG_1)
									|| (status_vorher == RnStatus.MAHNUNG_2)
									|| (status_vorher == RnStatus.MAHNUNG_3)) {
									rn.setStatus(status_vorher + 1);
								}
							} catch (Exception e1) {
								ExHandler.handle(e1);
								SWTHelper.showError("Fehler bei Trustx", "Konnte Datei " + fname
									+ " nicht schreiben");
								rn.reject(RnStatus.REJECTCODE.INTERNAL_ERROR, "write error: "
									+ fname);
								continue;
							}
							trustx.auto();
							monitor.worked(5);
							boolean status = xlog.read();
							monitor.worked(2);
							if (status) {
								rn.addTrace(
									Rechnung.OUTPUT,
									getDescription() + ":" + trustx.trustCenter() + ":"
										+ trustx.asasLogin());
								continue;
							}
							rn.reject(RnStatus.REJECTCODE.REJECTED_BY_PEER,
								xlog.getLastErrorString());
							res.add(Result.SEVERITY.ERROR, 1,
								"Fehler beim Übertragen: " + xlog.getLastErrorString(), rn, true);
							errors++;
						}
						monitor.done();
						if (errors > 0) {
							SWTHelper.alert(
								"Fehler bei der Übermittlung",
								Integer.toString(errors)
									+ " Rechnungen waren fehlerhaft. Sie können diese unter Rechnungen mit dem Status fehlerhaft aufsuchen und korrigieren");
						} else {
							SWTHelper.showInfo("Übermittlung beendet",
								"Es sind keine Fehler aufgetreten");
						}
					}
				}, null);
		} catch (Exception ex) {
			ExHandler.handle(ex);
			res.add(Result.SEVERITY.ERROR, 2, ex.getMessage(), null, true);
			ErrorDialog.openError(null, "Fehler bei der Ausgabe",
				"Konnte TrustX-Transmit nicht starten", ResultAdapter.getResultAsStatus(res));
			return res;
		}
		return res;
	}
	
	boolean initTrustX(){
		try {
			trustx = ClassFactory.createCTrustx();
			inputdir = trustx.inputDirectory();
			String base = trustx.workDirectory();
			String session = trustx.session();
			if (StringTool.isNothing(session)) {
				session = new TimeTool().toString(TimeTool.DATE_ISO);
			}
			xlog =
				new TrustXLog(base + File.separator + "logs" + File.separator + session + ".log");
			return true;
			
		} catch (Throwable ex) {
			ExHandler.handle(ex);
			return false;
		}
		
	}
	
	public String getDescription(){
		return "Übermittlung via TrustX";
	}
	
	public boolean canStorno(final Rechnung rn){
		// we need to know when a bill is cancelled, because we have to send the storno message to
		// the trust center
		return true;
	}
	
	public Control createSettingsControl(final Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		Label trustxver = new Label(ret, SWT.NONE);
		Label asasver = new Label(ret, SWT.NONE);
		trustxver.setText("TrustX nicht gefunden oder falsch konfiguriert");
		asasver.setText("ASAS nicht vorhanden oder nicht gestartet");
		cbASAS = new Combo(ret, SWT.READ_ONLY);
		try {
			if (initTrustX()) {
				String tv = trustx.trustxVersion();
				trustxver.setText("TrustX Version " + tv);
				asasver.setText(trustx.asasVersion());
				IAsasCollection asasLogins = trustx.asasLogins();
				int iLogins = asasLogins.count();
				for (int i = 0; i < iLogins; i++) {
					Object o = asasLogins.item(i + 1);
					cbASAS.add(o.toString());
				}
				String def = "";
				if (iLogins > 0) {
					def = cbASAS.getItem(0);
				}
				cbASAS.setText(Hub.mandantCfg.get(PreferenceCostants.TRUSTX_ASASLOGIN, def));
				cbTC = new Combo(ret, SWT.READ_ONLY);
				cbTC.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
				for (String s : TrustCenters.getTCList()) {
					cbTC.add(s);
				}
				cbTC.setText(Hub.localCfg.get(PreferenceConstants.TARMEDTC, "TC test"));
				cbTC.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e){
						Hub.localCfg.set(PreferenceConstants.TARMEDTC, cbTC.getText());
					}
					
				});
			}
		} catch (Exception ex) {
			ExHandler.handle(ex);
			trustxver.setText("Fehler beim Initialisieren von TrustX");
		}
		return ret;
	}
	
	public boolean canBill(final Fall fall){
		return true;
	}
	
	public void saveComposite(){
		// Nothing
	}
	
}
