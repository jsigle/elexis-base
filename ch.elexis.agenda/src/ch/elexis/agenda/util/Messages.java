package ch.elexis.agenda.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.agenda.util.messages"; //$NON-NLS-1$
	public static String Plannables_databaseError;
	public static String Plannables_errorInAppointmentText;
	public static String TermineLockedTimesUpdater_0;
	public static String TermineLockedTimesUpdater_4;
	public static String TermineLockedTimesUpdater_5;
	public static String TermineLockedTimesUpdater_6;
	public static String TermineLockedTimesUpdater_7;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
