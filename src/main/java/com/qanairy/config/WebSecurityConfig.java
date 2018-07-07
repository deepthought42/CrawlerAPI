package com.qanairy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter  {
	
	@Value(value = "${auth0.domain}")
	private String domain;
	@Value(value = "${auth0.apiAudience}")
    private String audience;
    @Value(value = "${auth0.issuer}")
    private String issuer;
    @Value(value = "${auth0.clientId}")
    private String clientId;
    
    @Bean
    public UserDetailsService userDetailsServiceBean() throws Exception {
        return super.userDetailsServiceBean();
    }
    /**
     *  Our API Configuration - for Profile CRUD operations
     *
     *  Here we choose not to bother using the `auth0.securedRoute` property configuration
     *  and instead ensure any unlisted endpoint in our config is secured by default
     */
    @Override
    protected void configure(final HttpSecurity http) throws Exception {
    	 JwtWebSecurityConfigurer
    	 .forRS256(audience, issuer)
    	 //.forHS256(audience, issuer, secret.getBytes())
         .configure(http).cors().and().csrf().disable().authorizeRequests()
         .antMatchers(HttpMethod.GET, "/actuator/info").permitAll()
         .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
         .antMatchers(HttpMethod.POST, "/accounts").permitAll()
         .anyRequest().authenticated();
    	/*http.cors().and().addFilterAfter(new SimpleCORSFilter(), Auth0CORSFilter.class).authorizeRequests()
    		.antMatchers("/realtime/**").permitAll()
    		.anyRequest().authenticated();
    		*/
    }
    
    @Bean
	CorsConfigurationSource corsConfigurationSource() {
    	final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    	source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());

    	return source;
    }
}