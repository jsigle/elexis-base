package ch.elexis.selectors;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.selectors.messages"; //$NON-NLS-1$
	public static String SelectorPanel_clearFields;
	public static String SelectorPanel_automaticSearch;
	public static String SelectorPanel_activateAutomaticSearch;
	public static String SelectorPanel_performSearch;
	public static String SelectorPanel_performSearchTooltip;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
