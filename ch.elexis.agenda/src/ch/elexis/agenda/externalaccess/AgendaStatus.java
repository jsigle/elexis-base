package ch.elexis.agenda.externalaccess;

import ch.elexis.actions.Activator;
import ch.rgw.tools.TimeTool;

public class AgendaStatus {
	
	private static Activator agenda;
	
	static {
		agenda = Activator.getDefault();
	}
	
	/**
	 * 
	 * @return the currently selected date
	 */
	public static TimeTool getSelectedDate(){
		return agenda.getActDate();
	}
	
	/**
	 * 
	 * @return the currently selected Bereich
	 */
	public static String getSelectedBereich(){
		return agenda.getActResource();
	}
	
}
