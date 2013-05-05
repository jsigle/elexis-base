/*******************************************************************************
 * Copyright (c) 2006-2011, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *******************************************************************************/

package ch.elexis.privatnotizen;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.privatnotizen.messages"; //$NON-NLS-1$
	public static String KonsExtension_noteActionLabel;
	public static String KonsExtension_noteActionXREFText;
	public static String NotizInputDialog_noteDlgMessage;
	public static String NotizInputDialog_noteDlgText;
	public static String NotizInputDialog_noteDlgTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
