package com.qanairy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static com.qanairy.models.SpringExtension.SpringExtProvider;

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
	    ActorSystem system = ActorSystem.create("QanairyActorSystem");
	    // initialize the application context in the Akka Spring Extension
	    SpringExtProvider.get(system).initialize(applicationContext);
	    return system;
	  }
}