package ch.elexis.export.hl7;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LastUpdate {
	
	private static final String lastUpdateFilename = "lastupdate.id"; //$NON-NLS-1$
	
	/**
	 * Liest LastUpdate Datei
	 */
	public static String readLastUpdate() throws IOException{
		File file = new File(lastUpdateFilename);
		if (!file.exists()) {
			return "0"; //$NON-NLS-1$
		}
		
		FileInputStream input = null;
		byte[] daten = null;
		try {
			input = new FileInputStream(lastUpdateFilename);
			daten = new byte[input.available()];
			input.read(daten);
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return new String(daten);
	}
	
	/**
	 * Schreibt LastUpdate in Datei
	 */
	public static void writeLastUpdate(final long lastUpdate) throws IOException{
		byte[] daten = new Long(lastUpdate).toString().getBytes();
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(lastUpdateFilename);
			output.write(daten);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}
}
