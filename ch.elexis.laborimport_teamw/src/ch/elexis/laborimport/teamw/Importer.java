/**
 * (c) 2007-2011 by G. Weirich
 * All rights reserved
 * 
 * Adapted from Viollier to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 * Important changes:
 * - OpenMedical Library configurable
 * - Easier handling of direct import
 * - Non-unique patients can be assigned to existing patients by user
 *   (instead of creating new patients)
 *   
 * Adapted to Risch by Gerry Weirich
 * Changes:
 * -  Improved detection of Patient ID by evaluation the fields PATIENT_ID and PLACER_ORDER_NUMBER
 * -  Improved matching of Names to the database
 */

package ch.elexis.laborimport.teamw;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Vector;

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
import ch.elexis.data.Query;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.Messages;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.UtilFile;
import ch.ngiger.comm.ftp.FtpSemaException;
import ch.ngiger.comm.ftp.FtpServer;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.Result.SEVERITY;

public class Importer extends ImporterPage {
	public static final String MY_LAB = "Team W"; //$NON-NLS-1$
	public static final String PLUGIN_ID = "ch.elexis.laborimport_teamw"; //$NON-NLS-1$
	
	protected static Log log = Log.get(PLUGIN_ID); //$NON-NLS-1$
	
	private static final String COMMENT_NAME = "Kommentar"; //$NON-NLS-1$
	private static final String COMMENT_CODE = "kommentar"; //$NON-NLS-1$
	private static final String COMMENT_GROUP = "00 Kommentar"; //$NON-NLS-1$
	
	private static final String PRAXIS_SEMAPHORE = "praxis.sem"; //$NON-NLS-1$
	private static final String TEAMW_SEMAPHORE = "teamw.sem"; //$NON-NLS-1$
	
	// importer type
	private static final int FILE = 1;
	private static final int DIRECT = 2;
	
	public Importer(){}
	
