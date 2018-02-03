package com.minion.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import akka.actor.ActorSystem;



/**
 * Initializes the system and launches it. 
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.minion","com.qanairy"})
@PropertySources({
	@PropertySource("classpath:application.properties"),
	@PropertySource("classpath:auth0.properties")
})
public class EntryPoint {

	public static void main(String[] args){
        SpringApplication.run(EntryPoint.class, args);

        final ActorSystem system = ActorSystem.create("Qanairy-api");
   	}
	
	@Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
/*
    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/").allowedOrigins("http://alpha.qanairy.com");
            }
        };
    }
    */
}


