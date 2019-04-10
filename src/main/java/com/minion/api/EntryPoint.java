package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import akka.actor.ActorSystem;

/**
 * Initializes the system and launches it. 
 *
 */

@SpringBootApplication(exclude={Neo4jDataAutoConfiguration.class})
@ComponentScan(basePackages = {"com.minion","com.qanairy"})
@PropertySources({
	@PropertySource("classpath:application.properties"),
	@PropertySource("classpath:auth0.properties")
})
@EnableAutoConfiguration(exclude = { FreeMarkerAutoConfiguration.class })
public class EntryPoint {

	@Autowired
	private static ActorSystem actor_system;
	
	public static void main(String[] args){
        SpringApplication.run(EntryPoint.class, args);
        
        //actor_system.actorOf(SpringExtProvider.get(actor_system).props("memoryRegistryActor"), "memoryRegistryActor");
   	}
}


