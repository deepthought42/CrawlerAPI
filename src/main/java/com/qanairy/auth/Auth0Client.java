package com.qanairy.auth;

import org.springframework.stereotype.Component;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;

@Component
public class Auth0Client {

    private AuthAPI auth0;
    //private final AuthenticationAPIClient client;

    public Auth0Client() {
        this.auth0 = new AuthAPI("qanairy.auth0.com", "wT7Phjs9BpwEfnZeFLvK1hwHWP2kU7LV", "8hk4R5YJ4gO5xPZdjjMdy7YtUF8eA22F");//Auth0(clientid, domain);
        //this.client = this.auth0.newAuthenticationAPIClient();
    }

	/**
	 * @return the auth0
	 */
	public AuthAPI getApi() {
		return auth0;
	}

    /*public String getUsername(Auth0JWTToken token) {
        final Request<UserInfo> request = this.auth0.tokenInfo(token.getJwt());
        final UserInfo profile = request.execute();
        return profile.getValues().get("email");
    }

    public String getUserId(Auth0JWTToken token) {
        final Request<UserProfile> request = client.tokenInfo(token.getJwt());
        final UserProfile profile = request.execute();
        return profile.getId();
    }
    */
}
