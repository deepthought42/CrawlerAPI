package com.minion.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.context.request.RequestContextListener;

import akka.actor.ActorSystem;


/**
 * Initializes the system and launches it. 
 * 
 * @author Brandon Kindred
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.minion","com.qanairy"})
@EnableAutoConfiguration
@PropertySources({
	@PropertySource("classpath:application.properties"),
	@PropertySource("classpath:auth0.properties")
})
public class EntryPoint {
	
	public static void main(String[] args){
        SpringApplication.run(EntryPoint.class, args);
        
        final ActorSystem system = ActorSystem.create("Minion");
   	}
	

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}


