package ch.elexis.buchhaltung.util;

import ch.rgw.tools.StringTool;

public class PatientIdFormatter {
	
	int stellen;
	
	public PatientIdFormatter(int stellen){
		this.stellen = stellen;
	}
	
	public String format(String id){
		if (id == null) {
			id = ""; //$NON-NLS-1$
		}
		return StringTool.pad(StringTool.LEFT, '0', id, stellen);
	}
}
