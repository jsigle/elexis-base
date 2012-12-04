package ch.elexis.laborimport.analytica;

public class HL7 extends ch.elexis.importers.HL7 {
	
	public HL7(String labor, String kuerzel){
		super(labor, kuerzel);
	}
	
	/**
	 * Extract all comments (NTE), global and OBX comments
	 * 
	 * @return a string containing all comments, separated by newlines
	 */
	protected String getComments(String[] hl7Rows){
		StringBuffer comments = new StringBuffer();
		
		String lastObxName = "";
		
		for (int i = 0; i < hl7Rows.length; i++) {
			if (hl7Rows[i].startsWith("NTE")) {
				String[] nte = hl7Rows[i].split(getSeparator());
				if (nte.length > 3) {
					if (lastObxName.length() > 0) {
						comments.append(lastObxName);
						comments.append(": ");
					}
					comments.append(nte[3]);
					comments.append("\n");
				}
			} else if (hl7Rows[i].startsWith("OBX")) {
				String[] obx = hl7Rows[i].split(getSeparator());
				String raw = obx[3];
				String[] split = raw.split("\\^");
				String obxName;
				if (split.length > 1) {
					obxName = split[1];
				} else {
					obxName = split[0];
				}
				lastObxName = obxName;
			}
		}
		
		return comments.toString();
	}
	
	/**
	 * Findet Kommentare zu einem OBX. NTE Kommentar nachfolgend abgelegt
	 * 
	 * @param hl7Rows
	 * @return String
	 */
	protected String getOBXComments(String[] hl7Rows, String[] obxFields){
		StringBuilder ret = new StringBuilder();
		
		int i = 0;
		boolean started = false;
		boolean end = false;
		while (i < hl7Rows.length && !end) {
			if (hl7Rows[i].startsWith("OBX")) { //$NON-NLS-1$
				String[] obx = hl7Rows[i].split(getSeparator());
				if (started) {
					end = true;
				} else {
					started =
						(obx[1].equals(obxFields[1]) && obx[2].equals(obxFields[2]) && obx[3]
							.equals(obxFields[3]));
				}
			} else if (hl7Rows[i].startsWith("OBR")) { // Nach OBR kommen OBR Kommentare //$NON-NLS-1$
				if (started) {
					end = true;
				}
			} else if (started && hl7Rows[i].startsWith("NTE")) { //$NON-NLS-1$
				String[] nte = hl7Rows[i].split(getSeparator());
				ret.append(nte[3]).append("\n"); //$NON-NLS-1$
			}
			i++;
		}
		return ret.toString();
	}
}
