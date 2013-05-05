/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.mail;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "ch.elexis.mail.messages"; //$NON-NLS-1$
	
	public static String Activator_0;
	
	public static String Mailer_1;
	
	public static String Mailer_Error;
	
	public static String Preferences_EclipseLogfile;
	
	public static String Preferences_ElexisLogfile;
	
	public static String Preferences_ErrorMailAdress;
	
	public static String Preferences_MailSendSettings;
	
	public static String Preferences_mailServer;
	
	public static String Preferences_method;
	
	public static String Preferences_SenderEMail;
	
	public static String Preferences_SendErrorMsg;
	
	public static String Preferences_SMTPServer;
	
	public static String Preferences_SMTPServerAuth;
	
	public static String Preferences_SMTPServerPort;
	
	public static String Preferences_SenderEMailUser;
	
	public static String Preferences_SenderEMailPass;
	
	public static String QFADialog_ErrorMessage;
	
	public static String QFADialog_ExplanationErrorMail;
	
	public static String QFADialog_MessageText;
	
	public static String QFADialog_MessageTitle;
	
	public static String QFADialog_SendErrorMail;
	
	public static String SendQFA_couldtZip;
	
	public static String SendQFA_ErrorWhileSending;
	
	public static String SendQFA_MessageSent;
	
	public static String SendQFA_YourMessageIsSent;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages(){}
}
