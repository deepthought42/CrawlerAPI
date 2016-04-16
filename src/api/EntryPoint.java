package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import akka.actor.ActorSystem;

/**
 * Initializes the system and launches it. 
 * 
 * @author Brandon Kindred
 *
 */
@ComponentScan
@SpringBootApplication
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
