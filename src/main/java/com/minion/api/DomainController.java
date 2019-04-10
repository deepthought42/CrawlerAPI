package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.omg.CORBA.UnknownUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.minion.structs.Message;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.integrations.DeepthoughtApi;
import com.qanairy.models.Account;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.TestUser;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.FormStatus;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.FormRepository;
import com.qanairy.models.repository.TestUserRepository;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private FormRepository form_repo;
	
	@Autowired
    private ActorSystem actor_system;
	    
	@Autowired
	private TestUserRepository test_user_repo;
	   
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
							    		 @RequestParam(value="logo_url", required=false) String logo_url,
							    		 @RequestParam(value="test_users", required=false) List<TestUser> users) 
    											throws UnknownUserException, UnknownAccountException, MalformedURLException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

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
    	formatted_url = formatted_url.replace("http://", "");
    	formatted_url = formatted_url.replace("https://", "");
    	protocol = "http";
    	URL url_obj = new URL(protocol+"://"+formatted_url);
		
    	Domain domain = new Domain(protocol, url_obj.getHost(), browser_name, logo_url);
		try{
			domain = domain_service.save(domain);
		}catch(Exception e){
			domain = domain_service.findByHost(url_obj.getHost());
		}
		
    	acct.addDomain(domain);
    	acct.setLastDomain(url_obj.getHost());
    	account_service.save(acct);
    	
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
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Domain domain = domain_service.findByKey(key);
    	domain.setDiscoveryBrowserName(browser_name);
    	domain.setLogoUrl(logo_url);
    	domain.setProtocol(protocol);
    	
    	return domain_service.save(domain);
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

    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	acct.setLastDomain(domain.getUrl());
    	account_service.save(acct);
    }

    @PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Set<Domain> getAll(HttpServletRequest request) throws UnknownAccountException {        
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Set<Domain> domains = account_service.getDomains(id);
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
	@RequestMapping(method = RequestMethod.DELETE, path="/{domain_id}")
	public @ResponseBody void remove(HttpServletRequest request,
										@PathVariable(value="domain_id", required=true) long domain_id)
								   throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
	
		if(acct == null){
			throw new UnknownAccountException();
		}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}		
		
		Optional<Domain> domain = domain_service.findById(domain_id);
		if(domain.isPresent()){
			account_service.removeDomain(acct.getUsername(), domain.get().getKey());
		}
	}
    
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/page_states")
    public @ResponseBody Set<PageState> getAllPageStates(HttpServletRequest request, 
    													  @RequestParam(value="host", required=true) String host) 
    															throws UnknownAccountException {        
    	//String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	
    	//String username = auth.getUsername(auth_access_token);
    	
    	//Account acct = account_service.findByUsername(username);
    	//if(acct == null){
    	//	throw new UnknownAccountException();
    	//}
    	//else if(acct.getSubscriptionToken() == null){
    	//	throw new MissingSubscriptionException();
    	//}

		Set<PageState> page_states = domain_service.getPageStates(host);
		log.info("###### PAGE STATE COUNT :: "+page_states.size());
		return page_states;
    	
    			
	    //return new HashSet<PageState>();
    }

	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/path")
    public @ResponseBody Set<PathObject> getAllPathObjects(HttpServletRequest request, 
    													  @RequestParam(value="host", required=true) String host) 
    															throws UnknownAccountException {        		
		Set<PageState> page_state = domain_service.getPageStates(host);
		Set<ElementState> page_elem = domain_service.getElementStates(host);
		Set<Action> actions = domain_service.getActions(host);
		Set<PathObject> path_objects = new HashSet<PathObject>();
		//merge(page_state, page_elem, actions);

		path_objects.addAll(page_state);
		path_objects.addAll(page_elem);
		path_objects.addAll(actions);
		return path_objects;
    }
	
	@SafeVarargs
	public static <T> Set<T> merge(Collection<? extends T>... collections) {
	    Set<T> newSet = new HashSet<T>();
	    for (Collection<? extends T> collection : collections)
	        newSet.addAll(collection);
	    return newSet;
	}
	
	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link ElementState}s belonging to all page states for the {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/page_elements")
    public @ResponseBody Set<ElementState> getAllElementStates(HttpServletRequest request, 
    													  @RequestParam(value="host", required=true) String host) 
    															throws UnknownAccountException {        
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		Set<ElementState> page_elements = domain_service.getElementStates(host);
		log.info("###### PAGE ELEMENT COUNT :: "+page_elements.size());
		return page_elements;
    }
	
	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link ElementState}s belonging to all page states for the {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="{domain_id}/forms")
    public @ResponseBody Set<Form> getAllForms(HttpServletRequest request, 
												@PathVariable(value="domain_id", required=true) long domain_id)
														throws UnknownAccountException {        
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	Optional<Domain> domain = domain_service.findById(domain_id);
    	if(domain.isPresent()){
    		return domain_service.getForms(domain.get().getUrl());
    	}
    	else{
    		throw new DomainNotFoundException();
    	}
    }
	
	
	//USERS ENDPOINTS
	
	/**
	 * Create a new test user and add it to the domain
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(path="/{domain_id}/users", method = RequestMethod.POST)
    public @ResponseBody TestUser addUser(HttpServletRequest request,
    									@PathVariable(value="domain_id", required=true) long domain_id,
    									@RequestParam(value="username", required=true) String username,
    									@RequestParam(value="password", required=true) String password,
    									@RequestParam(value="role", required=false) String role,
    									@RequestParam(value="enabled", required=true) boolean enabled) 
    											throws UnknownUserException, 
														UnknownAccountException, 
														MalformedURLException {
    	Optional<Domain> optional_domain = domain_service.findById(domain_id);
    	
		log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
    	log.info("starting to add user");
    	if(optional_domain.isPresent()){
    		Domain domain = optional_domain.get();
    		log.info("domain : "+domain);
    		Set<TestUser> test_users = domain_service.getTestUsers(domain.getKey());
    		
    		log.info("Test users : "+test_users.size());
    		for(TestUser user : test_users){
    			if(user.getUsername().equals(username)){
    				log.info("User exists, returning user : "+user);
    				return user;
    			}
    		}
    		
    		log.info("Test user does not exist for domain yet");
    		
    		TestUser user = new TestUser(username, password, role, enabled);
    		user = test_user_repo.save(user);
    		Set<TestUser> users = new HashSet<TestUser>();
    		users.add(user);
    		domain.setTestUsers(users);
    		domain = domain_service.save(domain);
    		log.info("saved domain :: "+domain.getKey());
    		return user;
    	}
		throw new DomainNotFoundException();
    }
    
    

	/**
	 * 
	 * @param request
	 * @param user_id
	 * @throws UnknownUserException
	 */
    @PreAuthorize("hasAuthority('create:test_user')")
    @RequestMapping(path="test_users/{user_id}", method = RequestMethod.DELETE)
    public @ResponseBody void delete(HttpServletRequest request,
    									@RequestParam(value="domain_key", required=true) String domain_key,
    									@RequestParam(value="username", required=true) String username) {
		domain_service.deleteTestUser(domain_key, username);
    }
    
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(path="{domain_id}/users", method = RequestMethod.GET)
    public @ResponseBody Set<TestUser> getUsers(HttpServletRequest request,
    									@PathVariable(value="domain_id", required=true) long domain_id) 
    											throws UnknownUserException, 
														UnknownAccountException, 
														MalformedURLException {
    	Optional<Domain> optional_domain = domain_service.findById(domain_id);
    	if(optional_domain.isPresent()){
    		Domain domain = optional_domain.get();
    		Set<TestUser> users = domain_service.getTestUsers(domain.getKey());

    		return users;
    	}
    	else{
    		throw new DomainNotFoundException();
    	}
    }
    
    /**
     * Retrieves {@link FormRecord account} with a given key
     * 
     * @param key account key
     * @return {@link FormRecord account}
     * @throws IOException 
     * @throws UnknownAccountException 
     */

	@PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(path="{domain_id}/forms", method = RequestMethod.PUT)
    public @ResponseBody void updateForm(HttpServletRequest request,
    							 @PathVariable(value="domain_id", required=true) long domain_id,
    							 @RequestParam(value="key", required=true) String key,
    							 @RequestParam(value="name", required=false) String name,
    							 @RequestParam(value="type", required=true) String form_type) throws IOException, UnknownAccountException {
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
		
		Form form_record = form_repo.findByKey(key);

		if(form_record == null){
			throw new FormNotFoundException();
		}
		else{
			if(name!=null && !name.isEmpty()){
				form_record.setName(name);
			}
			form_record.setType(FormType.create(form_type.toLowerCase()));
			
			if(!form_record.getType().equals(FormType.UNKNOWN)){
				form_record.setStatus(FormStatus.CLASSIFIED);
			}
			
	        //learn from form classification    
	    	DeepthoughtApi.learn(form_record);
	    
	    	form_record = form_repo.save(form_record);
	
			Optional<Domain> optional_domain = domain_service.findById(domain_id);
			log.info("Does the domain exist :: "+optional_domain.isPresent());
	    	if(optional_domain.isPresent()){
	    		Domain domain = optional_domain.get();
	        		
	    		log.info("domain exists with domain :: "+domain.getUrl()+ "  ::   "+domain.getDiscoveryBrowserName());
	    		//start form test creation actor
	    		Map<String, Object> options = new HashMap<String, Object>();
				options.put("browser", domain.getDiscoveryBrowserName());
		        options.put("host", domain.getUrl());
		        Message<Form> form_msg = new Message<Form>(acct.getUsername(), form_record, options);
	
		        log.info("Sending form message  :: "+form_msg.toString());
	    		final ActorRef form_test_discovery_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
	  				  .props("formTestDiscoveryActor"), "form_test_discovery_actor"+UUID.randomUUID());
	    		form_test_discovery_actor.tell(form_msg, ActorRef.noSender());
	    	}
	    	else{
	    		throw new DomainNotFoundException();
	    	}
		}
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RequiredFieldMissingException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public RequiredFieldMissingException() {
		super("Please fill in or select all required fields.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DomainNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public DomainNotFoundException() {
		super("Domain could not be found.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class FormNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public FormNotFoundException() {
		super("Form could not be found.");
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class ExistingAccountDomainException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public ExistingAccountDomainException() {
		super("This domain already exists for your account.");
	}
}
