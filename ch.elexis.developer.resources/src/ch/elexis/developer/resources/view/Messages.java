/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/
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
