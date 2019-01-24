package com.qanairy.services;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.stereotype.Service;

/**
 * Sends emails
 */
@Service
public class EmailService {

	String username = "qanairy@qanairy.com";
	String password = "TweetyPie";
	
	/**
	 * Creates a new email connection session
	 * 
	 * @return new {@link Session}
	 */
	public Session getNewSession(){
		Properties prop = new Properties();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", "smtp.gmail.com");
		prop.put("mail.smtp.port", "587");
		prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		
		Session session = Session.getInstance(prop, new Authenticator() {
		    @Override
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication(username, password);
		    }
		});
		
		return session;
	}
	
	public void sendDiscoveryCompleteEmail(String to_email, String domain_url) throws AddressException, MessagingException{
		Session session = getNewSession();
		
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("qanairy@qanairy.com"));
		message.setRecipients(
		  Message.RecipientType.TO, InternetAddress.parse(to_email));
		message.setSubject("Qanairy discovery has finished");
		 
		String msg = "You're discovery on domain "+domain_url +" is complete.";
		 
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(msg, "text/html");
		 
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mimeBodyPart);
		 
		message.setContent(multipart);
		 
		Transport.send(message);
	}
}
