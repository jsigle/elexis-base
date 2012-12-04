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