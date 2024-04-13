package com.looksee.security;

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
        http.authorizeRequests()
                .mvcMatchers("/actuator/info").permitAll()
                .mvcMatchers( "/actuator/health").permitAll()
                .mvcMatchers( "/auditor/start-individual").permitAll()
                .mvcMatchers("/audits").authenticated()
                .and().cors()
                .and().oauth2ResourceServer().jwt();
        return http.build();
    }
    /*
	@Value(value = "${auth0.domain}")
	private String domain;
    
    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Bean
    public UserDetailsService userDetailsServiceBean() throws Exception {
        return super.userDetailsServiceBean();
    }
    
    @Override
    protected void configure(final HttpSecurity http) throws Exception {
    	http.csrf().disable().authorizeRequests()
	        .mvcMatchers(HttpMethod.GET, "/actuator/info").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/actuator/health").permitAll()
	        .mvcMatchers(HttpMethod.POST, "/auditor/start-individual").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/audits/pages").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/audits/elements").permitAll()
	        .mvcMatchers(HttpMethod.POST, "/auditrecords/{audit_record_id:[0-9]+}/report").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/auditrecords/{audit_record_id:[0-9]+}/pages").permitAll()
	        .mvcMatchers(HttpMethod.POST, "/accounts").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/auditrecords/{audit_record_id:[0-9]+}/stats").permitAll()
	        .mvcMatchers(HttpMethod.GET, "/auditrecords/{audit_record_id:[0-9]+}/elements").permitAll()
	        .mvcMatchers(HttpMethod.POST, "/auditrecords/{audit_record_id:[0-9]+}/persona/education").permitAll()
	        .mvcMatchers(HttpMethod.POST, "/subscribe/stripe_webhook").permitAll()
	        .mvcMatchers("/api/private-scoped").hasAuthority("SCOPE_read:messages") //this line is left in as future example
	        .anyRequest()
	        .authenticated()
	        .and().cors()
	        .and().oauth2ResourceServer()
	        .jwt()
            .decoder(jwtDecoder());
    }
    
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
        JwtDecoders.fromOidcIssuerLocation(issuer);
        
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
    
    */
    @Bean
	CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    	source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        
    	return source;
    }
}