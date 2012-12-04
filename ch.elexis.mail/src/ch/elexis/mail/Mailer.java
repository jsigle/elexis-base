/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    R. Zweifel - SMTP-Authentifizierung
 *    
 *  $Id: Mailer.java 5953 2010-01-22 20:51:41Z rgw_ch $
 *******************************************************************************/

package ch.elexis.mail;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import ch.elexis.Hub;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Result;

public class Mailer {
	static Properties props;
	
	static {
		props = new Properties();
		props.put(
			"mail.smtp.host", Hub.localCfg.get(PreferenceConstants.MAIL_SMTP, Messages.Mailer_1)); //$NON-NLS-1$
		props.put("mail.smtp.auth", Hub.localCfg.get( //$NON-NLS-1$
			PreferenceConstants.MAIL_AUTH, Messages.Mailer_1));
		props.put("mail.smtp.port", Hub.localCfg.get( //$NON-NLS-1$
			PreferenceConstants.MAIL_SMTPPORT, Messages.Mailer_1));
	}
	
	/**
	 * Convenience-Methode um einfach schnell eine simple Text-Mail zu versenden
	 * 
	 * @param recipient
	 *            empfänger-Email
	 * @param subject
	 *            Betreff
	 * @param message
	 *            Nachrichtentext
	 * @param from
	 *            Absender
	 * @return Result.OK oder eine Feghlermeldung
	 */
	public static Result<String> postMail(String recipient, String subject, String message,
		String from){
		try {
			Session session = null;
			Authenticator auth = new SMTPAuthenticator();
			if (props.getProperty("mail.smtp.auth").equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$
				session = Session.getDefaultInstance(props, auth);
			} else {
				session = Session.getDefaultInstance(props);
			}
			Message msg = new MimeMessage(session);
			InternetAddress addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);
			InternetAddress addressTo = new InternetAddress(recipient);
			msg.setRecipient(Message.RecipientType.TO, addressTo);
			msg.setSubject(subject);
			msg.setContent(message, "text/plain"); //$NON-NLS-1$		    
			Transport.send(msg);
			return new Result<String>("Ok"); //$NON-NLS-1$
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return new Result<String>(Result.SEVERITY.ERROR, 1, ex.getMessage(),
				Messages.Mailer_Error, true);
		}
	}
	
	/**
	 * Eine vorerst leere Mime/Multipart Nachricht erstellen. Danach kann beliebig oft unt gemischt
	 * addTextPart(), addBinaryPart() und addFilePart() aufgerufen werden, und am Schluss send()
	 * 
	 * @param subject
	 *            Titel
	 * @param from
	 *            Absender
	 * @return die Message
	 */
	public Message createMultipartMessage(String subject, String from){
		try {
			Session session = null;
			Authenticator auth = new SMTPAuthenticator();
			if (props.getProperty("mail.smtp.auth").equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$
				session = Session.getDefaultInstance(props, auth);
			} else {
				session = Session.getDefaultInstance(props);
			}
			Message msg = new MimeMessage(session);
			InternetAddress addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);
			msg.setSubject(subject);
			msg.setContent(new MimeMultipart());
			return msg;
		} catch (MessagingException ex) {
			ExHandler.handle(ex);
			return null;
		}
	}
	
	/**
	 * Einen TextPart an eine vorher mit createMultipartMessage erstellte Nachricht anhängen
	 * 
	 * @param msg
	 *            die Nachricht
	 * @param text
	 *            der Text für den Part
	 * @return true bei Erfolg
	 */
	public boolean addTextPart(Message msg, String text){
		try {
			MimeMultipart content = (MimeMultipart) msg.getContent();
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setContent(text, "text/plain"); //$NON-NLS-1$
			content.addBodyPart(textPart);
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return false;
	}
	
	public boolean addTextPart(Message msg, String text, String name){
		try {
			MimeMultipart content = (MimeMultipart) msg.getContent();
			MimeBodyPart textPart = new MimeBodyPart();
			textPart.setContent(text, "text/plain"); //$NON-NLS-1$
			textPart.setFileName(name);
			content.addBodyPart(textPart);
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return false;
	}
	
	public boolean addFilePart(Message msg, File file){
		if (!file.canRead()) {
			return false;
		}
		try {
			MimeMultipart content = (MimeMultipart) msg.getContent();
			MimeBodyPart filePart = new MimeBodyPart();
			filePart.attachFile(file);
			filePart.setFileName(file.getName());
			content.addBodyPart(filePart);
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		
		return false;
	}
	
	/**
	 * Eine binäre Part an eine vorher mit createMultipatrtMessage erstellte Nachricht anhängen.
	 * 
	 * @param msg
	 *            die Nachricht
	 * @param name
	 *            Name für die Part
	 * @param part
	 *            die Daten, die angehängt werden sollen
	 * @return true bei Erfolg
	 */
	public boolean addBinaryPart(Message msg, String name, byte[] part){
		try {
			DataSource ds = new ByteArrayDataSource(part, "application/octet-stream"); //$NON-NLS-1$
			MimeMultipart content = (MimeMultipart) msg.getContent();
			MimeBodyPart binaryPart = new MimeBodyPart();
			binaryPart.setDataHandler(new DataHandler(ds));
			binaryPart.setFileName(name);
			content.addBodyPart(binaryPart);
			return true;
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		return false;
	}
	
	/**
	 * Die vorher mit createMultipartMessage etc. erstellte Nachricht versenden
	 * 
	 * @param msg
	 *            die Nachricht
	 * @param to
	 *            Empfänger-Email
	 * @return Result.OK oder eine Fehlermeldung
	 */
	public Result<String> send(Message msg, String to){
		try {
			InternetAddress addressTo = new InternetAddress(to);
			msg.setRecipient(Message.RecipientType.TO, addressTo);
			Transport.send(msg);
			return new Result<String>("Ok"); //$NON-NLS-1$
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return new Result<String>(Result.SEVERITY.ERROR, 1, ex.getClass().getName()
				+ " " + ex.getMessage(), //$NON-NLS-1$
				Messages.Mailer_Error, true);
			
		}
	}
}
