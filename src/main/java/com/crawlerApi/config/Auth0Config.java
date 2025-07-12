package com.crawlerApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "auth0")
@Getter
@Setter
public class Auth0Config {
    
    private String domain;
    private String issuer;
    private String apiAudience;
    private String clientId;
    private String clientSecret;
    private String audience;
    private String securedRoute;
    private boolean base64EncodedSecret;
    private String authorityStrategy;
    private boolean defaultAuth0ApiSecurityEnabled;
    private String signingAlgorithm;
    
    // Getters and Setters
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getApiAudience() {
        return apiAudience;
    }
    
    public void setApiAudience(String apiAudience) {
        this.apiAudience = apiAudience;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getAudience() {
        return audience;
    }
    
    public void setAudience(String audience) {
        this.audience = audience;
    }
    
    public String getSecuredRoute() {
        return securedRoute;
    }
    
    public void setSecuredRoute(String securedRoute) {
        this.securedRoute = securedRoute;
    }
    
    public boolean isBase64EncodedSecret() {
        return base64EncodedSecret;
    }
    
    public void setBase64EncodedSecret(boolean base64EncodedSecret) {
        this.base64EncodedSecret = base64EncodedSecret;
    }
    
    public String getAuthorityStrategy() {
        return authorityStrategy;
    }
    
    public void setAuthorityStrategy(String authorityStrategy) {
        this.authorityStrategy = authorityStrategy;
    }
    
    public boolean isDefaultAuth0ApiSecurityEnabled() {
        return defaultAuth0ApiSecurityEnabled;
    }
    
    public void setDefaultAuth0ApiSecurityEnabled(boolean defaultAuth0ApiSecurityEnabled) {
        this.defaultAuth0ApiSecurityEnabled = defaultAuth0ApiSecurityEnabled;
    }
    
    public String getSigningAlgorithm() {
        return signingAlgorithm;
    }
    
    public void setSigningAlgorithm(String signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
    }
} 