package com.minion.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.omg.CORBA.UnknownUserException;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	AccountRepository account_repo;
	
	@Autowired
	DomainRepository domain_repo;
	
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
							    		 @RequestParam(value="browser_name", required=true) String browser_name,
							    		 @RequestParam(value="logo_url", required=false) String logo_url) 
    											throws UnknownUserException, UnknownAccountException, MalformedURLException {
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
       	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);

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
		
    	Domain domain = new Domain(protocol, url_obj.getHost(), browser_name, logo_url);
		try{
			domain = domain_repo.save(domain);
		}catch(Exception e){
			domain = domain_repo.findByHost(url_obj.getHost());
		}
		
    	acct.addDomain(domain);
    	acct.setLastDomain(url_obj.getHost());
    	account_repo.save(acct);
    	
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
   		 								 @RequestParam(value="key", required=true) String key,
							    		 @RequestParam(value="protocol", required=true) String protocol,
								   		 @RequestParam(value="browser_name", required=true) String browser_name,
								   		 @RequestParam(value="logo_url", required=false) String logo_url) 
    											throws UnknownUserException, 
    													UnknownAccountException, 
    													MalformedURLException {
        //printGrantedAuthorities((Auth0JWTToken) principal);
        /*if ("ROLES".equals(appConfig.getAuthorityStrategy())) {
            
            // log username of user requesting domain creation
            logger.info("creating new domain in domain");
        }*/
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Domain domain = domain_repo.findByKey(key);
    	domain.setDiscoveryBrowserName(browser_name);
    	domain.setLogoUrl(logo_url);
    	domain.setProtocol(protocol);
    	
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
    @RequestMapping(path="/select", method = RequestMethod.PUT)
    public @ResponseBody void selectDomain(HttpServletRequest request,
    									@RequestBody Domain domain) 
    											throws UnknownUserException, 
														UnknownAccountException, 
														MalformedURLException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	acct.setLastDomain(domain.getUrl());
    	account_repo.save(acct);
    }

    @PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Set<Domain> getAll(HttpServletRequest request) throws UnknownAccountException {        
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Set<Domain> domains = account_repo.getDomains(username);
    	System.err.println("Domain size :: "+domains.size());
	    return domains;
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

		Account acct = account_repo.findByUsername(username);
	
		if(acct == null){
			throw new UnknownAccountException();
		}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}		
		
		Domain domain = domain_repo.findByKey(key);
		acct.getDomains().remove(domain);
		account_repo.save(acct);
		
	    return domain;
	}
    
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/page_states")
    public @ResponseBody Set<PageState> getAllPageStates(HttpServletRequest request, 
    													  @RequestParam(value="host", required=true) String host) 
    															throws UnknownAccountException {        
    	//String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	//Auth0Client auth = new Auth0Client();
    	//String username = auth.getUsername(auth_access_token);
    	
    	//Account acct = account_repo.findByUsername(username);
    	//if(acct == null){
    	//	throw new UnknownAccountException();
    	//}
    	//else if(acct.getSubscriptionToken() == null){
    	//	throw new MissingSubscriptionException();
    	//}

		System.err.println("$$$$$$ GETTING PAGE STATES FOR HOST :: "+host);
		Set<PageState> page_states = domain_repo.getPageStates(host);
		System.err.println("###### PAGE STATE COUNT :: "+page_states.size());
		return page_states;
    	
    			
	    //return new HashSet<PageState>();
    }
	
	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link PageElement}s belonging to all page states for the {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/page_elements")
    public @ResponseBody Set<PageElement> getAllPageElements(HttpServletRequest request, 
    													  @RequestParam(value="host", required=true) String host) 
    															throws UnknownAccountException {        
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	
    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	/*Set<PageElement> unique_page_elements = new HashSet<PageElement>();
    	Set<PageElement> page_elements = new HashSet<PageElement>();
    	for(Domain domain : acct.getDomains()){
    		for(PageState page_state : domain_repo.getPageStates(host)){
    			boolean element_exists = false;
    			for(PageElement element : page_state.getElements()){
    				for(PageElement unique : unique_page_elements){
    					if(element.getKey().equals(unique.getKey())){
    						element_exists = true;
    					}
    				}
    				if(!element_exists){
    					unique_page_elements.add(element);
    				}
    			}
    			page_elements.addAll(page_state.getElements());
    		}
    	} 
    	*/
    	System.err.println("$$$$$$ GETTING PAGE ELEMENTS FOR HOST :: "+host);
		Set<PageElement> page_elements = domain_repo.getPageElements(host);
		System.err.println("###### PAGE ELEMENT COUNT :: "+page_elements.size());
		return page_elements;
    	//	    return domain_repo.getPageElements();

	    //return unique_page_elements;
    }
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
