package com.minion.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.omg.CORBA.UnknownUserException;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AccountService accountService;
    
    @Autowired
    protected DomainService domainService;

    /**
     * Create a new {@link Domain domain}
     * 
     * @throws UnknownUserException 
     * @throws UnknownAccountException 
     * @throws MalformedURLException 
     */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody Domain create(HttpServletRequest request,
    									@RequestBody Domain domain) throws UnknownUserException, UnknownAccountException, MalformedURLException {
        //printGrantedAuthorities((Auth0JWTToken) principal);
        /*if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            
            // log username of user requesting domain creation
            logger.info("creating new domain in domain");
        }*/
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	System.err.println("Auth Access token :: "+auth_access_token);
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
    	URL url_obj = new URL(domain.getProtocol()+"://"+domain.getUrl());
		domain.setUrl(url_obj.getHost());
    	
    	acct.addDomain(domain);
    	accountService.update(acct);
    	return domainService.create(domain);
    }


    @PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET)
    public  @ResponseBody List<Domain> getAll(HttpServletRequest request) throws UnknownAccountException {        
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	System.err.println("Auth Access token :: "+auth_access_token);
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	
    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}

	    return acct.getDomains();
    }
    
	/**
	 * Removes domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException 
	 */
	@PreAuthorize("hasAuthority('delete:domains')")
	@RequestMapping(method = RequestMethod.DELETE)
	public @ResponseBody Domain remove(HttpServletRequest request,
									   @RequestParam(value="key", required=true) String key) 
								   throws UnknownAccountException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

		Account acct = accountService.find(username);
	
		if(acct == null){
			throw new UnknownAccountException();
		}
		
		Domain domain = domainService.get(key);
		acct = accountService.deleteDomain(acct, domain);
		
	    return domain;
	}
    
    /**
     * Simple demonstration of how Principal info can be accessed
     */
    /*private void printGrantedAuthorities(final Auth0JWTToken principal) {
        for(final GrantedAuthority grantedAuthority: principal.getAuthorities()) {
            final String authority = grantedAuthority.getAuthority();
            logger.info(authority);
        }
    }
    */	
}
