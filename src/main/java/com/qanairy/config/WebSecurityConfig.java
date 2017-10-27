package com.qanairy.config;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.auth0.spring.security.api.Auth0CORSFilter;
import com.auth0.spring.security.api.Auth0SecurityConfig;
import com.qanairy.auth.Auth0Client;

@Configuration
@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class WebSecurityConfig extends Auth0SecurityConfig {

    /**
     * Provides Auth0 API access
     */
    @Bean
    public Auth0Client auth0Client() {
        return new Auth0Client(clientId, issuer);
    }
    
    /**
     *  Our API Configuration - for Profile CRUD operations
     *
     *  Here we choose not to bother using the `auth0.securedRoute` property configuration
     *  and instead ensure any unlisted endpoint in our config is secured by default
     */
    @Override
    protected void authorizeRequests(final HttpSecurity http) throws Exception {
    	http.cors().and().addFilterAfter(new SimpleCORSFilter(), Auth0CORSFilter.class).authorizeRequests()
    		.antMatchers("/realtime/**").permitAll()
    		.anyRequest().authenticated();
    }

    /*
     * Only required for sample purposes..
     */
    public String getAuthorityStrategy() {
       return super.authorityStrategy;
    }
    
    
}