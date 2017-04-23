package com.minion.api;

import java.security.Principal;
import java.util.List;

import org.omg.CORBA.UnknownUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.auth0.spring.security.api.Auth0JWTToken;
import com.auth0.spring.security.api.Auth0UserDetails;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.QanairyUser;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.QanairyUserRepository;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AccountService accountService;
    
    @Autowired
    protected DomainService domainService;

    
    /**
     * Create a new {@link Domain domain}
     * 
     * @throws UnknownUserException 
     * @throws UnknownAccountException 
     */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody Domain create(final @Validated @RequestBody String url, final Principal principal) throws UnknownUserException, UnknownAccountException {
        /*printGrantedAuthorities((Auth0JWTToken) principal);
        if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            
            // log username of user requesting domain creation
            logger.info("creating new domain in domain");
        }*/
        OrientConnectionFactory conn = new OrientConnectionFactory();

    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
        QanairyUserRepository user_repo = new QanairyUserRepository();
        QanairyUser user = user_repo.find(conn, currentUser.getUsername());
    	if(user == null){
    		throw new UnknownUserException();
    	}
    	
    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        logger.info("Creating domain with url : " + url);
        Domain domain = new Domain(url);
    	acct.addDomain(domain);
    	accountService.update(acct);
        Domain domain2 = domainService.create(domain);
        return domain2;
    }


    @PreAuthorize("hasAuthority('qanairy')")
    @RequestMapping(method = RequestMethod.GET)
    public  @ResponseBody List<Domain> getAll() {
        logger.info("get invoked");
	    DomainRepository domain_repo = new DomainRepository();
	    List<Domain> domains = domain_repo.findAll(new OrientConnectionFactory());

	    return domains;
    }
    
   /* @RequestMapping(value ="/domains/{id}", method = RequestMethod.GET)
    public Domain get(final @PathVariable String key) {
        logger.info("get invoked");
        return domainService.get(key);
    }
*/
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
    @RequestMapping(value ="/domains/{id}", method = RequestMethod.PUT)
    public @ResponseBody Domain update(final @PathVariable String key, final @Validated @RequestBody Domain domain) {
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
