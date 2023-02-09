package com.looksee.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 
 */
@Configuration
@ComponentScan({"com.looksee.models"})
public class ConfigService {

	@Bean
	public JavaMailSender getJavaMailSender() {
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    
	    mailSender.setHost("email-smtp.us-east-1.amazonaws.com");
	    mailSender.setPort(587);
	    mailSender.setUsername("AKIAIFMYVAQFASYCGXPA");
	    mailSender.setPassword("BBdxu5oKv9GRFRKIytwrb9NmeYPGtPlJCrES6vEb2TFX");
	     
	    Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", "smtp");
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	     
	    return mailSender;
	}
}