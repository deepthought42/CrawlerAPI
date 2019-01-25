package com.qanairy.config;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import akka.actor.ActorSystem;

/**
 * 
 */
@Configuration
@ComponentScan({"com.qanairy.models"})
public class ConfigService {
     
	@Autowired
    private ApplicationContext applicationContext;
	
	/**
	 * Actor system singleton for this application.
	 */
	@Bean
	public ActorSystem actorSystem() {
	  ActorSystem system = ActorSystem.create("QanairyActorSystem1");
	  // initialize the application context in the Akka Spring Extension
	  SpringExtProvider.get(system).initialize(applicationContext);
	  return system;
	}
	

	@Bean
	public JavaMailSender getJavaMailSender() {
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    mailSender.setHost("smtp.gmail.com");
	    mailSender.setPort(587);
	     
	    mailSender.setUsername("my.gmail@gmail.com");
	    mailSender.setPassword("password");
	     
	    Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", "smtp");
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.debug", "true");
	     
	    return mailSender;
	}
}