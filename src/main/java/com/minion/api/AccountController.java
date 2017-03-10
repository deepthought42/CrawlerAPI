package com.minion.api;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.auth0.spring.security.api.Auth0JWTToken;
import com.qanairy.auth.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.services.AccountService;
import com.qanairy.services.UsernameService;

/**
 *	API endpoints for interacting with {@link User} data
 */
@Controller
@RequestMapping("/account")
public class AccountController {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected UsernameService usernameService;
    
    /**
     * Simple demonstration of how Principal can be injected
     * Here, as demonstration, we want to do audit as only ROLE_ADMIN can create user..
     */
    @RequestMapping(value ="accounts", method = RequestMethod.POST)
    public Account create(final @Validated @RequestBody Account account, final Principal principal) {
        logger.info("create invoked");
        printGrantedAuthorities((Auth0JWTToken) principal);
        if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            final String username = usernameService.getUsername();
            // log username of user requesting account creation
            logger.info("User with email: " + username + " creating new account");
        }
        return accountService.create(account);
    }

    @RequestMapping(value ="accounts/{id}", method = RequestMethod.GET)
    public Account get(final @PathVariable String key) {
        logger.info("get invoked");
        return accountService.get(key);
    }

    @RequestMapping(value ="accounts/{id}", method = RequestMethod.PUT)
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
