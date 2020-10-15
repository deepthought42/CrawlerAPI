package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

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

import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.models.Account;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.Element;
import com.qanairy.models.PageState;
import com.qanairy.models.LookseeObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.TestUser;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.models.repository.FormRepository;
import com.qanairy.models.repository.TestUserRepository;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.qanairy.services.FormService;
import com.qanairy.services.RedirectService;
import com.qanairy.services.SubscriptionService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping("/domains")
public class DomainController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static Map<String, ActorRef> domain_actors = new HashMap<>();

	@Autowired
	private AccountService account_service;
	
	@Autowired
	private RedirectService redirect_service;
	
	@Autowired
	private DomainService domain_service;
	
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
    	
    	String lowercase_url = url.toLowerCase();
    	String formatted_url = BrowserUtils.sanitizeUserUrl(protocol+"://"+lowercase_url );
    	URL url_obj = new URL(formatted_url);
		/*
    	String sanitized_url = url_obj.getHost()+url_obj.getPath();
		
		//check if qanairy domain. prevent creating if user email isn't a qanairy.com email
		if(sanitized_url.contains("qanairy.com") && !acct.getUsername().contains("qanairy.com")) {
			throw new QanairyEmployeesOnlyException();
		}
		*/
    	Domain domain = new Domain(protocol, url_obj.getHost(), url_obj.getPath(), browser_name, logo_url);
		try{
			domain = domain_service.save(domain);
		}catch(Exception e){
			domain = null;
		}
		
		//check if domain is on account
		Domain domain_record = null;
		if(domain == null) {
			domain_record = domain_service.findByHostForUser(url_obj.getHost(), acct.getUserId());
		}
		
		if(domain_record == null) {
			account_service.addDomainToAccount(acct, domain);
		}
    	
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
    	
    	Domain domain = domain_service.findByKey(key, acct.getUserId());
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
    	
    	acct.setLastDomain(domain.getEntryPath());
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
    													  @RequestParam(value="url", required=true) String url) 
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
		

		Set<PageState> page_states = domain_service.getPageStates(url);
		log.info("###### PAGE STATE COUNT :: "+page_states.size());
		return page_states;
    }
	
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/pages")
    public @ResponseBody Set<PageVersion> getAllPages(HttpServletRequest request, 
											   @RequestParam(value="url", required=true) String url
	) throws UnknownAccountException {        
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		Set<PageVersion> pages = domain_service.getPagesForUser(acct.getUserId(), url);
		log.info("###### PAGE STATE COUNT :: "+pages.size());
		return pages;
    	
    			
	    //return new HashSet<PageState>();
    }

	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/path")
    public @ResponseBody Set<LookseeObject> getAllPathObjects(HttpServletRequest request, 
    													   @RequestParam(value="url", required=true) String url
    ) throws UnknownAccountException {        		
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
		
		Set<PageState> page_state = domain_service.getPageStates(url);
		Set<Element> page_elem = domain_service.getElementStates(url, acct.getUserId());
		Set<Action> actions = domain_service.getActions(acct.getUserId(), url);
		Set<Redirect> redirects = redirect_service.getRedirects(acct.getUserId(), url);
		Set<PageLoadAnimation> animations = domain_service.getAnimations(acct.getUserId(), url);
		Set<LookseeObject> path_objects = new HashSet<LookseeObject>();
		//merge(page_state, page_elem, actions);

		path_objects.addAll(redirects);
		path_objects.addAll(page_state);
		path_objects.addAll(page_elem);
		path_objects.addAll(actions);
		path_objects.addAll(animations);
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
	 * @return a unique set of {@link Element}s belonging to all page states for the {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/page_elements")
    public @ResponseBody Set<Element> getAllElementStates(HttpServletRequest request, 
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

		Set<Element> page_elements = domain_service.getElementStates(host, acct.getUserId());
		log.info("###### PAGE ELEMENT COUNT :: "+page_elements.size());
		return page_elements;
    }
	
	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link Element}s belonging to all page states for the {@link Domain} with the given host
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
    		return domain_service.getForms(acct.getUserId(), domain.get().getEntryPath());
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
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account account = account_service.findByUserId(id);
    	
    	if(account == null){
    		throw new UnknownAccountException();
    	}
    	
    	Optional<Domain> optional_domain = domain_service.findById(domain_id);
    	
		log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
    	log.info("starting to add user");
    	if(optional_domain.isPresent()){
    		Domain domain = optional_domain.get();
    		log.info("domain : "+domain);
    		Set<TestUser> test_users = domain_service.getTestUsers(account.getUserId(), domain.getKey());
    		
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
	 * @throws UnknownAccountException 
	 * @throws UnknownUserException
	 */
    @PreAuthorize("hasAuthority('create:test_user')")
    @RequestMapping(path="test_users/{user_id}", method = RequestMethod.DELETE)
    public @ResponseBody void delete(HttpServletRequest request,
    									@RequestParam(value="domain_key", required=true) String domain_key,
    									@RequestParam(value="username", required=true) String username
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account account = account_service.findByUserId(id);
    	
    	if(account == null){
    		throw new UnknownAccountException();
    	}
    	
		domain_service.deleteTestUser(account.getUserId(), domain_key, username);
    }
    
    /**
     * 
     * @param request
     * @param domain_id
     * @return
     * @throws UnknownUserException
     * @throws UnknownAccountException
     * @throws MalformedURLException
     */
    @PreAuthorize("hasAuthority('create:domains')")
    @RequestMapping(path="{domain_id}/users", method = RequestMethod.GET)
    public @ResponseBody Set<TestUser> getUsers(HttpServletRequest request,
    									@PathVariable(value="domain_id", required=true) long domain_id) 
    											throws UnknownUserException, 
														UnknownAccountException, 
														MalformedURLException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account account = account_service.findByUserId(id);
    	
    	if(account == null){
    		throw new UnknownAccountException();
    	}
    	
    	Optional<Domain> optional_domain = domain_service.findById(domain_id);
    	if(optional_domain.isPresent()){
    		Domain domain = optional_domain.get();
    		Set<TestUser> users = domain_service.getTestUsers(account.getUserId(), domain.getKey());

    		return users;
    	}
    	else{
    		throw new DomainNotFoundException();
    	}
    }

	/**
	 *
	 * @param request
	 * @param account_key key of account to stop work for
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException
	 */
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping("{domain_id}/stop")
	public @ResponseBody void stopDiscovery(HttpServletRequest request, @RequestParam(value="url", required=true) String url)
			throws MalformedURLException, UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	/*
    	DiscoveryRecord last_discovery_record = null;
		Date started_date = new Date(0L);
		for(DiscoveryRecord record : domain_service.getDiscoveryRecords(url)){
			if(record.getStartTime().compareTo(started_date) > 0 && record.getDomainUrl().equals(url)){
				started_date = record.getStartTime();
				last_discovery_record = record;
			}
		}

		last_discovery_record.setStatus(DiscoveryStatus.STOPPED);
		discovery_service.save(last_discovery_record);
		WorkAllowanceStatus.haltWork(acct.getUsername());
		*/
    	Domain domain = domain_service.findByUrlAndAccountId(url, acct.getUserId());

    	if(!domain_actors.containsKey(domain.getEntryPath())){
			ActorRef domain_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("domainActor"), "domain_actor"+UUID.randomUUID());
			domain_actors.put(domain.getEntryPath(), domain_actor);
		}
    	
		DiscoveryActionMessage discovery_action_msg = new DiscoveryActionMessage(DiscoveryAction.STOP, domain, acct.getUserId(), BrowserType.create(domain.getDiscoveryBrowserName()));
		domain_actors.get(url).tell(discovery_action_msg, null);
		
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RequiredFieldMissingException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public RequiredFieldMissingException() {
		super("Please fill in or select all required fields.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class QanairyEmployeesOnlyException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716215L;

	public QanairyEmployeesOnlyException() {
		super("It looks like you tried to add a Qanairy domain. If you would like to test Qanairy, please apply by emailing us at careers@qanairy.com.");
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
