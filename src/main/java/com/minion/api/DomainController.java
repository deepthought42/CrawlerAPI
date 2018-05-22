package com.minion.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.omg.CORBA.UnknownUserException;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.DomainPOJO;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.Domain;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

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
							    		 @RequestParam(value="protocol", required=true) String protocol,
							    		 @RequestParam(value="url", required=true) String url,
							    		 @RequestParam(value="discoveryBrowser", required=true) String discoveryBrowser,
							    		 @RequestParam(value="logo_url", required=false) String logo_url) 
    											throws UnknownUserException, UnknownAccountException, MalformedURLException {
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = AccountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	//check if domain should have a 'www.' or not. We do this for consistency of naming in the database
    	int dot_idx = url.indexOf('.');
    	int last_dot_idx = url.lastIndexOf('.');
    	String formatted_url = url;
    	if(dot_idx == last_dot_idx){
    		formatted_url = "www."+url;
    	}
    	URL url_obj = new URL(protocol+"://"+formatted_url);
		
    	DomainPOJO domain_pojo = new DomainPOJO(protocol, url_obj.getHost(), discoveryBrowser, logo_url);
		
		Domain domain = DomainService.save(domain_pojo);
    	acct.addDomain(domain);
    	acct.setLastDomain(domain.getUrl());
    	//AccountService.save(acct);
    	return domain;
    }

    /**
     * Create a new {@link Domain domain}
     * 
     * @throws UnknownUserException 
     * @throws UnknownAccountException 
     * @throws MalformedURLException 
     */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(method = RequestMethod.PUT)
    public @ResponseBody Domain update(HttpServletRequest request,
    									@RequestBody DomainPOJO domain) throws UnknownUserException, UnknownAccountException, MalformedURLException {
        //printGrantedAuthorities((Auth0JWTToken) principal);
        /*if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            
            // log username of user requesting domain creation
            logger.info("creating new domain in domain");
        }*/
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = AccountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	return DomainService.save(domain);
    }
    
    /**
     * Create a new {@link Domain domain}
     * 
     * @throws UnknownUserException 
     * @throws UnknownAccountException 
     * @throws MalformedURLException 
     */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(path="/select", method = RequestMethod.PUT)
    public @ResponseBody void selectDomain(HttpServletRequest request,
    									@RequestBody DomainPOJO domain) throws UnknownUserException, UnknownAccountException, MalformedURLException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = AccountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	acct.setLastDomain(domain.getUrl());
    	AccountService.save(acct);
    }

    @PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody List<Domain> getAll(HttpServletRequest request) throws UnknownAccountException {        
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	
    	Account acct = AccountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
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

		Account acct = AccountService.find(username);
	
		if(acct == null){
			throw new UnknownAccountException();
		}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}		
		
		Domain domain = DomainService.get(key);
		AccountService.deleteDomain(acct, domain);
		
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

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RequiredFieldMissingException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public RequiredFieldMissingException() {
		super("Please fill in or select all required fields.");
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class ExistingAccountDomainException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public ExistingAccountDomainException() {
		super("This domain already exists for your account.");
	}
}
