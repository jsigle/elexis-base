/**
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     G. Weirich - initial API and implementation
 * All the rest is done generically. See plug-in elexis-importer.
 * Adapted to Bioanalytica by Daniel Lutz <danlutz@watz.ch>
 */

package ch.elexis.laborimport.bioanalytica;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Labor;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;
import ch.elexis.dialogs.KontaktSelektor;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * This class parses a HL7 file containing lab results. It tries to comply with several possible
 * Substandards of the HL7 and to return always reasonable values for each field.
 * 
 * @author Gerry
 * 
 */
public class HL7 {
	private String separator;
	private String labName;
	private String labID;
	
	private String[] lines;
	// private Kontakt labor;
	// Patient pat = null;
	
	private static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
	private static final String DEFAULT_CHARSET = CHARSET_ISO_8859_1;
	
	/**
	 * We can force this hl7 to be attributed to a specific lab (if we know, who the sender should
	 * be) by providing a name and a short name. If we pass null, the lab will be taken out of the
	 * file (if a sender is provided here)
	 * 
	 * @param labor
	 *            String
	 * @param kuerzel
	 *            String
	 */
	public HL7(String labor, String kuerzel){
		labName = labor;
		labID = kuerzel;
	}
	
	/**
	 * Load file into memory and break it up to separate lines. All other methods should only be
	 * called after load was successful. To comply with some of the many standards around, we accept
	 * \n and \r and any combination thereof as field separators
	 * 
	 * @param file
	 *            the file
	 * @return
	 */
	public Result<String> load(File file){
		if (!file.canRead()) {
			return new Result<String>(Result.SEVERITY.ERROR, 1, "Kann Datei nicht lesen",
				file.getAbsolutePath(), true);
		}
		try {
			// FileReader fr=new FileReader(file);
			// simulate FileReader and explicitly set charset
			System.err.println(Charset.defaultCharset());
			InputStreamReader fr =
				new InputStreamReader(new FileInputStream(file), DEFAULT_CHARSET);
			char[] in = new char[(int) file.length()];
			if (fr.read(in) != in.length) {
				return new Result<String>(Result.SEVERITY.ERROR, 3, "EOF", file.getAbsolutePath(),
					true);
			}
			String hl7raw = new String(in);
			lines = hl7raw.split("[\\r\\n]+");
			separator = "\\" + lines[0].substring(3, 4);
			fr.close();
			return new Result<String>("OK");
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return new Result<String>(Result.SEVERITY.ERROR, 2, "Exception beim Lesen",
				ex.getMessage(), true);
		} finally {}
		
	}
	
	/**
	 * find a single HL7-Record
	 * 
	 * @param header
	 *            header identifying the desired record
	 * @param start
	 *            what line to start scanning
	 * @return the first occurence of an element of type 'header' after 'start' lines or an empty
	 *         Element if no such record was found
	 */
	private String[] getElement(String header, int start){
		for (int i = start; i < lines.length; i++) {
			if (lines[i].startsWith(header)) {
				return lines[i].split(separator);
			}
		}
		return new String[0];
	}
	
