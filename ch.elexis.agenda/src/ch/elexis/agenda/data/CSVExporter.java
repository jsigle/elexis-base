package ch.elexis.agenda.data;

import java.io.FileWriter;
import java.util.List;

import ch.elexis.scripting.CSVWriter;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

public class CSVExporter {
	
	public void doExport(String filename, List<Termin> termine){
		try {
			CSVWriter csv = new CSVWriter(new FileWriter(filename));
			String[] header =
				new String[] {
					"UUID", "Bereich", "Typ", "Datum", "Startzeit", "Dauer", "Grund",
					"Patient-UUID-oder-Name"
				};
			String[] fields =
				new String[] {
					"ID", Termin.FLD_BEREICH, Termin.FLD_TERMINTYP, Termin.FLD_TAG,
					Termin.FLD_BEGINN, Termin.FLD_DAUER, Termin.FLD_GRUND, Termin.FLD_PATIENT
				};
			csv.writeNext(header);
			for (Termin t : termine) {
				String[] line = new String[fields.length];
				t.get(fields, line);
				csv.writeNext(line);
			}
			csv.close();
			SWTHelper.showInfo("Termine exportiert", "Der Export nach " + filename
				+ " ist abgeschlossen");
		} catch (Exception ex) {
			ExHandler.handle(ex);
			SWTHelper.showError("Fehler", ex.getMessage());
		}
		
	}
	
}
