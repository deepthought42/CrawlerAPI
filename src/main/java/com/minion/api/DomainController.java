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
import com.qanairy.models.Domain;
import com.qanairy.services.DomainService;

/**
 *	API endpoints for interacting with {@link User} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected WebSecurityConfig appConfig;

    @Autowired
    protected DomainService domainService;

    
    /**
     * Simple demonstration of how Principal can be injected
     * Here, as demonstration, we want to do audit as only ROLE_ADMIN can create user..
     */
    @RequestMapping(value ="domains", method = RequestMethod.POST)
    public Domain create(final @Validated @RequestBody Domain domain, final Principal principal) {
        logger.info("create invoked");
        printGrantedAuthorities((Auth0JWTToken) principal);
        if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            
            // log username of user requesting domain creation
            logger.info("creating new domain in domain");
        }
        return domainService.create(domain);
    }


    @RequestMapping(value ="domains", method = RequestMethod.GET)
    public Domain getAll(final @PathVariable String key) {
        logger.info("get invoked");
        return domainService.get(key);
    }
    
    @RequestMapping(value ="domains/{id}", method = RequestMethod.GET)
    public Domain get(final @PathVariable String key) {
        logger.info("get invoked");
        return domainService.get(key);
    }

    @RequestMapping(value ="domains/{id}", method = RequestMethod.PUT)
    public Domain update(final @PathVariable String key, final @Validated @RequestBody Domain domain) {
        logger.info("update invoked");
        return domainService.update(domain);
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
