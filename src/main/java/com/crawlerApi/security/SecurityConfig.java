package com.crawlerApi.security;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.crawlerApi.config.Auth0Config;
import com.crawlerApi.service.Auth0Service;
import com.looksee.models.Account;

@EnableWebSecurity
@PropertySource("classpath:auth0.properties")
@Component
public class SecurityConfig {
	
	private final Auth0Config auth0Config;
	private final Auth0Service auth0Service;
	private final Environment environment;
	
	@Autowired
	public SecurityConfig(Auth0Config auth0Config, Auth0Service auth0Service, Environment environment) {
		this.auth0Config = auth0Config;
		this.auth0Service = auth0Service;
		this.environment = environment;
	}
	
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/actuator/info")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auditor/start-individual")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/audits")).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .cors();
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(auth0Config.getAuth0Issuer());
        
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(auth0Config.getAuth0Issuer());
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, withTimestamp);
        
        jwtDecoder.setJwtValidator(withAudience);
        
        return jwtDecoder;
    }
    
    /**
     * Get the current user's account
     * @param principal The authenticated principal
     * @return Optional containing the Account if found
     */
    public Optional<Account> getCurrentUserAccount(Principal principal) {
        return auth0Service.getCurrentUserAccount(principal);
    }
    
    /**
     * Get user information from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing user information
     */
    public Optional<String> getUsername(String accessToken) {
        return auth0Service.getUsername(accessToken);
    }
    
    /**
     * Get user nickname from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing the nickname
     */
    public Optional<String> getNickname(String accessToken) {
        return auth0Service.getNickname(accessToken);
    }
    
    /**
     * Get user email from Auth0
     * @param accessToken The Auth0 access token
     * @return Optional containing the email
     */
    public Optional<String> getEmail(String accessToken) {
        return auth0Service.getEmail(accessToken);
    }
    
    /**
     * Extract user ID from Auth0 principal name
     * @param principalName The principal name
     * @return The user ID without the "auth0|" prefix
     */
    public String extractUserId(String principalName) {
        return auth0Service.extractUserId(principalName);
    }
    
    /**
     * Check if Auth0 is properly configured
     * @return true if Auth0 is configured and initialized
     */
    public boolean isAuth0Configured() {
        return auth0Service.isConfigured();
    }
	
    @Bean
	CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Check if we're in production
        boolean isProduction = isProductionProfile();
        
        if (isProduction) {
            // Strict CORS for production - only allow specific origins
            // TODO: Replace with your actual production frontend URLs
            configuration.addAllowedOrigin("https://your-production-domain.com");
            configuration.addAllowedOrigin("https://www.your-production-domain.com");
            configuration.setAllowCredentials(true);
        } else {
            // Permissive CORS for development
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(false);
        }
        
        // Common settings for both environments
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Check if the application is running in production profile
     * @return true if production profile is active
     */
    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production")) {
                return true;
            }
        }
        return false;
    }
}