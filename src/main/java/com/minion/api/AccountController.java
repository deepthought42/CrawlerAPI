package com.minion.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.qanairy.api.exception.Auth0ManagementApiException;
import com.qanairy.api.exception.InvalidUserException;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

/**
 *	API endpoints for interacting with {@link User} data
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {
	private static Logger log = LoggerFactory.getLogger(AccountController.class);
    
    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected AccountService accountService;

    /*@Autowired
    protected UsernameService usernameService;
    */
    
    /**
     * Create new account
     * 
     * @param authorization_header
     * @param account
     * @param principal
     * 
     * @return
     * 
     * @throws InvalidUserException
     * @throws UnirestException
     * @throws Auth0ManagementApiException 
     */
    //@PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
    @RequestMapping(method = RequestMethod.POST)
    //@PreAuthorize("hasAuthority('create:account')")
    public ResponseEntity<Account> create(@RequestParam(value="service_package", required=true) String service_package) 
    				throws InvalidUserException, UnirestException{        
       
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
        
        if("brandon.kindred@gmail.com".equals("UNKNOWN_USER")){
        	throw new InvalidUserException();
        }
       
        //create account
        Account acct = new Account("brandon.kindred@gmail.com", service_package, "tmp_payment_acct_num", new ArrayList<QanairyUser>());
    	
        //Create user
        QanairyUser user = new QanairyUser("brandon.kindred@gmail.com");
        acct.addUser(user);

        // Connect to Auth0 API and update user metadata
        //HttpResponse<String> api_resp = Auth0ManagementApi.updateUserAppMetadata(auth0Client.getUserId((Auth0JWTToken) principal), "{\"status\": \"account_owner\"}");
        /*if(api_resp.getStatus() != 200){
        	throw new Auth0ManagementApiException();
        }
        */
        
        //printGrantedAuthorities((Auth0JWTToken) principal);
        Account new_account = null;
        //final String username = usernameService.getUsername();
        // log username of user requesting account creation
        log.info("User with email: " + user.getEmail() + " creating new account");
        new_account = accountService.create(acct);
        
        
        Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        //traits.put("name", principal.getName());
        traits.put("email", "brandon.kindred@gmail.com");        
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(new_account.getKey())
    		    .traits(traits)
    		);
    	
    	Map<String, String> account_signup_properties = new HashMap<String, String>();
    	account_signup_properties.put("plan", new_account.getServicePackage());
    	analytics.enqueue(TrackMessage.builder("Signed Up")
    		    .userId(new_account.getKey())
    		    .properties(account_signup_properties)
    		);
    	
        return ResponseEntity.accepted().body(new_account);
    }

    /**
     * Retrieves {@link Account account} with a given key
     * 
     * @param key account key
     * @return {@link Account account}
     */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
    @RequestMapping(value ="/{id}", method = RequestMethod.GET)
    public Account get(final @PathVariable String key) {
        log.info("get invoked");
        return accountService.get(key);
    }

	@PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    public Account update(final @PathVariable String key, 
    					  final @Validated @RequestBody Account account) {
        log.info("update invoked");
        return accountService.update(account);
    }
}
