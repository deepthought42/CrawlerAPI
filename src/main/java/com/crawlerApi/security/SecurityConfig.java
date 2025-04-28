package com.crawlerApi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
public class SecurityConfig {
	
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests()
                .mvcMatchers("/actuator/info").permitAll()
                .mvcMatchers( "/actuator/health").permitAll()
                .mvcMatchers( "/auditor/start-individual").permitAll()
                .mvcMatchers("/audits").authenticated()
                .and().cors()
                .and().oauth2ResourceServer().jwt();
        return http.build();
    }
	@Value(value = "${auth0.domain}")
	private String domain;
    
    @Value("${auth0.audience}")
    private String audience;
    
    @Bean
	CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    	source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        
    	return source;
    }
}