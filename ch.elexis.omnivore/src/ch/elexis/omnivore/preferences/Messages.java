package ch.elexis.omnivore.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.omnivore.preferences.messages"; //$NON-NLS-1$
 	public static String Omnivore_jsErrNoActivator;
 	public static String Omnivore_jsPREF_omnivore_js;
 	public static String Omnivore_jsPREF_MAX_FILENAME_LENGTH;
 	public static String Omnivore_jsPREF_automatic_archiving_of_processed_files;
 	public static String Omnivore_jsPREF_Rule;
 	public static String Omnivore_jsPREF_SRC_PATTERN;
	public static String Omnivore_jsPREF_DEST_DIR;
 	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
