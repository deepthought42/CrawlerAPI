package com.qanairy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.stereotype.Component;

@Component
@SpringBootConfiguration
public class Auth0Configuration {

	@Value("${auth0.domain}")
	private String domain;
	
	@Value("${auth0.clientId}")
	private String clientId;
	
	@Value("${auth0.clientSecret}")
	private String clientSecret;
	
	@Value("${auth0.apiAudience}")
	private String apiAudience;
	
	@Value("${auth0.issuer}")
	private String issuer;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
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

	public String getApiAudience() {
		return apiAudience;
	}

	public void setApiAudience(String apiAudience) {
		this.apiAudience = apiAudience;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}
