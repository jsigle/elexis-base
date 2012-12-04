package ch.elexis.developer.resources.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.developer.resources.view.messages"; //$NON-NLS-1$
	public static String SampleView_deleteItem;
	public static String SampleView_newSampleDataType;
	public static String SampleView_OnlyCreateObjectsIfPatIsSelected;
	public static String SampleView_PleaseSelectPatient;
	public static String SampleView_SDTCreated;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