	/**
	 * Find the patient denoted by this HL7-record. If it can't be found in the database, ask the
	 * user to choose one.
	 * 
	 * @return the Patient or null if it has not been found, or an error indicating the problem
	 */
	public Result<Patient> getPatient(){
		// avoid global variables
		Patient pat = null;
		
		if (pat == null) {
			String[] elPid = getElement("PID", 0);
			String patid = elPid[2];
			if (StringTool.isNothing(patid)) {
				patid = elPid[3];
				if (StringTool.isNothing(patid)) {
					patid = elPid[4];
					if (patid == null) {
						patid = "";
					}
				}
			}
			String[] pidflds = patid.split("[\\^ ]+");
			String pid = pidflds[pidflds.length - 1];
			
			// Find a patient with the given ID
			Query<Patient> qbe = new Query<Patient>(Patient.class);
			qbe.add("PatientNr", "=", pid);
			List<Patient> list = qbe.execute();
			String[] name = elPid[5].split("\\^");
			String gebdat = elPid[7];
			// sex may be not available
			String sex = "?";
			if (elPid[8] != null) {
				sex = elPid[8].equalsIgnoreCase("M") ? "m" : "w";
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append(name[0]);
			sb.append(" ");
			sb.append(name[1]);
			sb.append("(");
			sb.append(sex);
			sb.append("), ");
			sb.append(new TimeTool(gebdat).toString(TimeTool.DATE_GER));
			String labLabel = sb.toString();
			
			if (list.size() == 0) {
				// ask the user to choose one
				
				// use PatientSelektorAdapter, since we don't run in the main thread
				PatientSelektorAdapter psa =
					new PatientSelektorAdapter("Patient wählen", "Der Patient \"" + labLabel
						+ "\" wurde anhand der Nummer des Labors nicht gefunden. Bitte wählen Sie"
						+ " einen Patienten aus.");
				psa.open();
				if (psa.getPatient() != null) {
					pat = psa.getPatient();
				} else {
					return new Result<Patient>(Result.SEVERITY.WARNING, 1,
						"Patient nicht in Datenbank (ID: " + pid + ")", null, true);
				}
			} else {
				// if the patient with the given ID was found, we verify, if it is the correct name
				// and sex
				pat = list.get(0);
				if (!pat.getName().equalsIgnoreCase(name[0])
					|| !pat.getVorname().equalsIgnoreCase(name[1])
					|| !pat.getGeschlecht().equals(sex)) {
					String elexisLabel = pat.getLabel();
					pat = null;
					
					// ask the user for a different patient
					// use PatientSelektorAdapter, since we don't run in the main thread
					PatientSelektorAdapter psa =
						new PatientSelektorAdapter(
							"Patient wählen",
							"Der Patient \""
								+ labLabel
								+ "\" stimmt nicht mit dem in Elexis gefundene Patienten \""
								+ elexisLabel
								+ "\" überein."
								+ " Bitte wählen Sie einen Patienten aus, oder wählen Sie \"Abbrechen\", um diese Labordaten zu ignorieren.");
					psa.open();
					if (psa.getPatient() != null) {
						pat = psa.getPatient();
					} else {
						return new Result<Patient>(Result.SEVERITY.WARNING, 4,
							"Patient mit dieser ID (" + pid + ") schon mit anderem Namen vorhanden"
								+ "(Labor: " + labLabel + ", Elexis: " + elexisLabel + ")", null,
							true);
					}
				}
			}
		}
		return new Result<Patient>(pat);
	}
	
	/**
	 * Find the lab issuing this file. If we provided a lab name in ze constructor, ths will return
	 * that lab.
	 * 
	 * @return the lab or null if it could not be found
	 */
	public Result<Kontakt> getLabor(){
		// avoid global variables
		Labor labor = null;
		
		if (labor == null) {
			if (labName == null) {
				if (lines.length > 1) {
					String[] msh = getElement("MSH", 0);
					if (msh.length > 4) {
						labName = msh[4];
						if (labID == null) {
							labID = msh[4].length() > 10 ? msh[4].substring(0, 10) : msh[4];
						}
					}
				}
			}
			Query<Labor> qbe = new Query<Labor>(Labor.class);
			qbe.startGroup();
			qbe.add("Kuerzel", "LIKE", "%" + labName + "%");
			qbe.or();
			qbe.add("Name", "LIKE", "%" + labName + "%");
			qbe.or();
			qbe.add("Kuerzel", "=", labID);
			qbe.endGroup();
			List<Labor> list = qbe.execute();
			if (list.size() != 1) {
				labor = new Labor(labName, "Labor " + labName);
			} else {
				labor = list.get(0);
			}
		}
		return new Result<Kontakt>(labor);
	}
	
	/**
	 * Extract all comments (NTE), global and OBX comments
	 * 
	 * @return a string containing all comments, separated by newlines
	 */
	public String getComments(){
		StringBuffer comments = new StringBuffer();
		
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("NTE")) {
				String[] nte = lines[i].split(separator);
				if (nte.length > 3) {
					String rawComment;
					String source = nte[1];
					if (source.matches("^0*$")) {
						// independent comment
						rawComment = nte[3];
					} else {
						// OBX comment
						String obxName = getItemNameForNTE(source);
						rawComment = obxName + ": " + nte[3];
					}
					comments.append(rawComment);
					comments.append("\n");
				}
			}
		}
		
