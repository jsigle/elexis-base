/**
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Adapted from Viollier to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 * Important changes:
 * - OpenMedical Library configurable
 * - Easier handling of direct import
 * - Non-unique patients can be assigned to existing patients by user
 *   (instead of creating new patients)
 */

package ch.elexis.laborimport.bioanalytica;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Kontakt;
import ch.elexis.data.LabItem;
import ch.elexis.data.LabResult;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Messages;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class Importer extends ImporterPage {
	public static final String MY_LAB = "Bioanalytica";
	public static final String PLUGIN_ID = "ch.elexis.laborimport.bioanalytica";
	
	private static final String OPENMEDICAL_MAINCLASS = "ch.openmedical.JMedTransfer.JMedTransfer";
	
	private static final String COMMENT_NAME = "Kommentar";
	private static final String COMMENT_CODE = "kommentar";
	private static final String COMMENT_GROUP = "00 Kommentar";
	
	private static final String FOLGT_TEXT = "folgt";
	
	// importer type
	private static final int FILE = 1;
	private static final int DIRECT = 2;
	
	private Object openmedicalObject = null;
	private Method openmedicalDownloadMethod = null;
	
	public Importer(){}
	
	private static URLClassLoader getURLClassLoader(URL jarURL){
		return new URLClassLoader(new URL[] {
			jarURL
		});
	}
	
	@Override
	public Composite createPage(Composite parent){
		// try to dynamically load the openmedical JAR file
		String jarPath = Hub.localCfg.get(PreferencePage.JAR_PATH, null);
		if (jarPath != null) {
			File jar = new File(jarPath);
			if (jar.canRead()) {
				try {
					URLClassLoader urlLoader =
						getURLClassLoader(new URL("file", null, jar.getAbsolutePath()));
					
					Class openmedicalClass = urlLoader.loadClass(OPENMEDICAL_MAINCLASS);
					
					// try to get the download method
					Method meth;
					try {
						meth = openmedicalClass.getMethod("download", String[].class);
					} catch (Throwable e) {
						throw e;
					}
					
					// try to get an instance
					Object obj = openmedicalClass.newInstance();
					
					// success (no exception); set the global variables
					openmedicalObject = obj;
					openmedicalDownloadMethod = meth;
				} catch (Throwable e) {
					// loading the class failed; do nothing
				}
			}
		}
		
		// parentShell=parent.getShell();
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		LabImporter labImporter = new LabImporter(ret, this);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		labImporter.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		return ret;
	}
	
	/**
	 * Create reminder for mandant, so that he knows that new lab results are avilable. To be
	 * replaced by a separate view.
	 * 
	 * @param lr
	 *            the added LabResult
	 */
	/*
	 * private void notifyMandant(LabResult lr) { Query<Reminder> query = new
	 * Query<Reminder>(Reminder.class); query.add("IdentID", "=", lr.getPatient().getId());
	 * query.and(); query.add("Due", "=", lr.getDate()); query.and(); query.add("Message", "=",
	 * "Neue Laborwerte"); List<Reminder> reminders = query.execute(); if (reminders == null ||
	 * reminders.size() == 0) { // create new reminder Mandant mandant = Hub.actMandant; Reminder
	 * reminder = new Reminder(lr.getPatient(), new
	 * TimeTool(lr.getDate()).toString(TimeTool.DATE_GER), Reminder.Typ.anzeigeTodoAll, "",
	 * "Neue Laborwerte"); reminder.addToList("Responsibles", mandant.getId(), (String[]) null); }
	 * else { // reset status Reminder reminder = reminders.get(0); Reminder.Status status =
	 * reminder.getStatus(); if (status == Reminder.Status.erledigt || status ==
	 * Reminder.Status.unerledigt) { reminder.setStatus(Reminder.Status.geplant); } } }
	 */
	
	private Result<String> parse(HL7 hl7, Kontakt labor, Patient pat){
		HL7.OBR obr = hl7.firstOBR();
		int number = 1;
		String timestamp = new TimeTool().toString(TimeTool.TIMESTAMP);
		while (obr != null) {
			// the date in the obr record is the date when the sample
			// has been produced. The date in the obx record is the date
			// when the order has been imported by the lab
			TimeTool sampleDate = obr.getDate();
			
			HL7.OBX obx = obr.firstOBX();
			while (obx != null) {
				String itemname = obx.getItemName();
				Query<LabItem> qbe = new Query<LabItem>(LabItem.class);
				qbe.add("LaborID", "=", labor.getId());
				// disabled, this would avoid renaming the title
				// qbe.add("titel", "=", itemname);
				qbe.add("kuerzel", "=", obx.getItemCode());
				List<LabItem> list = qbe.execute();
				LabItem li = null;
				if (list.size() < 1) {
					LabItem.typ typ = LabItem.typ.NUMERIC;
					if (obx.isFormattedText()) {
						typ = LabItem.typ.TEXT;
					}
					
					String code = obx.getItemCode();
					String name = Groups.getCodeName(code);
					String group = Groups.getGroupNameOfCode(code);
					String order = Groups.getCodeOrderByGroupName(group, labor);
					// String order = timestamp + String.format("%04d",
					// number++);
					li =
						new LabItem(code, name, labor, obx.getRefRange(), obx.getRefRange(),
							obx.getUnits(), typ, group, order);
				} else {
					li = list.get(0);
				}
				LabResult lr;
				;
				Query<LabResult> qr = new Query<LabResult>(LabResult.class);
				qr.add("PatientID", "=", pat.getId());
				qr.add("Datum", "=", sampleDate.toString(TimeTool.DATE_GER));
				qr.add("ItemID", "=", li.getId());
				List<LabResult> existingResults = qr.execute();
				if (existingResults != null && existingResults.size() != 0) {
					lr = null;
					
					/*
					 * if(SWTHelper.askYesNo("Dieser Laborwert wurde schon importiert" ,
					 * "Weitermachen?")){ obx=obr.nextOBX(obx); continue; }else{ return new
					 * Result<String>("Cancelled"); }
					 */
					
					// just re-import. especially required for "folgt" values
					// we just overwrite the values; for recognizing succeeding
					// results,
					// we would need a concept of a "lab order".
					// it's not enough only to consider "folgt" values, since
					// there may
					// be succeeding values
					// 2008-07-10, danlutz: it happens that we get multiple
					// results from
					// multiple orders of the same day. so without
					// order management, we just need to import
					// all values
					for (LabResult existingResult : existingResults) {
						String oldResult = existingResult.getResult();
						String oldComment = existingResult.get("Kommentar");
						
						String newResult;
						String newComment;
						
						if (obx.isFormattedText()) {
							newResult = "text";
							newComment = PersistentObject.checkNull(obx.getResultValue());
						} else {
							newResult = PersistentObject.checkNull(obx.getResultValue());
							newComment = PersistentObject.checkNull(obx.getComment());
						}
						
						// check if we have a result from a not-yet finished
						// order
						if (oldResult.equals(FOLGT_TEXT)) {
							// overwrite this result
							lr = existingResult;
							
							// we are going to replace an existing value, so add
							// it to the "unseen"
							// values
							lr.addToUnseen();
							
							break;
						}
						
						// check if we have a result with the same values
						if (oldResult.equals(newResult) && oldComment.equals(newComment)) {
							// just overwrite this result
							lr = existingResult;
							break;
						}
					}
					
					if (lr != null) {
						// update an existing result
						if (obx.isFormattedText()) {
							lr.setResult("text");
							lr.set("Kommentar", obx.getResultValue());
						} else {
							lr.setResult(obx.getResultValue());
							lr.set("Kommentar", obx.getComment());
						}
					} else {
						// create a new result
						if (obx.isFormattedText()) {
							lr = new LabResult(pat, sampleDate, li, "text", obx.getResultValue());
						} else {
							lr =
								new LabResult(pat, sampleDate, li, obx.getResultValue(),
									obx.getComment());
						}
					}
				} else {
					if (obx.isFormattedText()) {
						lr = new LabResult(pat, sampleDate, li, "text", obx.getResultValue());
					} else {
						lr =
							new LabResult(pat, sampleDate, li, obx.getResultValue(),
								obx.getComment());
					}
				}
				// notifyMandant(lr);
				if (obx.isPathologic()) {
					lr.setFlag(LabResult.PATHOLOGIC, true);
				}
				obx = obr.nextOBX(obx);
			}
			obr = obr.nextOBR(obr);
		}
		
		// add comments as a LabResult
		
		String comments = hl7.getComments();
		if (!StringTool.isNothing(comments)) {
			obr = hl7.firstOBR();
			if (obr != null) {
				TimeTool commentsDate = obr.getDate();
				
				// find LabItem
				Query<LabItem> qbe = new Query<LabItem>(LabItem.class);
				qbe.add("LaborID", "=", labor.getId());
				// disabled, this would avoid renaming the title
				// qbe.add("titel", "=", COMMENT_NAME);
				qbe.add("kuerzel", "=", COMMENT_CODE);
				List<LabItem> list = qbe.execute();
				LabItem li = null;
				if (list.size() < 1) {
					// LabItem doesn't yet exist
					LabItem.typ typ = LabItem.typ.TEXT;
					String order = Groups.getCodeOrderByGroupName(COMMENT_GROUP, labor);
					li =
						new LabItem(COMMENT_CODE, COMMENT_NAME, labor, "", "", "", typ,
							COMMENT_GROUP, order);
				} else {
					li = list.get(0);
				}
				
				// add LabResult
				Query<LabResult> qr = new Query<LabResult>(LabResult.class);
				qr.add("PatientID", "=", pat.getId());
				qr.add("Datum", "=", commentsDate.toString(TimeTool.DATE_GER));
				qr.add("ItemID", "=", li.getId());
				if (qr.execute().size() == 0) {
					// only add coments not yet existing
					
					new LabResult(pat, commentsDate, li, "Text", comments);
				}
			}
		}
		
		return new Result<String>("OK");
	}
	
	/**
	 * Equivalent to importFile(new File(file), null)
	 * 
	 * @param filepath
	 *            the file to be imported (full path)
	 * @return
	 */
	private Result importFile(String filepath){
		return importFile(new File(filepath), null);
	}
	
	/**
	 * Import the given HL7 file. Optionally, move the file into the given archive directory
	 * 
	 * @param file
	 *            the file to be imported (full path)
	 * @param archiveDir
	 *            a directory where the file should be moved to on success, or null if it should not
	 *            be moved.
	 * @return the result as type Result
	 */
	private Result importFile(File file, File archiveDir){
		HL7 hl7 = new HL7(null, null);
		Result<String> r = hl7.load(file);
		if (r.isOK()) {
			Result<Patient> res = hl7.getPatient();
			if (res.isOK()) {
				Result<Kontakt> rk = hl7.getLabor();
				if (rk.isOK()) {
					Patient pat = res.get();
					Kontakt labor = rk.get();
					
					Result ret = parse(hl7, labor, pat);
					
					// move result to archive
					if (ret.isOK()) {
						if (archiveDir != null) {
							if (archiveDir.exists() && archiveDir.isDirectory()) {
								if (file.exists() && file.isFile() && file.canRead()) {
									File newFile = new File(archiveDir, file.getName());
									if (!file.renameTo(newFile)) {
										SWTHelper.showError("Fehler beim Archivieren", "Die Datei "
											+ file.getAbsolutePath()
											+ " konnte nicht ins Archiv verschoben werden.");
									}
								}
							}
						}
					}
					ElexisEventDispatcher.reload(LabItem.class);
					return ret;
				} else {
					return rk;
				}
			} else {
				
				ResultAdapter.displayResult(res, "Fehler beim Import");
				return res;
			}
		}
		return r;
		
	}
	
	private Result importDirect(){
		if (openmedicalObject == null) {
			return new Result<String>(Result.SEVERITY.ERROR, 1, MY_LAB,
				"Fehlerhafte Konfiguration", true);
		}
		Result<String> result = new Result<String>("OK");
		
		String downloadDirPath =
			Hub.localCfg.get(PreferencePage.DL_DIR, Hub.getTempDir().toString());
		String iniPath = Hub.localCfg.get(PreferencePage.INI_PATH, null);
		
		int res = -1;
		if (iniPath != null) {
			try {
				Object omResult =
					openmedicalDownloadMethod.invoke(openmedicalObject, new Object[] {
						new String[] {
							"--download", downloadDirPath, "--logPath", downloadDirPath, "--ini",
							iniPath, "--verbose", "INF", "-#OpenMedicalKey#", "-allInOne"
						}
					});
				if (omResult instanceof Integer) {
					res = ((Integer) omResult).intValue();
					System.out.println(res + " files downoladed");
					if (res < 1) {
						SWTHelper.showInfo("Verbindung mit Labor " + MY_LAB + " erfolgreich",
							"Es sind keine Resultate zum Abholen vorhanden");
					}
				}
			} catch (Throwable e) {
				// method call failed; do nothing
			}
		}
		// if (res > 0) {
		File downloadDir = new File(downloadDirPath);
		if (downloadDir.isDirectory()) {
			File archiveDir = new File(downloadDir, "archive");
			if (!archiveDir.exists()) {
				archiveDir.mkdir();
			}
			
			String[] files = downloadDir.list(new FilenameFilter() {
				
				public boolean accept(File path, String name){
					if (name.toLowerCase().endsWith(".hl7")) {
						return true;
					}
					return false;
				}
			});
			for (String file : files) {
				File f = new File(downloadDir, file);
				Result rs = importFile(f, archiveDir);
				if (!rs.isOK()) {
					// importFile already shows error
					// rs.display("Fehler beim Import");
				}
			}
			SWTHelper.showInfo("Verbindung mit Labor " + MY_LAB + " erfolgreich", "Es wurden "
				+ Integer.toString(res) + " Dateien verarbeitet");
		} else {
			SWTHelper.showError("Falsches Verzeichnis",
				"Bitte kontrollieren Sie die Einstellungen für das Download-Verzeichnis");
			result =
				new Result<String>(Result.SEVERITY.ERROR, 1, MY_LAB, "Fehlerhafte Konfiguration",
					true);
		}
		// }
		
		return result;
	}
	
	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		int type;
		try {
			String sType = results[0];
			type = Integer.parseInt(sType);
		} catch (NumberFormatException ex) {
			type = FILE;
		}
		
		if (type != FILE && type != DIRECT) {
			type = FILE;
		}
		
		if (type == FILE) {
			String filename = results[1];
			return ResultAdapter.getResultAsStatus(importFile(filename));
		} else {
			return ResultAdapter.getResultAsStatus(importDirect());
		}
	}
	
	@Override
	public String getDescription(){
		return "Bitte wählen Sie eine Datei im HL7-Format oder die Direktübertragung zum Import aus";
	}
	
	@Override
	public String getTitle(){
		return "Labor " + MY_LAB;
	}
	
	String getBasePath(){
		try {
			URL url = Platform.getBundle(PLUGIN_ID).getEntry("/");
			url = FileLocator.toFileURL(url);
			String bundleLocation = url.getPath();
			File file = new File(bundleLocation);
			bundleLocation = file.getAbsolutePath();
			return bundleLocation;
		} catch (Throwable throwable) {
			return null;
		}
	}
	
	/**
	 * An importer that lets the user select a file to import or directly import the data from the
	 * lab. The chosen type (file or direct import) is stored in results[0] (FILE or DIRECT). If
	 * FILE is chosen, the file path is stored in results[1].
	 * 
	 * @author gerry, danlutz
	 * 
	 */
	private class LabImporter extends Composite {
		private Button bFile;
		private Button bDirect;
		
		private Text tFilename;
		
		public LabImporter(final Composite parent, final ImporterPage home){
			super(parent, SWT.BORDER);
			setLayout(new GridLayout(3, false));
			
			bFile = new Button(this, SWT.RADIO);
			bFile.setText("Import aus Datei (HL7)");
			bFile.setLayoutData(SWTHelper.getFillGridData(3, true, 1, false));
			
			Label lFile = new Label(this, SWT.NONE);
			lFile.setText("    " + Messages.getString("ImporterPage.file")); //$NON-NLS-1$ //$NON-NLS-2$
			GridData gd = SWTHelper.getFillGridData(1, false, 1, false);
			gd.horizontalAlignment = GridData.END;
			gd.widthHint = lFile.getSize().x + 20;
			
			tFilename = new Text(this, SWT.BORDER);
			tFilename.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			
			Button bBrowse = new Button(this, SWT.PUSH);
			bBrowse.setText(Messages.getString("ImporterPage.browse")); //$NON-NLS-1$
			
			bDirect = new Button(this, SWT.RADIO);
			bDirect.setText("Direkter Import");
			bDirect.setLayoutData(SWTHelper.getFillGridData(3, true, 1, false));
			
			int type = Hub.localCfg.get("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
			if (openmedicalObject == null) {
				type = FILE;
			}
			
			home.results = new String[2];
			
			if (type == FILE) {
				bFile.setSelection(true);
				bDirect.setSelection(false);
				
				String filename =
					Hub.localCfg.get("ImporterPage/" + home.getTitle() + "/filename", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				tFilename.setText(filename);
				
				home.results[0] = new Integer(FILE).toString();
				home.results[1] = filename;
			} else {
				bFile.setSelection(false);
				bDirect.setSelection(true);
				
				tFilename.setText("");
				
				home.results[0] = new Integer(DIRECT).toString();
				home.results[1] = "";
			}
			
			if (openmedicalObject == null) {
				bDirect.setEnabled(false);
			}
			
			SelectionAdapter sa = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e){
					Button button = (Button) e.getSource();
					
					// only handle selection == true
					if (!button.getSelection()) {
						return;
					}
					
					int type = FILE;
					
					if (button == bFile) {
						type = FILE;
					} else if (button == bDirect) {
						type = DIRECT;
					}
					
					if (type == FILE) {
						bFile.setSelection(true);
						bDirect.setSelection(false);
						
						String filename = tFilename.getText();
						
						home.results[0] = new Integer(FILE).toString();
						home.results[1] = filename;
						
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", filename); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						bFile.setSelection(false);
						bDirect.setSelection(true);
						
						tFilename.setText("");
						
						home.results[0] = new Integer(DIRECT).toString();
						home.results[1] = "";
						
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", DIRECT); //$NON-NLS-1$ //$NON-NLS-2$
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", ""); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			};
			
			bFile.addSelectionListener(sa);
			bDirect.addSelectionListener(sa);
			
			bBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					bFile.setSelection(true);
					bDirect.setSelection(false);
					
					FileDialog fdl = new FileDialog(parent.getShell(), SWT.OPEN);
					fdl.setFilterExtensions(new String[] {
						"*"}); //$NON-NLS-1$
					fdl.setFilterNames(new String[] {
						Messages.getString("ImporterPage.allFiles")}); //$NON-NLS-1$
					String filename = fdl.open();
					if (filename == null) {
						filename = "";
					}
					
					tFilename.setText(filename);
					home.results[0] = new Integer(FILE).toString();
					home.results[1] = filename;
					
					Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
					Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", filename); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
			});
		}
	}
}
