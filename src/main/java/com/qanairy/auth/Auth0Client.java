package com.qanairy.auth;

import org.springframework.stereotype.Component;

import com.auth0.client.auth.AuthAPI;

@Component
public class Auth0Client {

    private final String clientid;
    private final String domain;
    private final AuthAPI auth0;
    //private final AuthenticationAPIClient client;

    public Auth0Client(String clientid, String clientSecret, String domain) {
        this.clientid = clientid;
        this.domain = domain;
        this.auth0 = new AuthAPI(domain, clientid, clientSecret);//Auth0(clientid, domain);
        //this.client = this.auth0.newAuthenticationAPIClient();
    }

    /*public String getUsername(Auth0JWTToken token) {
        final Request<UserProfile> request = client.tokenInfo(token.getJwt());
        final UserProfile profile = request.execute();
        return profile.getEmail();
    }

    public String getUserId(Auth0JWTToken token) {
        final Request<UserProfile> request = client.tokenInfo(token.getJwt());
        final UserProfile profile = request.execute();
        return profile.getId();
    }
    */
}
