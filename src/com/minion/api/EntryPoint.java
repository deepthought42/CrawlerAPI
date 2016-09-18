package com.minion.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import akka.actor.ActorSystem;

/**
 * Initializes the system and launches it. 
 * 
 * @author Brandon Kindred
 *
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.auth0.spring.security.api"})
@PropertySources({
	@PropertySource("classpath:application.properties"),
	@PropertySource("classpath:auth0.properties")
})
//@PropertySource("file://media/brandon/My Passport/Linux/development/java/WebTestVisualizer/src/main/resources/application.properties")
public class EntryPoint {
	
	public static void main(String[] args){
      /*  ApplicationContext ctx = SpringApplication.run(EntryPoint.class, args);
        System.out.println("Let's inspect the beans provided by Spring Boot:");

        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }		
        */
        SpringApplication.run(EntryPoint.class, args);
        
        final ActorSystem system = ActorSystem.create("Minion");
	}
	
	
	
}


