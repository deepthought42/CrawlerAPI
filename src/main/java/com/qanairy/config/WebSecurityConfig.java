package com.qanairy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
import com.qanairy.auth.Auth0Client;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter  {

	@Value(value = "${auth0.apiAudience}")
    private String audience;
    @Value(value = "${auth0.issuer}")
    private String issuer;
    @Value(value = "${auth0.secret}")
    private String secret;
    @Value(value = "${auth0.clientId}")
    private String clientId;
    
    /**
     * Provides Auth0 API access
     */
    @Bean
    public Auth0Client auth0Client() {
        return new Auth0Client(clientId, secret, issuer);
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
    	 .forHS256(audience, issuer, secret.getBytes())
         .configure(http).cors().and().authorizeRequests().anyRequest().permitAll();
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