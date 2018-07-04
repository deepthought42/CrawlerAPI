package com.qanairy.config;

import org.springframework.beans.factory.annotation.Autowired;
import static com.qanairy.config.SpringExtension.SPRING_EXTENSION_PROVIDER;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import akka.actor.ActorSystem;

/**
 * 
 */
@Configuration
@ComponentScan
public class ConfigService {
     
	@Autowired
    private ApplicationContext applicationContext;
 
	@Autowired
    private AkkaConfig akkaConfig;
	
    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("MinionActorSystem");
        SPRING_EXTENSION_PROVIDER.get(system)
          .initialize(applicationContext);
        return system;
    }
}