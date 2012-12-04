package ch.elexis.agenda.data;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.agenda.data.messages"; //$NON-NLS-1$
	public static String Import_Agenda_cancelled;
	public static String Import_Agenda_creatingTables;
	public static String Import_Agenda_errorsDuringImport;
	public static String Import_Agenda_importFromJavaAgenda;
	public static String Import_Agenda_importingAgenda;
	public static String Import_Agenda_importingApps;
	public static String Import_Agenda_importWasCancelled;
	public static String TagesNachricht_29;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
