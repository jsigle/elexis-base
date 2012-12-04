/*******************************************************************************
 * Copyright (c) 2006-2011, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *******************************************************************************/

package ch.elexis.omnivore.data;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.program.Program;

import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.core.PersistenceException;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.omnivore.views.FileImportDialog;
import ch.elexis.text.IOpaqueDocument;
import ch.elexis.util.Log;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.VersionInfo;

public class DocHandle extends PersistentObject implements IOpaqueDocument {
	public static final String TABLENAME = "CH_ELEXIS_OMNIVORE_DATA"; //$NON-NLS-1$
	public static final String DBVERSION = "1.2.1"; //$NON-NLS-1$
	public static final String createDB = "CREATE TABLE " + TABLENAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
		"ID				VARCHAR(25) primary key," + //$NON-NLS-1$
		"lastupdate     BIGINT," + //$NON-NLS-1$
		"deleted        CHAR(1) default '0'," + //$NON-NLS-1$
		"PatID			VARCHAR(25)," + //$NON-NLS-1$
		"Datum			CHAR(8)," + //$NON-NLS-1$
		"Title 			VARCHAR(80)," + //$NON-NLS-1$
		"Mimetype		VARCHAR(255)," + //$NON-NLS-1$
		"Keywords		VARCHAR(255)," + //$NON-NLS-1$
		"Path			VARCHAR(255)," + //$NON-NLS-1$
		"Doc			BLOB);" + //$NON-NLS-1$
		"CREATE INDEX OMN1 ON " + TABLENAME + " (PatID);" + //$NON-NLS-1$ //$NON-NLS-2$
		"CREATE INDEX OMN2 ON " + TABLENAME + " (Keywords);" + //$NON-NLS-1$ //$NON-NLS-2$
		"INSERT INTO " + TABLENAME + " (ID, TITLE) VALUES ('1','" + DBVERSION + "');"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	public static final String upd120 =
		"ALTER TABLE " + TABLENAME + " MODIFY Mimetype VARCHAR(255);" + //$NON-NLS-1$ //$NON-NLS-2$
			"ALTER TABLE " + TABLENAME + " MODIFY Keywords VARCHAR(255);" + //$NON-NLS-1$ //$NON-NLS-2$
			"ALTER TABLE " + TABLENAME + " Modify Path VARCHAR(255);"; //$NON-NLS-1$ //$NON-NLS-2$
	
	private static final String upd121 = "ALTER TABLE " + TABLENAME + " ADD lastupdate BIGINT;"; //$NON-NLS-1$ //$NON-NLS-2$
	
	static {
		addMapping(TABLENAME, "PatID", "Datum=S:D:Datum", "Titel=Title", "Keywords", "Path", "Doc", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"Mimetype"); //$NON-NLS-1$
		DocHandle start = load("1"); //$NON-NLS-1$
		if (start == null) {
			init();
		} else {
			VersionInfo vi = new VersionInfo(start.get("Titel")); //$NON-NLS-1$
			if (vi.isOlder(DBVERSION)) {
				if (vi.isOlder("1.1.0")) { //$NON-NLS-1$
					getConnection().exec(
						"ALTER TABLE " + TABLENAME + " ADD deleted CHAR(1) default '0';"); //$NON-NLS-1$ //$NON-NLS-2$
					start.set("Titel", DBVERSION); //$NON-NLS-1$
				}
				if (vi.isOlder("1.2.0")) { //$NON-NLS-1$
					createOrModifyTable(upd120);
					start.set("Titel", DBVERSION); //$NON-NLS-1$
				}
				if (vi.isOlder("1.2.1")) { //$NON-NLS-1$
					createOrModifyTable(upd121);
					start.set("Titel", DBVERSION); //$NON-NLS-1$
				}
				
			}
		}
	}
	
	public DocHandle(byte[] doc, Patient pat, String title, String mime, String keyw){
		if ((doc == null) || (doc.length == 0)) {
			SWTHelper.showError(Messages.DocHandle_docErrorCaption,
				Messages.DocHandle_docErrorMessage);
			return;
		}
		create(null);
		try {
			setBinary("Doc", doc); //$NON-NLS-1$
			set(new String[] {
				"PatID", "Datum", "Titel", "Keywords", "Mimetype" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}, pat.getId(), new TimeTool().toString(TimeTool.DATE_GER), title, keyw, mime);
		} catch (PersistenceException pe) {
			log.log(Messages.DocHandle_dataNotWritten + "; " + pe.getMessage(), Log.ERRORS);
		}
	}
	
