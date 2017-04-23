package com.minion.api;

import java.security.Principal;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.spring.security.api.Auth0JWTToken;
import com.auth0.spring.security.api.Auth0UserDetails;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.qanairy.api.exception.InvalidUserException;
import com.qanairy.auth.Auth0Client;
import com.qanairy.auth.Auth0ManagementApi;
import com.qanairy.auth.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.QanairyUser;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.qanairy.services.UsernameService;

/**
 *	API endpoints for interacting with {@link User} data
 */
@RestController
@CrossOrigin(origins = "http://localhost:8001")
@RequestMapping("/accounts")
public class AccountController {
	
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
     * Simple demonstration of how Principal can be injected
     * Here, as demonstration, we want to do audit as only ROLE_ADMIN can create user..
     * @throws UnirestException 
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Account> create(@RequestHeader("Authorization") String authorization_header, 
    						@RequestBody Account account,
    						final Principal principal) 
    				throws InvalidUserException, UnirestException{
        logger.info("create invoked");
        
        OrientConnectionFactory conn = new OrientConnectionFactory();
       // ServicePackage alpha_pkg = pkg_repo.find(conn, "alpha");
       
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
        logger.info("Current user accessed Admin secured resource: " + currentUser.getUsername());

        if(currentUser.getUsername().equals("UNKNOWN_USER")){
        	throw new InvalidUserException();
        	//return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR_500).body(null);
        }
        
        //create account
        Account acct = new Account(currentUser.getUsername(), account.getServicePackage(), account.getPaymentAcctNum(), new ArrayList<QanairyUser>());
        AccountRepository acct_repo = new AccountRepository();
        
        //Create user
        QanairyUser user = new QanairyUser(currentUser.getUsername());
        acct.addUser(user);
        
       String user_id = auth0Client.getUserId((Auth0JWTToken) principal);
       
       // Connect to Auth0 API and update user metadata
       Auth0ManagementApi.updateUserAppMetadata(user_id, "{\"status\": \"account_owner\"}");
        
        printGrantedAuthorities((Auth0JWTToken) principal);
        if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            final String username = usernameService.getUsername();
            // log username of user requesting account creation
            logger.info("User with email: " + username + " creating new account");
        }

        return ResponseEntity.accepted().body(acct_repo.create(conn, acct));
    }

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
     */
    private void printGrantedAuthorities(final Auth0JWTToken principal) {
        for(final GrantedAuthority grantedAuthority: principal.getAuthorities()) {
            final String authority = grantedAuthority.getAuthority();
            logger.info(authority);
        }
    }
}
