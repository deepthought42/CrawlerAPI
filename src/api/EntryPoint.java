package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;

import static com.stormpath.spring.config.StormpathWebSecurityConfigurer.stormpath;

import akka.actor.ActorSystem;
import config.CORSFilter;

/**
 * Initializes the system and launches it. 
 * 
 * @author Brandon Kindred
 *
 */
@EnableAutoConfiguration
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
	
	@Configuration
	protected static class SecurityConfiguration extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.addFilterBefore(new CORSFilter(), ChannelProcessingFilter.class)
				.apply(stormpath())
				.and()
				.authorizeRequests();

	    }
	}
}
