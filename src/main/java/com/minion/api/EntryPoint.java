package com.minion.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;



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
public class EntryPoint {

	public static void main(String[] args){
        SpringApplication.run(EntryPoint.class, args);
   	}
}


