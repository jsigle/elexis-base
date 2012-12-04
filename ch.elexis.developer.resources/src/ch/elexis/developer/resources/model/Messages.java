package ch.elexis.developer.resources.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.developer.resources.model.messages"; //$NON-NLS-1$
	public static String ACLContributor_CreateSampleDataTypes;
	public static String ACLContributor_DeleteSampleDataTypes;
	public static String ACLContributor_ModifySampleDataTypes;
	public static String ACLContributor_ReadSampleDataTypes;
	public static String ACLContributor_SampleDataTypeAccess;
	public static String SampleDataType_hasBoreFactor;
	public static String SampleDataType_hasFunFactor;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
