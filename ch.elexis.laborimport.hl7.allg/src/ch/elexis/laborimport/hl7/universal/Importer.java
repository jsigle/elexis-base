package ch.elexis.laborimport.hl7.universal;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.importers.HL7Parser;
import ch.elexis.util.ResultAdapter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Result;

public class Importer extends Action implements IAction {
	public static final String MY_LAB = "Eigenlabor";
	
	private HL7Parser hlp = new HL7Parser(MY_LAB);
	
	public Importer(){
		super("Hl7 Datei", Desk.getImageDescriptor(Desk.IMG_IMPORT));
	}
	
	@Override
	public void run(){
		File dir = new File(Hub.localCfg.get(Preferences.CFG_DIRECTORY, File.separator));
		if ((!dir.exists()) || (!dir.isDirectory())) {
			SWTHelper.showError("bad directory for import", "Konfigurationsfehler",
				"Das Transferverzeichnis ist nicht korrekt eingestellt");
		} else {
			File archiveDir = new File(dir, "archive");
			if (!archiveDir.exists()) {
				archiveDir.mkdir();
			}
			File errorDir = new File(dir, "fehlerhaft");
			if (!errorDir.exists()) {
				errorDir.mkdir();
			}
			int err = 0;
			int files = 0;
			Result<?> r = null;
			for (String fn : dir.list(new FilenameFilter() {
				
				public boolean accept(File arg0, String arg1){
					if (arg1.toLowerCase().endsWith(".hl7")) {
						return true;
					}
					return false;
				}
			})) {
				files++;
				File hl7file = new File(dir, fn);
				r = hlp.importFile(hl7file, archiveDir, false);
				if (!r.isOK()) {
					err++;
					File errFile = new File(errorDir, fn);
					hl7file.renameTo(errFile);
				}
			}
			if (err > 0) {
				ResultAdapter.displayResult(r,
					Integer.toString(err) + " von " + Integer.toString(files)
						+ " Dateien hatten Fehler\n");
			} else if (files == 0) {
				SWTHelper.showInfo("Laborimport", "Es waren keine Dateien zum Import vorhanden");
			} else {
				SWTHelper.showInfo("Laborimport", Integer.toString(files)
					+ " Dateien wurden fehlerfrei verarbeitet.");
			}
		}
	}
	
}