		return comments.toString();
	}
	
	/**
	 * Get Item Name in OBX corresponding to NTE Helper method for getComments()
	 * 
	 * @param source
	 * @return the item's name, or "" if not found
	 */
	private String getItemNameForNTE(String source){
		String[] obx;
		int i = findNext("OBX", 0);
		while (i != -1) {
			obx = lines[i].split(separator);
			if (obx[1].equals(source)) {
				String raw = obx[3];
				String[] split = raw.split("\\^");
				String obxName;
				if (split.length > 1) {
					obxName = split[1];
				} else {
					obxName = split[0];
				}
				
				return obxName;
			}
			
			i = findNext("OBX", i + 1);
		}
		
		// not found
		return "";
	}
	
	/**
	 * Find the first OBR record in the file
	 * 
	 * @return an OBR or null if none was found
	 */
	OBR firstOBR(){
		return new OBR(0).nextOBR(0);
	}
	
	/**
	 * Find the index of the next Element of a given type
	 * 
	 * @param type
	 *            String
	 * @param prev
	 *            position to start searching
	 * @return
	 */
	int findNext(String type, int prev){
		for (int i = prev; i < lines.length; i++) {
			if (lines[i].startsWith(type)) {
				return i;
			}
		}
		return -1;
	}
	
	class OBR {
		int of;
		String[] field;
		
		OBR(int off){
			of = off;
			field = lines[of].split(separator);
		}
		
		OBR nextOBR(OBR obr){
			return nextOBR(obr.of);
		}
		
		OBR nextOBR(int of){
			int n = findNext("OBR", of + 1);
			if (n == -1) {
				return null;
			}
			return new OBR(n);
		}
		
		/**
		 * Find the next OBX after a given OBX
		 * 
		 * @param old
		 *            the OBX from which to start searching
		 * @return the next OBX or null if none was found
		 */
		OBX nextOBX(OBX old){
			return nextOBX(old.of);
		}
		
		/**
		 * Find the first OBX of this OBR
		 * 
		 * @return an OBX or null if none found
		 */
		OBX firstOBX(){
			while (++of < lines.length) {
				if (lines[of].startsWith("OBX")) {
					return new OBX(this, of);
				}
				if (lines[of].startsWith("OBR")) {
					return null;
				}
			}
			return null;
		}
		
		/**
		 * Find the next OBX after a given position
		 * 
		 * @param old
		 *            the position to start looking from
		 * @return the first OBX after 'old' or null if none was found
		 */
		OBX nextOBX(int old){
			int nf = old + 1;
			while (true) {
				if (nf >= lines.length) {
					return null;
				}
				if (lines[nf].startsWith("OBX")) {
					return new OBX(this, nf);
				}
				if (lines[nf].startsWith("OBR")) {
					return null;
				}
				nf += 1;
			}
		}
		
		/**
		 * Unfortunately, not all labs use all date fields. So we try several possible positions.
		 * 
		 * @return the OBR's date. If none was found, it will be the date of the first OBX found
		 */
		TimeTool getDate(){
			String date = field[7];
			if (StringTool.isNothing(date)) {
				if (field.length > 22) {
					date = field[22];
				} else {
					date = field[6];
					if (date.length() == 0) {
						OBX obx = firstOBX();
						if (obx != null) {
							return obx.getDate();
						} else {
							return new TimeTool();
						}
					}
				}
			}
			TimeTool tt = makeTime(date);
			return tt;
		}
		
	}
	
	class OBX {
		int of;
		String[] obxFields;
		OBR myOBR;
		
		OBX(OBR obr, int off){
			of = off;
			obxFields = lines[of].split(separator);
			myOBR = obr;
		}
		
		public String getObxNr(){
			return obxFields[1];
		}
		
		public String getItemCode(){
			return obxFields[3].split("\\^")[0];
		}
		
		public String getItemName(){
			String raw = getField(3);
			String[] split = raw.split("\\^");
			if (split.length > 1) {
				return split[1];
			}
			return split[0];
		}
		
		public String getResultValue(){
			return getField(5);
		}
		
		public String getUnits(){
			return getField(6);
		}
		
		public String getRefRange(){
			return getField(7);
		}
		
		/**
		 * Unfortunately, the date field is not provided by all applications. If we don't find an
		 * OBX date, we use the OBR date.
		 * 
		 * @return
		 */
		public TimeTool getDate(){
			String tim = getField(14);
			if (tim.length() == 0) {
				return myOBR.getDate();
			}
			return makeTime(tim);
		}
		
		/**
		 * This is greatly simplified from the possible values <<, <, >,>>, +, ++, -, -- and so on
		 * we just say "it's pathologic".
		 * 
		 * @return true if it's any of the pathologic values.
		 */
		public boolean isPathologic(){
			String abnormalFlag = getField(8);
			if (StringTool.isNothing(abnormalFlag)) {
				return false;
			}
			return true;
		}
		
		public boolean isFormattedText(){
			return (obxFields[2].equals("FT"));
		}
		
		/**
		 * Find the comment field of this OBX. Funny enough this is stored outside of the OBX
		 * usually. To make things simpler, we put all comments one after another in the same string
		 * (why not?).
		 * 
		 * @return The comment (that can be an empty String or might contain several NTE records)
		 */
		public String getComment(){
			StringBuilder ret = new StringBuilder();
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].startsWith("NTE")) {
					String[] nte = lines[i].split(separator);
					if (nte.length > 1) {
						if (nte[1].equals(getObxNr())) {
							if (nte.length > 3) {
								ret.append(nte[3]).append("\n");
							}
						}
					}
				}
			}
			return ret.toString();
			
		}
		
		private String getField(int f){
			if (obxFields.length > f) {
				return obxFields[f];
			}
			return "";
		}
	}
	
	public static TimeTool makeTime(String datestring){
		String date = datestring.substring(0, 8);
		TimeTool ret = new TimeTool();
		if (ret.set(date)) {
			return ret;
		}
		return null;
	}
	
	private class PatientSelektorAdapter {
		String title;
		String message;
		
		Patient pat = null;
		
		PatientSelektorAdapter(String title, String message){
			this.title = title;
			this.message = message;
		}
		
		void open(){
			Desk.getDisplay().syncExec(new Runnable() {
				public void run(){
					KontaktSelektor ksl =
						new KontaktSelektor(Hub.getActiveShell(), Patient.class,
							PatientSelektorAdapter.this.title, PatientSelektorAdapter.this.message,
							Patient.DEFAULT_SORT);
					if (ksl.open() == Dialog.OK) {
						Kontakt sel = (Kontakt) ksl.getSelection();
						if (sel instanceof Patient) {
							pat = (Patient) sel;
						}
					}
				}
			});
		}
		
		Patient getPatient(){
			return pat;
		}
	}
}
