package com.minion.api;

import java.security.Principal;
import java.util.ArrayList;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.spring.security.api.Auth0JWTToken;
import com.auth0.spring.security.api.Auth0UserDetails;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.minion.actors.PathExpansionActor;
import com.qanairy.api.exception.Auth0ManagementApiException;
import com.qanairy.api.exception.InvalidUserException;
import com.qanairy.auth.Auth0Client;
import com.qanairy.auth.Auth0ManagementApi;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.services.AccountService;
import com.qanairy.services.UsernameService;

/**
 *	API endpoints for interacting with {@link User} data
 */
@RestController
@CrossOrigin(origins = "http://localhost:8001")
@RequestMapping("/accounts")
public class AccountController {
	private static Logger log = LoggerFactory.getLogger(AccountController.class);

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    private Auth0Client auth0Client;
    
    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected UsernameService usernameService;
    
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
    public ResponseEntity<Account> create(@RequestBody Account account,
    										final Principal principal) 
    				throws InvalidUserException, UnirestException, Auth0ManagementApiException{        
       
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
        log.info("Current user accessed Admin secured resource: " + currentUser.getUsername());
        
        if(currentUser.getUsername().equals("UNKNOWN_USER")){
        	throw new InvalidUserException();
        }
        
        //create account
        Account acct = new Account(currentUser.getUsername(), account.getServicePackage(), account.getPaymentAcctNum(), new ArrayList<QanairyUser>());
        
        //Create user
        QanairyUser user = new QanairyUser(currentUser.getUsername());
        acct.addUser(user);

        // Connect to Auth0 API and update user metadata
        HttpResponse<String> api_resp = Auth0ManagementApi.updateUserAppMetadata(auth0Client.getUserId((Auth0JWTToken) principal), "{\"status\": \"account_owner\"}");
        if(api_resp.getStatus() != 200){
        	throw new Auth0ManagementApiException();
        }
        
        printGrantedAuthorities((Auth0JWTToken) principal);
        Account new_account = null;
        if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            final String username = usernameService.getUsername();
            // log username of user requesting account creation
            log.info("User with email: " + username + " creating new account");
            new_account = accountService.create(acct);
        }

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
        logger.info("get invoked");
        return accountService.get(key);
    }

    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    public Account update(final @PathVariable String key, final @Validated @RequestBody Account account) {
        logger.info("update invoked");
        return accountService.update(account);
    }
    
    

    /**
     * Simple demonstration of how Principal info can be accessed
     * 
     * @param principal
     */
    private void printGrantedAuthorities(final Auth0JWTToken principal) {
        for(final GrantedAuthority grantedAuthority: principal.getAuthorities()) {
            final String authority = grantedAuthority.getAuthority();
            logger.info(authority);
        }
    }
}