	/**
	 * Tabelle neu erstellen
	 */
	public static void init(){
		createOrModifyTable(createDB);
	}
	
	public static DocHandle load(String id){
		DocHandle ret = new DocHandle(id);
		if (ret.exists()) {
			return ret;
		}
		return null;
	}
	
	@Override
	public String getLabel(){
		StringBuilder sb = new StringBuilder();
		sb.append(get("Datum")).append(" ").append(get("Titel")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sb.toString();
	}
	
	@Override
	public boolean delete(){
		return super.delete();
	}
	
	public byte[] getContentsAsBytes(){
		byte[] ret = getBinary("Doc"); //$NON-NLS-1$
		return ret;
	}
	
	public InputStream getContentsAsStream(){
		byte[] bytes = getContentsAsBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}
	
	public void execute(){
		try {
			String ext = ""; //$NON-NLS-1$
			String typname = get("Mimetype"); //$NON-NLS-1$
			int r = typname.lastIndexOf('.');
			if (r == -1) {
				typname = get("Titel"); //$NON-NLS-1$
				r = typname.lastIndexOf('.');
			}
			
			if (r != -1) {
				ext = typname.substring(r + 1);
			}
			File temp = File.createTempFile("omni_", "_vore." + ext); //$NON-NLS-1$ //$NON-NLS-2$
			temp.deleteOnExit();
			byte[] b = getBinary("Doc"); //$NON-NLS-1$
			if (b == null) {
				SWTHelper.showError(Messages.DocHandle_readErrorCaption,
					Messages.DocHandle_readErrorMessage);
				return;
			}
			FileOutputStream fos = new FileOutputStream(temp);
			fos.write(b);
			fos.close();
			Program proggie = Program.findProgram(ext);
			if (proggie != null) {
				proggie.execute(temp.getAbsolutePath());
			} else {
				if (Program.launch(temp.getAbsolutePath()) == false) {
					Runtime.getRuntime().exec(temp.getAbsolutePath());
				}
				
			}
			
		} catch (Exception ex) {
			ExHandler.handle(ex);
			SWTHelper.showError(Messages.DocHandle_execError, ex.getMessage());
		}
	}
	
	@Override
	protected String getTableName(){
		return TABLENAME;
	}
	
	protected DocHandle(String id){
		super(id);
	}
	
	protected DocHandle(){}
	
	public static void assimilate(String f){
		Patient act = ElexisEventDispatcher.getSelectedPatient();
		if (act == null) {
			SWTHelper.showError(Messages.DocHandle_noPatientSelected,
				Messages.DocHandle_pleaseSelectPatien);
			return;
		}
		File file = new File(f);
		if (!file.canRead()) {
			SWTHelper.showError(Messages.DocHandle_cantReadCaption,
				MessageFormat.format(Messages.DocHandle_cantReadMessage, f));
			return;
		}
		FileImportDialog fid = new FileImportDialog(file.getName());
		if (fid.open() == Dialog.OK) {
			try {
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int in;
				while ((in = bis.read()) != -1) {
					baos.write(in);
				}
				bis.close();
				baos.close();
				String nam = file.getName();
				if (nam.length() > 255) {
					SWTHelper.showError(Messages.DocHandle_importErrorCaption,
						Messages.DocHandle_importErrorMessage);
					return;
				}
				new DocHandle(baos.toByteArray(), act, fid.title, file.getName(), fid.keywords);
			} catch (Exception ex) {
				ExHandler.handle(ex);
				SWTHelper.showError(Messages.DocHandle_importErrorCaption,
					Messages.DocHandle_importErrorMessage2);
			}
		}
		
	}
	
	@Override
	public String getTitle(){
		return checkNull(get("Titel")); //$NON-NLS-1$
	}
	
	@Override
	public String getMimeType(){
		return checkNull(get("Mimetype")); //$NON-NLS-1$
	}
	
	@Override
	public String getKeywords(){
		return checkNull(get("Keywords")); //$NON-NLS-1$
	}
	
	@Override
	public String getCategory(){
		return ""; //$NON-NLS-1$
	}
	
	@Override
	public String getCreationDate(){
		return get("Datum"); //$NON-NLS-1$
	}
	
	@Override
	public Patient getPatient(){
		return Patient.load(get("PatID")); //$NON-NLS-1$
	}
	
	@Override
	public String getGUID(){
		return getId();
	}
	
}
