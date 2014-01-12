package ch.elexis.export.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UtilFile {
	public static final String TEXT_ENCODING = "ISO-8859-1"; //$NON-NLS-1$
	
	public static String getCorrectSeparators(final String pathOrFilename){
		return pathOrFilename.replace("\\", File.separator).replace("//", File.separator)
			.replace("/", File.separator);
	}
	
	/**
	 * Retourniert Dateinamen ohne Pfad als String
	 */
	public static String getFilename(final File file){
		String correctFilenamePath = getCorrectSeparators(file.getPath());
		
		if (correctFilenamePath.indexOf(File.separator) < 0) {
			return file.getPath();
		}
		return correctFilenamePath.substring(correctFilenamePath.lastIndexOf(File.separator) + 1,
			correctFilenamePath.length());
	}
	
	/**
	 * Retourniert Dateinamen ohne Pfad und Endung. Falls keine Endung vorhanden ist, wird der
	 * Dateinamen retourniert.
	 */
	public static String getNakedFilename(final File file){
		String filename = getFilename(file);
		
		if (filename.lastIndexOf(".") > 0) {
			return filename.substring(0, filename.lastIndexOf("."));
		}
		
		return filename;
	}
	
	/**
	 * Schreibt Text Datei
	 */
	public static void writeTextFile(final File file, final String text) throws IOException{
		byte[] encodedData = text.getBytes(TEXT_ENCODING);
		if (text != null) {
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
				output.write(encodedData);
			} finally {
				if (output != null) {
					output.close();
				}
			}
		}
	}
	
	/**
	 * Liest Textdatei.
	 */
	public static String readTextFile(final File file) throws IOException{
		FileInputStream input = null;
		byte[] daten = null;
		try {
			input = new FileInputStream(file);
			daten = new byte[input.available()];
			input.read(daten);
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return new String(daten, TEXT_ENCODING);
	}
}
