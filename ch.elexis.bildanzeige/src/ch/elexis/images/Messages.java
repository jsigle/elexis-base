/*******************************************************************************
 * Copyright (c) 2006-2010, Gerry Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gerry Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.images;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.images.messages"; //$NON-NLS-1$
	public static String Bild_BadVersionNUmber;
	public static String Bild_NoPatientSelected;
	public static String Bild_VersionConflict;
	public static String Bild_YouShouldSelectAPatient;
	public static String BildanzeigeFenster_Cannot;
	public static String BildanzeigeFenster_Close;
	public static String BildanzeigeFenster_Create;
	public static String BildanzeigeFenster_Error;
	public static String BildanzeigeFenster_ErrorWriting;
	public static String BildanzeigeFenster_Export;
	public static String BildImportDialog_DescriptionOfImage;
	public static String BildImportDialog_ImportCaption;
	public static String BildImportDialog_ImportMessage;
	public static String BildImportDialog_ImportTitle;
	public static String BildImportDialog_JPEG_Description;
	public static String BildImportDialog_PNG_Description;
	public static String BildImportDialog_StorageFormat;
	public static String BildImportDialog_TitleOfImage;
	public static String KonsExtension_ErrorLoading;
	public static String KonsExtension_Image;
	public static String KonsExtension_ImageCouldnotBeLoaded;
	public static String KonsExtension_InsertImage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
