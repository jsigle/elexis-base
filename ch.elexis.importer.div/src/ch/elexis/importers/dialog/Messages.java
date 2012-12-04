package ch.elexis.importers.dialog;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.importers.dialog.messages"; //$NON-NLS-1$
	public static String QueryOverwriteDialog_NO;
	public static String QueryOverwriteDialog_YES;
	public static String QueryOverwriteDialog_YESTOALL;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