	@Override
	public Composite createPage(final Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());
		LabImporter labImporter = new LabImporter(ret, this);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		labImporter.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		return ret;
	}
	
	private Result<String> parse(final HL7 hl7, final Kontakt labor, final Patient pat){
		HL7.OBR obr = hl7.firstOBR();
		int nummer = 0;
		String dat = new TimeTool().toString(TimeTool.DATE_GER);
		while (obr != null) {
			HL7.OBX obx = obr.firstOBX();
			while (obx != null) {
				String itemname = obx.getItemName();
				Query<LabItem> qbe = new Query<LabItem>(LabItem.class);
				qbe.add("LaborID", "=", labor.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				// disabled, this would avoid renaming the title
				// qbe.add("titel", "=", itemname);
				qbe.add("kuerzel", "=", obx.getItemCode()); //$NON-NLS-1$ //$NON-NLS-2$
				List<LabItem> list = qbe.execute();
				LabItem li = null;
				if (list.size() < 1) {
					LabItem.typ typ = LabItem.typ.NUMERIC;
					if (obx.isFormattedText()) {
						typ = LabItem.typ.TEXT;
					}
					li =
						new LabItem(
							obx.getItemCode(),
							itemname,
							labor,
							obx.getRefRange(),
							obx.getRefRange(),
							obx.getUnits(),
							typ,
							ch.elexis.laborimport.teamw.Messages.getString("Importer.automatisch") + dat, Integer //$NON-NLS-1$
								.toString(nummer++));
				} else {
					li = list.get(0);
				}
				LabResult lr;
				Query<LabResult> qr = new Query<LabResult>(LabResult.class);
				qr.add("PatientID", "=", pat.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				qr.add("Datum", "=", obr.getDate().toString(TimeTool.DATE_GER)); //$NON-NLS-1$ //$NON-NLS-2$
				qr.add("ItemID", "=", li.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				if (qr.execute().size() != 0) {
					if (SWTHelper.askYesNo(ch.elexis.laborimport.teamw.Messages
						.getString("Importer.question.allreadyImported"), //$NON-NLS-1$
						ch.elexis.laborimport.teamw.Messages
							.getString("Importer.question.allreadyImported.continue"))) { //$NON-NLS-1$
						obx = obr.nextOBX(obx);
						continue;
					} else {
						return new Result<String>(
							ch.elexis.laborimport.teamw.Messages.getString("Importer.cancelled")); //$NON-NLS-1$
					}
				}
				if (obx.isFormattedText()) {
					lr = new LabResult(pat, obr.getDate(), li, "text", obx //$NON-NLS-1$
						.getResultValue());
				} else {
					lr =
						new LabResult(pat, obr.getDate(), li, obx.getResultValue(),
							obx.getComment());
				}
				
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
				qbe.add("LaborID", "=", labor.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				qbe.add("titel", "=", COMMENT_NAME); //$NON-NLS-1$ //$NON-NLS-2$
				qbe.add("kuerzel", "=", COMMENT_CODE); //$NON-NLS-1$ //$NON-NLS-2$
				List<LabItem> list = qbe.execute();
				LabItem li = null;
				if (list.size() < 1) {
					// LabItem doesn't yet exist
					LabItem.typ typ = LabItem.typ.TEXT;
					li = new LabItem(COMMENT_CODE, COMMENT_NAME, labor, "", "", //$NON-NLS-1$ //$NON-NLS-2$
						"", typ, COMMENT_GROUP, Integer.toString(nummer++)); //$NON-NLS-1$
				} else {
					li = list.get(0);
				}
				
				// add LabResult
				Query<LabResult> qr = new Query<LabResult>(LabResult.class);
				qr.add("PatientID", "=", pat.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				qr.add("Datum", "=", commentsDate.toString(TimeTool.DATE_GER)); //$NON-NLS-1$ //$NON-NLS-2$
				qr.add("ItemID", "=", li.getId()); //$NON-NLS-1$ //$NON-NLS-2$
				if (qr.execute().size() == 0) {
					// only add coments not yet existing
					
					new LabResult(pat, commentsDate, li, "Text", comments); //$NON-NLS-1$
				}
			}
		}
		
		return new Result<String>(ch.elexis.laborimport.teamw.Messages.getString("Importer.ok")); //$NON-NLS-1$
	}
	
	/**
	 * Equivalent to importFile(new File(file), null)
	 * 
	 * @param filepath
	 *            the file to be imported (full path)
	 * @return
	 */
	private Result<?> importFile(final String filepath){
		File file = new File(filepath);
		Result<?> result = importFile(file, null);
		if (result.isOK()) {
			if (!file.delete()) {
				log.log("Datei " + file.getPath() //$NON-NLS-1$
					+ " konnte nicht gelöscht werden.", Log.WARNINGS); //$NON-NLS-1$
			}
		}
		return result;
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
	private Result<?> importFile(final File file, final File archiveDir){
		HL7 hl7 =
			new HL7(ch.elexis.laborimport.teamw.Messages.getString("Importer.lab") + MY_LAB, MY_LAB); //$NON-NLS-1$
		Result<Object> resultLoad = hl7.load(file.getAbsolutePath());
		if (resultLoad.isOK()) {
			Result<Object> resultPatient = hl7.getPatient(false);
			if (resultPatient.isOK()) {
				Result<Kontakt> resultKontakt = hl7.getLabor();
				if (resultKontakt.isOK()) {
					Patient pat = (Patient) resultPatient.get();
					Kontakt labor = resultKontakt.get();
					
					Result<String> resultParse = parse(hl7, labor, pat);
					
					// move result to archive
					if (resultParse.isOK()) {
						if (archiveDir != null) {
							if (archiveDir.exists() && archiveDir.isDirectory()) {
								if (file.exists() && file.isFile() && file.canRead()) {
									File newFile = new File(archiveDir, file.getName());
									if (!file.renameTo(newFile)) {
										String msg =
											MessageFormat.format(
												ch.elexis.laborimport.teamw.Messages
													.getString("Importer.error.moveToArchive"), //$NON-NLS-1$
												new Object[] {
													file.getAbsolutePath()
												});
										SWTHelper.showError(ch.elexis.laborimport.teamw.Messages
											.getString("Importer.error.archivieren"), //$NON-NLS-1$
											msg);
									}
								}
							}
						}
					}
					ElexisEventDispatcher.reload(LabItem.class);
					return resultParse;
				} else {
					return resultKontakt;
				}
			} else {
				return resultPatient;
			}
		}
		return resultLoad;
	}
	
	private Result<?> importDirect(){
		String batchOrFtp = Hub.globalCfg.get(PreferencePage.BATCH_OR_FTP, PreferencePage.FTP);
		if (PreferencePage.BATCH.equals(batchOrFtp)) {
			return importDirectBatch();
		}
		
		return importDirectFtp();
	}
	
	private Result<?> importDirectFtp(){
		Result<String> result =
			new Result<String>(ch.elexis.laborimport.teamw.Messages.getString("Importer.ok")); //$NON-NLS-1$
		
		String ftpHost =
			Hub.globalCfg.get(PreferencePage.FTP_HOST, PreferencePage.DEFAULT_FTP_HOST);
		String user = Hub.globalCfg.get(PreferencePage.FTP_USER, PreferencePage.DEFAULT_FTP_USER);
		String pwd = Hub.globalCfg.get(PreferencePage.FTP_PWD, PreferencePage.DEFAULT_FTP_PWD);
		String downloadDir =
			UtilFile.getCorrectPath(Hub.globalCfg.get(PreferencePage.DL_DIR,
				PreferencePage.DEFAULT_DL_DIR));
		
		FtpServer ftp = new FtpServer();
		try {
			List<String> hl7FileList = new Vector<String>();
			try {
				ftp.openConnection(ftpHost, user, pwd);
				ftp.addSemaphore(downloadDir, PRAXIS_SEMAPHORE, TEAMW_SEMAPHORE);
				
				String[] filenameList = ftp.listNames();
				log.log("Verbindung mit Labor " + MY_LAB //$NON-NLS-1$
					+ " erfolgreich. " + filenameList.length //$NON-NLS-1$
					+ " Dateien gefunden.", Log.INFOS); //$NON-NLS-1$
				for (String filename : filenameList) {
					if (filename.toUpperCase().endsWith("HL7")) { //$NON-NLS-1$
						ftp.downloadFile(filename, downloadDir + filename);
						log.log("Datei <" + filename + "> downloaded.", //$NON-NLS-1$ //$NON-NLS-2$
							Log.INFOS);
						hl7FileList.add(filename);
						// Zeile um Files auf FTP zu löschen.
						ftp.deleteFile(filename);
					}
				}
			} finally {
				ftp.removeSemaphore();
				ftp.closeConnection();
			}
			
			String header =
				MessageFormat.format(
					ch.elexis.laborimport.teamw.Messages.getString("Importer.import.header"), //$NON-NLS-1$
					new Object[] {
						MY_LAB
					});
			String question =
				MessageFormat.format(
					ch.elexis.laborimport.teamw.Messages.getString("Importer.import.message"), //$NON-NLS-1$
					new Object[] {
						hl7FileList.size(), downloadDir
					});
			if (SWTHelper.askYesNo(header, question)) {
				for (String filename : hl7FileList) {
					importFile(downloadDir + filename);
					log.log("Datei <" + filename + "> verarbeitet.", Log.INFOS); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (IOException e) {
			result = new Result<String>(SEVERITY.ERROR, 1, e.getMessage(), MY_LAB, true);
			ResultAdapter.displayResult(result,
				ch.elexis.laborimport.teamw.Messages.getString("Importer.error.import")); //$NON-NLS-1$
		} catch (FtpSemaException e) {
			result = new Result<String>(SEVERITY.WARNING, 1, e.getMessage(), MY_LAB, true);
			ResultAdapter.displayResult(result,
				ch.elexis.laborimport.teamw.Messages.getString("Importer.error.import")); //$NON-NLS-1$
		}
		
		return result;
	}
	
	private Result<?> importDirectBatch(){
		Result<String> result =
			new Result<String>(ch.elexis.laborimport.teamw.Messages.getString("Importer.ok")); //$NON-NLS-1$
		
		String batchFile =
			UtilFile.getCorrectPath(Hub.globalCfg.get(PreferencePage.BATCH_DATEI, "")); //$NON-NLS-1$
		String downloadDir =
			UtilFile.getCorrectPath(Hub.globalCfg.get(PreferencePage.DL_DIR,
				PreferencePage.DEFAULT_DL_DIR));
		
		if (batchFile == null || batchFile.length() == 0) {
			return new Result<String>(SEVERITY.ERROR, 1,
				ch.elexis.laborimport.teamw.Messages.getString("Importer.leereBatchdatei.error"), //$NON-NLS-1$
				MY_LAB, true);
		}
		
		try {
			Process process = Runtime.getRuntime().exec(batchFile);
			int exitValue = -1;
			try {
				exitValue = process.waitFor();
			} catch (InterruptedException e) {
				log.log(e.getMessage(), Log.INFOS);
			}
			if (exitValue != 0) {
				return new Result<String>(
					SEVERITY.ERROR,
					1,
					ch.elexis.laborimport.teamw.Messages.getString("Importer.batchFehler.error") + process.exitValue(), //$NON-NLS-1$
					MY_LAB, true);
			}
			
			List<String> hl7FileList = new Vector<String>();
			File ddDir = new File(downloadDir);
			
			String[] filenameList = ddDir.list();
			for (String filename : filenameList) {
				if (filename.toUpperCase().endsWith("HL7")) { //$NON-NLS-1$
					log.log("Datei <" + filename + "> downloaded.", //$NON-NLS-1$ //$NON-NLS-2$
						Log.INFOS);
					hl7FileList.add(filename);
				}
			}
			
			String header =
				MessageFormat.format(
					ch.elexis.laborimport.teamw.Messages.getString("Importer.import.header"), //$NON-NLS-1$
					new Object[] {
						MY_LAB
					});
			String question =
				MessageFormat.format(
					ch.elexis.laborimport.teamw.Messages.getString("Importer.import.message"), //$NON-NLS-1$
					new Object[] {
						hl7FileList.size(), downloadDir
					});
			if (SWTHelper.askYesNo(header, question)) {
				for (String filename : hl7FileList) {
					importFile(downloadDir + filename);
					log.log("Datei <" + filename + "> verarbeitet.", Log.INFOS); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (IOException e) {
			result = new Result<String>(SEVERITY.ERROR, 1, e.getMessage(), MY_LAB, true);
			ResultAdapter.displayResult(result,
				ch.elexis.laborimport.teamw.Messages.getString("Importer.error.import")); //$NON-NLS-1$
		}
		
		return result;
	}
	
	@Override
	public IStatus doImport(final IProgressMonitor monitor) throws Exception{
		int type;
		try {
			String sType = results[0];
			type = Integer.parseInt(sType);
		} catch (NumberFormatException ex) {
			type = FILE;
		}
		
		if ((type != FILE) && (type != DIRECT)) {
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
		return ch.elexis.laborimport.teamw.Messages.getString("Importer.title.description"); //$NON-NLS-1$
	}
	
	@Override
	public String getTitle(){
		return ch.elexis.laborimport.teamw.Messages.getString("Importer.lab") + MY_LAB; //$NON-NLS-1$
	}
	
	String getBasePath(){
		try {
			URL url = Platform.getBundle(PLUGIN_ID).getEntry("/"); //$NON-NLS-1$
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
		private final Button bFile;
		private final Button bDirect;
		
		private final Text tFilename;
		
		public LabImporter(final Composite parent, final ImporterPage home){
			super(parent, SWT.BORDER);
			setLayout(new GridLayout(3, false));
			
			bFile = new Button(this, SWT.RADIO);
			bFile.setText(ch.elexis.laborimport.teamw.Messages
				.getString("Importer.label.importFile")); //$NON-NLS-1$
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
			
			String batchOrFtp = Hub.globalCfg.get(PreferencePage.BATCH_OR_FTP, PreferencePage.FTP);
			String direktHerkunft =
				ch.elexis.laborimport.teamw.Messages.getString("Importer.ftp.label"); //$NON-NLS-1$
			if (PreferencePage.BATCH.equals(batchOrFtp)) {
				direktHerkunft =
					ch.elexis.laborimport.teamw.Messages.getString("Importer.batch.label"); //$NON-NLS-1$
			}
			bDirect = new Button(this, SWT.RADIO);
			bDirect.setText(ch.elexis.laborimport.teamw.Messages
				.getString("Importer.label.importDirect") + " (" + direktHerkunft + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			bDirect.setLayoutData(SWTHelper.getFillGridData(3, true, 1, false));
			
			int type = Hub.localCfg.get("ImporterPage/" + home.getTitle() + "/type", FILE); //$NON-NLS-1$ //$NON-NLS-2$
			
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
				
				tFilename.setText(""); //$NON-NLS-1$
				
				home.results[0] = new Integer(DIRECT).toString();
				home.results[1] = ""; //$NON-NLS-1$
			}
			
			SelectionAdapter sa = new SelectionAdapter() {
				@Override
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
						
						tFilename.setText(""); //$NON-NLS-1$
						
						home.results[0] = new Integer(DIRECT).toString();
						home.results[1] = ""; //$NON-NLS-1$
						
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/type", DIRECT); //$NON-NLS-1$ //$NON-NLS-2$
						Hub.localCfg.set("ImporterPage/" + home.getTitle() + "/filename", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			};
			
			bFile.addSelectionListener(sa);
			bDirect.addSelectionListener(sa);
			
			bBrowse.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e){
					bFile.setSelection(true);
					bDirect.setSelection(false);
					
					FileDialog fdl = new FileDialog(parent.getShell(), SWT.OPEN);
					fdl.setFilterExtensions(new String[] {
						"*"}); //$NON-NLS-1$
					fdl.setFilterNames(new String[] {
						Messages.getString("ImporterPage.allFiles")}); //$NON-NLS-1$
					String filename = fdl.open();
					if (filename == null) {
						filename = ""; //$NON-NLS-1$
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
