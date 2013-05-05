/*******************************************************************************
 * Copyright (c) 2010, St. Schenk and Medshare GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    St. Schenk - initial implementation
 * 
 *******************************************************************************/

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
