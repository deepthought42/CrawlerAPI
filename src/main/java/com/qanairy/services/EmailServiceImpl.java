package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
	private static Logger log = LoggerFactory.getLogger(EmailServiceImpl.class.getName());

    @Autowired
    public JavaMailSender emailSender;
 
    public EmailServiceImpl(JavaMailSender emailSender){
    	this.emailSender = emailSender;
    }
    
    public void sendSimpleMessage(
      String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom("qanairy@qanairy.com");
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(text);
        emailSender.send(message);
    }
    
    public void sendHtmlMessage(
    	      String recipient, String subject, String message) {
    	
    	 MimeMessagePreparator messagePreparator = mimeMessage -> {
    	        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
    	        messageHelper.setFrom("qanairy@qanairy.com");
    	        messageHelper.setTo(recipient);
    	        messageHelper.setSubject(subject);
    	        messageHelper.setText(message);
    	    };
    	    try {
    	    	emailSender.send(messagePreparator);
    	    } catch (MailException e) {
    	    	log.error(e.getMessage());
    	    }
    }
}
