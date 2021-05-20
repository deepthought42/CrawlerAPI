package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.api.exceptions.MissingSubscriptionException;
import com.looksee.dto.DomainDto;
import com.looksee.dto.PageStatisticDto;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.repository.TestUserRepository;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

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
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AccountService account_service;
	
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
    //@PreAuthorize("hasAuthority('write:domains')")
    @RequestMapping(method = RequestMethod.POST)
    public @ResponseBody Domain create(HttpServletRequest request, 
    									@RequestBody(required=true) Domain domain
	) throws UnknownAccountException, MalformedURLException {

    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	log.warn("user id  :: "+id);
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		log.warn("account not found");
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	String lowercase_url = domain.getUrl().toLowerCase();
    	
    	log.warn("domain url ::   "+lowercase_url);
    	String formatted_url = BrowserUtils.sanitizeUserUrl(lowercase_url );
    	log.warn("sanitized domain url ::   "+formatted_url);
    	domain.setUrl(formatted_url.replace("http://", ""));
    	
		try{
			Domain domain_record = account_service.findDomain(acct.getUsername(), domain.getUrl());
			if(domain_record == null) {
				domain = domain_service.save(domain);
				account_service.addDomainToAccount(acct, domain);
			}
			else {
				throw new ExistingAccountDomainException();
			}
		}catch(Exception e){
			domain = null;
		}
    	
		try {
			MessageBroadcaster.sendDomainAdded(acct.getUserId(), domain);
		} catch (JsonProcessingException e) {
			log.error("Error occurred while sending domain message to user");
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
    @PreAuthorize("hasAuthority('write:domains')")
    @RequestMapping(method = RequestMethod.PUT)
    public @ResponseBody Domain update(HttpServletRequest request,
				 @RequestParam(value="key", required=true) String key,
	    		 @RequestParam(value="protocol", required=true) String protocol,
		   		 @RequestParam(value="browser_name", required=true) String browser_name,
		   		 @RequestParam(value="logo_url", required=false) String logo_url
								   		 
    ) throws UnknownAccountException, MalformedURLException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Domain domain = domain_service.findByKey(key, acct.getUsername());
    	domain.setLogoUrl(logo_url);
    	
    	return domain_service.save(domain);
    }
    
    /**
     * Create a new {@link Domain domain}
     * 
     * @throws UnknownUserException 
     * @throws UnknownAccountException 
     * @throws MalformedURLException 
     */
    @PreAuthorize("hasAuthority('write:domains')")
    @RequestMapping(path="/select", method = RequestMethod.PUT)
    public @ResponseBody void selectDomain(HttpServletRequest request,
    									@RequestBody Domain domain) 
    											throws UnknownAccountException, 
														MalformedURLException {

    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
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
    public @ResponseBody Set<DomainDto> getAll(HttpServletRequest request) throws UnknownAccountException {        
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);
    	if(acct == null){
    		log.warn("unknwon account...");
    		throw new UnknownAccountException();
    	}
    	/*
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	*/
    	log.warn("looking up account for domains 2 ...."+acct.getUsername());

    	Set<Domain> domains = account_service.getDomainsForUser(acct.getUsername());
    	Set<DomainDto> domain_info_set = new HashSet<>();
    	for(Domain domain: domains) {
    		Optional<DomainAuditRecord> audit_record_opt = domain_service.getMostRecentAuditRecord(domain.getId());
    		
    		int page_count = 0;
    		if(!audit_record_opt.isPresent()) {
    			page_count = audit_record_service.getPageAuditRecords(domain.getId()).size();

    			domain_info_set.add( new DomainDto(domain.getId(), domain.getUrl(), page_count, 0, 0, 0, 0) );
    			continue;
    		}
    		//get most recent audit record for this domain
    		AuditRecord audit = audit_record_opt.get();
    		log.warn("content audit found ..."+audit.getId());
    		
	    	//get all content audits for most recent audit record and calculate overall score
    		Set<Audit> content_audits = audit_record_service.getAllContentAuditsForDomainRecord(audit.getId());
    		log.warn("domain content audit record size  -    "+content_audits.size());
    		double content_score = AuditUtils.calculateScore(content_audits);
    		
	    	//get all info architecture audits for most recent audit record and calculate overall score
    		Set<Audit> info_arch_audits = audit_record_service.getAllInformationArchitectureAuditsForDomainRecord(audit.getId());
    		log.warn("domain info arch record size  -    "+info_arch_audits.size());
    		double info_arch_score = AuditUtils.calculateScore(info_arch_audits);
    		
    		//get all accessibility audits for most recent audit record and calculate overall score
    		Set<Audit> accessibility_audits = audit_record_service.getAllAccessibilityAuditsForDomainRecord(audit.getId());
    		double accessibility_score = AuditUtils.calculateScore(accessibility_audits);
    		
    		//get all Aesthetic audits for most recent audit record and calculate overall score
    		Set<Audit> aesthetics_audits = audit_record_service.getAllAestheticAuditsForDomainRecord(audit.getId());
    		log.warn("domain aesthetic record size  -    "+aesthetics_audits.size());
    		double aesthetics_score = AuditUtils.calculateScore(aesthetics_audits);
    		
    		//build domain stats
	    	//add domain stat to set
			page_count = audit_record_service.getPageAuditRecords(domain.getId()).size();

    		domain_info_set.add( new DomainDto(domain.getId(), domain.getUrl(), page_count, content_score, info_arch_score, accessibility_score, aesthetics_score) );
    	}
    	return domain_info_set;
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
    	String id = principal.getName();
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
	
	/**
	 * Retrieves pages for a given domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException 
	 */
	@PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path="/{domain_id}/pages")
	public @ResponseBody Set<PageStatisticDto> getPages(HttpServletRequest request,
							@PathVariable(value="domain_id", required=true) long domain_id
	)throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);
	
		if(acct == null){
			throw new UnknownAccountException();
		}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}		
		
		Set<PageStatisticDto> page_stats = new HashSet<>();
		//get latest domain audit record
		Optional<DomainAuditRecord> domain_audit_record = audit_record_service.findMostRecentDomainAuditRecord(domain_id);
		
		Set<PageAuditRecord> page_audits = audit_record_service.getPageAuditRecords(domain_audit_record.get().getId());
		for(PageAuditRecord page_audit : page_audits) {
			PageState page_state = audit_record_service.getPageStateForAuditRecord(page_audit.getId());
			double content_score = AuditUtils.calculateScore(audit_record_service.getAllContentAudits(page_audit.getId()));
			double info_architecture_score = AuditUtils.calculateScore(audit_record_service.getAllInformationArchitectureAudits(page_audit.getId()));
			double aesthetic_score = AuditUtils.calculateScore(audit_record_service.getAllAestheticAudits(page_audit.getId()));
			double accessibility_score = AuditUtils.calculateScore(audit_record_service.getAllAccessibilityAudits(page_audit.getId()));
			
			PageStatisticDto page = new PageStatisticDto(
										page_state.getId(), 
										page_state.getUrl(), 
										page_state.getViewportScreenshotUrl(), 
										content_score, 
										info_architecture_score,
										accessibility_score,
										aesthetic_score,
										page_audit.getId());	
			page_stats.add(page);
		}

		return page_stats;
	}
	
	@PreAuthorize("hasAuthority('read:domains')")
    @RequestMapping(method = RequestMethod.GET, path="/pages")
    public @ResponseBody Set<PageState> getAllPages(HttpServletRequest request, 
											   @RequestParam(value="url", required=true) String url
	) throws UnknownAccountException, MalformedURLException {        
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	URL url_obj = new URL(BrowserUtils.sanitizeUrl(url));
		Set<PageState> pages = domain_service.getPages(url_obj.getHost());
		
		// TODO filter through pages to get most recent for each unique page url
		log.info("###### PAGE STATE COUNT :: "+pages.size());
		return pages;
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

		Set<Element> page_elements = domain_service.getElementStates(host, acct.getUsername());
		log.info("###### PAGE ELEMENT COUNT :: "+page_elements.size());
		return page_elements;
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
    											throws UnknownAccountException, 
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
    		Set<TestUser> test_users = domain_service.getTestUsers(account.getUsername(), domain.getKey());
    		
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
    @RequestMapping(path="test_users/$user_id", method = RequestMethod.DELETE)
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
    	
		domain_service.deleteTestUser(account.getUsername(), domain_key, username);
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
    											throws UnknownAccountException, 
														MalformedURLException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account account = account_service.findByUserId(id);
    	
    	if(account == null){
    		throw new UnknownAccountException();
    	}
    	
    	Optional<Domain> optional_domain = domain_service.findById(domain_id);
    	if(optional_domain.isPresent()){
    		Domain domain = optional_domain.get();
    		Set<TestUser> users = domain_service.getTestUsers(account.getUsername(), domain.getKey());

    		return users;
    	}
    	else{
    		throw new DomainNotFoundException();
    	}
    }

    /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
    @PreAuthorize("hasAuthority('execute:audits')")
	@RequestMapping(path="/{domain_id}/start", method = RequestMethod.POST)
	public @ResponseBody AuditRecord startAudit(
			HttpServletRequest request,
			@PathVariable("domain_id") long domain_id
	) throws Exception {
    	Principal principal = request.getUserPrincipal();
    	String user_id = principal.getName();
    	Account account = account_service.findByUserId(user_id);
    	
    	if(account == null){
    		throw new UnknownAccountException();
    	}
    	log.warn("looking for domain by id :: "+domain_id);
    	Optional<Domain> domain_opt = domain_service.findById(domain_id);
    	if(!domain_opt.isPresent()) {
    		throw new DomainNotFoundException();
    	}
    	
    	Domain domain = domain_opt.get();
    	String lowercase_url = domain.getUrl().toLowerCase();
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));

	   	System.out.println("domain returned from db id ...."+domain.getId());
	   	System.out.println("domain returned from db key ...."+domain.getKey());
	   	System.out.println("domain returned from db url ...."+sanitized_url);
	   	//create new audit record
	   	AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.IN_PROGRESS);
	   	log.warn("audit record found ..."+audit_record.getKey());
	   	audit_record = audit_record_service.save(audit_record);
	   	
	   	domain_service.addAuditRecord(domain.getId(), audit_record.getKey());
	   	account_service.addAuditRecord(account.getUsername(), audit_record.getId());
	   	
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, domain, account.getUserId(), audit_record, false, sanitized_url);
		audit_manager.tell(crawl_action, null);
	   	
	   	return audit_record;
	}
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/audits")
    public DomainAuditRecord getMostRecentDomainAuditRecord(HttpServletRequest request,
			@PathVariable(value="host", required=true) String host
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        log.info("finding all page insights :: "+host);
        return domain_service.getMostRecentAuditRecord(host).get();
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
