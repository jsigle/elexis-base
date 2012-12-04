package ch.elexis.omnivore.views;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.omnivore.views.messages"; //$NON-NLS-1$
	public static String FileImportDialog_importFileCaption;
	public static String FileImportDialog_importFileText;
	public static String FileImportDialog_keywordsLabel;
	public static String FileImportDialog_titleLabel;
	public static String OmnivoreView_dateColumn;
	public static String OmnivoreView_deleteActionCaption;
	public static String OmnivoreView_deleteActionToolTip;
	public static String OmnivoreView_editActionCaption;
	public static String OmnivoreView_editActionTooltip;
	public static String OmnivoreView_importActionCaption;
	public static String OmnivoreView_importActionToolTip;
	public static String OmnivoreView_keywordsColumn;
	public static String OmnivoreView_reallyDeleteCaption;
	public static String OmnivoreView_reallyDeleteContents;
	public static String OmnivoreView_titleColumn;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
