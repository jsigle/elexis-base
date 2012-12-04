package ch.medshare.ebm;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	
	private static final String BUNDLE_NAME = "ch.medshare.ebm.messages";
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	private Messages(){}
	
	public static String getString(String context, String key){
		try {
			return RESOURCE_BUNDLE.getString(context + "." + key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
