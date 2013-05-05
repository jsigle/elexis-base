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

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

import ch.elexis.Hub;

/**
 * SimpleAuthenticator is used to do simple authentication when the SMTP server requires it.
 */
public class SMTPAuthenticator extends Authenticator {
	
	public PasswordAuthentication getPasswordAuthentication(){
		String username = Hub.localCfg.get(PreferenceConstants.MAIL_USER, Messages.Mailer_1);
		String password = Hub.localCfg.get(PreferenceConstants.MAIL_PASS, Messages.Mailer_1);
		return new PasswordAuthentication(username, password);
	}
}