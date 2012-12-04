package ch.elexis.agenda.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.agenda.preferences.messages"; //$NON-NLS-1$
	public static String AgendaDruck_printDirectly;
	public static String AgendaDruck_printerForCards;
	public static String AgendaDruck_settingsForPrint;
	public static String AgendaDruck_templateForCards;
	public static String AgendaDruck_TrayForCards;
	public static String PreferenceConstants_appointmentCard;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
