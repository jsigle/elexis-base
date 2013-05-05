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
