/*******************************************************************************
 * Copyright (c) 2007-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    $Id: Importer.java 5105 2009-02-06 15:44:15Z rgw_ch $
 *******************************************************************************/
package ch.elexis.eigendiagnosen.data;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import au.com.bytecode.opencsv.CSVReader;
import ch.elexis.data.Query;
import ch.elexis.importers.ExcelWrapper;
import ch.elexis.util.ImporterPage;
import ch.elexis.util.Log;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;

public class Importer extends ImporterPage {
	/**
	 * Create the page that will let the user select a file to import. For simplicity, we use the
	 * default FileBasedImporter of our superclass.
	 */
	@Override
	public Composite createPage(final Composite parent){
		FileBasedImporter fbi = new FileBasedImporter(parent, this);
		fbi.setFilter(new String[] {
			"*.csv", "*.xls", "*"
		}, new String[] {
			"Character Separated Values", "Microsoft Excel 97", "All Files"
		});
		fbi.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		return fbi;
	}
	
	/**
	 * The import process starts when the user has selected a file and clicked "OK". Warning: We can
	 * not read fields of the page created in createPage here! (The page is already disposed when
	 * doImport is called). If we have to transfer field values between createPage and doImport, we
	 * must override collect(). Our file based importer saves the user input in results[0]
	 */
	@Override
	public IStatus doImport(final IProgressMonitor monitor) throws Exception{
		File file = new File(results[0]);
		if (!file.canRead()) {
			log.log("Can't read " + results[0], Log.ERRORS);
			return new Status(Status.ERROR, "ch.elexis.privatrechnung", "Can't read " + results[0]);
		}
		Result<String> res;
		if (results[0].endsWith(".xls")) {
			res = importExcel(file.getAbsolutePath(), monitor);
		} else if (results[0].endsWith(".csv")) {
			res = importCSV(file.getAbsolutePath(), monitor);
		} else {
			return new Status(Status.ERROR, "ch.elexis.privatrechnung", "Unsupported file format");
		}
		if (res.isOK()) {
			
		}
		return ResultAdapter.getResultAsStatus(res);
	}
	
	/**
	 * return a description to display in the message area of the import dialog
	 */
	@Override
	public String getDescription(){
		return "Import aus CSV und Excel";
	}
	
	/**
	 * return a title to display in the title bar of the import dialog
	 */
	@Override
	public String getTitle(){
		return Eigendiagnose.CODESYSTEM_NAME;
	}
	
	private Result<String> importExcel(final String file, final IProgressMonitor mon){
		ExcelWrapper xl = new ExcelWrapper();
		if (!xl.load(file, 0)) {
			return new Result<String>(Result.SEVERITY.ERROR, 1, "Bad file format", file, true);
		}
		for (int i = xl.getFirstRow(); i <= xl.getLastRow(); i++) {
			List<String> row = xl.getRow(i);
			importLine(row.toArray(new String[0]));
		}
		return new Result<String>("OK");
	}
	
	private Result<String> importCSV(final String file, final IProgressMonitor mon){
		try {
			CSVReader cr = new CSVReader(new FileReader(file));
			String[] line;
			while ((line = cr.readNext()) != null) {
				importLine(line);
			}
			return new Result<String>("OK");
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return new Result<String>(Result.SEVERITY.ERROR, 1, "Could not read " + file,
				ex.getMessage(), true);
		}
		
	}
	
	private void importLine(final String[] line){
		Query<Eigendiagnose> qbe = new Query<Eigendiagnose>(Eigendiagnose.class);
		qbe.add("Kuerzel", "=", line[1]);
		List<Eigendiagnose> f = qbe.execute();
		if (f != null && f.size() > 0) {
			Eigendiagnose ed = f.get(0);
			ed.set(new String[] {
				"parent", "Kuerzel", "Text", "Kommentar"
			}, line[0], line[1], line[2], line[3]);
		} else {
			new Eigendiagnose(line[0], line[1], line[2], line[3]);
		}
		
	}
}
