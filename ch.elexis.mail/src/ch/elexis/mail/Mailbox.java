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

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import ch.elexis.Hub;
import ch.rgw.tools.ExHandler;

public class Mailbox {
	Session session = null;
	Store store = null;
	Folder inbox = null;
	Message[] messages;
	
	public Mailbox(){
		Properties props = new Properties();
		session = Session.getDefaultInstance(props);
	}
	
	public boolean open(){
		try {
			store = session.getStore(Hub.localCfg.get(PreferenceConstants.MAIL_MODE, "pop")); //$NON-NLS-1$
			inbox = store.getDefaultFolder();
			messages = inbox.getMessages();
			return true;
		} catch (Exception e) {
			ExHandler.handle(e);
		}
		
		return false;
	}
	
	public int getMessageCount(){
		return messages.length;
	}
	
	public String getHeaders(){
		for (Message m : messages) {
			
		}
		return ""; //$NON-NLS-1$
	}
	
	public void close(){
		try {
			if (inbox != null) {
				inbox.close(true);
			}
			if (store != null) {
				store.close();
			}
			
		} catch (MessagingException e) {
			ExHandler.handle(e);
		}
	}
}
