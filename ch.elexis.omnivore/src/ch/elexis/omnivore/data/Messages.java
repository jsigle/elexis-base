package ch.elexis.omnivore.data;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.omnivore.data.messages"; //$NON-NLS-1$
	public static String DocHandle_cantReadCaption;
	public static String DocHandle_cantReadMessage;
	public static String DocHandle_dataNotWritten;
	public static String DocHandle_docErrorCaption;
	public static String DocHandle_docErrorMessage;
	public static String DocHandle_execError;
	public static String DocHandle_importErrorCaption;
	public static String DocHandle_importErrorMessage;
	public static String DocHandle_importErrorMessage2;
	public static String DocHandle_noPatientSelected;
	public static String DocHandle_pleaseSelectPatien;
	public static String DocHandle_readErrorCaption;
	public static String DocHandle_readErrorMessage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
