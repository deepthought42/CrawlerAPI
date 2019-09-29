package com.qanairy.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;

@Component
public class Auth0Client {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

    private AuthAPI auth0;
    //private final AuthenticationAPIClient client;

    public Auth0Client() {
        //this.auth0 = new AuthAPI("dev-qanairy.auth0.com", "IGBRMudqLag0UKqwcEmYPAdTxQsoAMUv", "5R6Uy4EhKWuavOC9DDdoENMxSlJgJ6TIOBPYvES3ajhlWqaTEKfm5lZ4z9YgIT-F");//Auth0(clientid, domain);
        //this.auth0 = new AuthAPI("staging-qanairy.auth0.com", "jEhM2ZhIWwy49YiJsNE9g9YtLEEn3Vxw", "XUtP8AOw4cRKTpJJT_BWjUgfFnrEq4TRjA8oSkVeRBTbIPbeNIrXVoydc5EefRqm");//Auth0(clientid, domain);
    	this.auth0 = new AuthAPI("qanairy.auth0.com", "wWn9rubrIFRQZI7buiYVsadVQi6ewtQH", "EFXS4rxXk6a036e7DOLKXkh4gYs9nSdL93wzWcRvUUGAuL4Bxh9OmMDL-ZQ-VbnR");
    	//this.client = this.auth0.newAuthenticationAPIClient();
    }

	/**
	 * @return the auth0
	 */
	public AuthAPI getApi() {
		return auth0;
	}

	public String getUsername(String auth_access_token) {
		Request<UserInfo> user_info_request = auth0.userInfo(auth_access_token);
    	String username = null;
    	try {
    	    UserInfo info = user_info_request.execute();
    	    username = info.getValues().get("name").toString();
    	} catch (APIException exception) {
    	    // api error
    		log.error(exception.getError() + " \n "+
    						exception.getMessage());
    		exception.printStackTrace();

    	} catch (Auth0Exception exception) {
    	    // request error
    		exception.printStackTrace();
    		log.error(exception.getMessage());
    	}
    	
    	return username;
	}

	public String getNickname(String auth_access_token) {
		Request<UserInfo> user_info_request = auth0.userInfo(auth_access_token);
    	String nickname = null;
    	try {
    	    UserInfo info = user_info_request.execute();
    	    nickname = info.getValues().get("nickname").toString();
    	} catch (APIException exception) {
    	    // api error
    		log.error(exception.getError() + " \n "+
    						exception.getMessage());
    		exception.printStackTrace();

    	} catch (Auth0Exception exception) {
    	    // request error
    		exception.printStackTrace();
    		log.error(exception.getMessage());
    	}
    	
    	return nickname;	
	}

	public String getUserId(String auth_access_token) {
		Request<UserInfo> user_info_request = auth0.userInfo(auth_access_token);
    	String user_id = null;
    	try {
    	    UserInfo info = user_info_request.execute();
    	    user_id = info.getValues().get("sub").toString();
    	} catch (APIException exception) {
    	    // api error
    		log.error(exception.getError() + " \n "+
    						exception.getMessage());
    		exception.printStackTrace();

    	} catch (Auth0Exception exception) {
    	    // request error
    		exception.printStackTrace();
    		log.error(exception.getMessage());
    	}

    	return user_id;
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
