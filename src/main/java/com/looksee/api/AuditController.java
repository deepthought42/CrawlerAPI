package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.api.exceptions.MissingSubscriptionException;
import com.looksee.browsing.Crawler;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditFactory;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.ElementIssueMap;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.IssueElementMap;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.PageAudits;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "audits", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

   	public final static long SECS_PER_HOUR = 60 * 60;

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_service;
	
	@Autowired
	private AccountService account_service;
	
    @Autowired
    private DomainService domain_service;
    
    @Autowired
    protected SecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected UXIssueMessageService issue_message_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    private ActorSystem actor_system;
    
    @Autowired
	private AuditFactory audit_factory;
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws MalformedURLException 
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Set<Audit> getAudits(HttpServletRequest request,
										@RequestParam(value="domain_host", required=true) String domain_host
	) throws MalformedURLException {
    	URL url = new URL(BrowserUtils.sanitizeUrl(domain_host));
    	Domain domain = domain_service.findByHost(url.getHost());
    	Optional<DomainAuditRecord> audit_record = domain_service.getMostRecentAuditRecord(domain.getId()); 
    	if(audit_record.isPresent()) {
    		return audit_record_service.getAllAudits(audit_record.get().getKey());
    	}
    	return new HashSet<>();
    }

    /**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/{id}")
    public @ResponseBody Set<Audit> getAudit(HttpServletRequest request,
									@PathVariable("id") long id
	) {
    	Set<Audit> audit_set = new HashSet<Audit>();
    	
    	Audit audit = audit_service.findById(id).get();
    	audit.setMessages( audit_service.getIssues(audit.getId()) );
        
    	audit_set.add(audit);
    	
        return audit_set;
    }
    
    
    /**
     * Retrieves set of {@link Audit audits} for the most recent page with a given page url
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    /*
    @RequestMapping(method= RequestMethod.GET, path="/pages")
    public @ResponseBody PageAudits getAuditsByPage(
    		HttpServletRequest request,
    		@RequestParam("url") String url
	) throws MalformedURLException {
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(url));
    	
    	String page_url = BrowserUtils.getPageUrl(sanitized_url);
    	
    	//Get most recent audits
    	Optional<PageAuditRecord> audit_record_opt = domain_service.getMostRecentPageAuditRecord(page_url);

    	if(!audit_record_opt.isPresent()) {
    		return null;
    	}
    	
    	PageAuditRecord audit_record = audit_record_opt.get();
   		//PageState page_state = page_state_service.findByUrl(page_url);
   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getId());

   		log.warn("page state found for audit record :: "+page_state.getKey());
	   	SimplePage simple_page = new SimplePage(
	   									page_state.getUrl(), 
	   									page_state.getViewportScreenshotUrl(), 
	   									page_state.getFullPageScreenshotUrl(), 
	   									page_state.getFullPageWidth(), 
	   									page_state.getFullPageHeight(),
	   									page_state.getSrc(), 
	   									page_state.getKey(), 
	   									page_state.getId());


    	log.warn("Audit record key :: "+audit_record.getKey());

   		PageAudits page_audits = new PageAudits( ExecutionStatus.COMPLETE, audits, simple_page);
    	
    	return page_audits;
    }
    */
    
    

	/**
     * Creates a new {@link Observation observation} 
     * 
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.POST, value="/$key/issues")
    public @ResponseBody UXIssueMessage addIssue(
							    		HttpServletRequest request,
										@PathVariable("key") String key,
							    		@RequestBody UXIssueMessage issue_message
	) throws UnknownAccountException {
    	/*
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	*/

    	//find audit by key
    	//find audit by key and add recommendation
    
    	//add observation to page

    	issue_message.setKey(issue_message.generateKey());
		issue_message = issue_message_service.save( issue_message );
		audit_service.addIssue(key, issue_message.getKey());
		return issue_message;

    }

    /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/{page_id}/start-individual", method = RequestMethod.POST)
	public @ResponseBody PageAudits startSinglePageAudit(
			HttpServletRequest request,
			@RequestParam("page_id") long page_id
	) throws Exception {    	
    	//String lowercase_url = page.getUrl().toLowerCase();
		Optional<PageState> page_opt = page_service.findById(page_id);
		if(!page_opt.isPresent()) {
			throw new PageNotFoundError();
		}
		PageState page = page_opt.get();
		
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(page.getUrl() ));
	   //	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+page.getUrl()));
	   	
    	/*
    	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain(sanitized_url.getProtocol(), sanitized_url.getHost(), sanitized_url.getPath(), "");
	   		domain = domain_service.save(domain);
	   	}
	*/
   		String page_url = sanitized_url.getHost()+sanitized_url.getPath();
	   	Optional<PageAuditRecord> audit_record_optional = audit_record_service.getMostRecentPageAuditRecord(page_url);
	   	
	   	if(audit_record_optional.isPresent()) {
	   		PageAuditRecord audit_record = audit_record_optional.get();
	   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
	   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getId());
	   		
	    	log.warn("processing audits for page quick audit :: "+audits.size());
	    	//Map audits to page states
	    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits);
	    	Set<IssueElementMap> issue_element_map = audit_service.generateIssueElementMap(audits);
	    	AuditScore score = AuditUtils.extractAuditScore(audits);
	    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
		   	
	   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issue_element_map, element_issue_map, score, page_src);
	   		
	   		SimplePage simple_page = new SimplePage(
		   									page_state.getUrl(), 
		   									page_state.getViewportScreenshotUrl(), 
		   									page_state.getFullPageScreenshotUrl(), 
		   									page_state.getFullPageWidth(), 
		   									page_state.getFullPageHeight(),
		   									page_state.getSrc(),
		   									page_state.getKey(), 
		   									page_state.getId());
		   	
	   		return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page, audit_record.getId());
	   	}
	   	
	   	PageState page_state = browser_service.buildPageState(sanitized_url);
	   	page_state = page_service.save(page_state);
		//domain_service.addPage(domain.getId(), page_state.getKey());

	   	//create new audit record
	   	AuditRecord audit_record = new PageAuditRecord(ExecutionStatus.IN_PROGRESS, new HashSet<>(), page_state);

	   	audit_record = audit_record_service.save(audit_record);
	   	//domain_service.addAuditRecord(domain.getId(), audit_record.getKey());
	   	Principal principal = request.getUserPrincipal();
		if(principal != null) {
			String user_id = principal.getName();
	    	Account account = account_service.findByUserId(user_id);
	    	account_service.addAuditRecord(account.getEmail(), audit_record.getId());
		}
	   	/*
	   	log.warn("telling audit manager about crawl action");
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, domain, "temp-account", audit_record, true, sanitized_url);
		audit_manager.tell(crawl_action, null);
	   	*/
	   	Set<Audit> audits = new HashSet<>();
	   	
	   	//check if page state already
	   	//perform audit and return audit result
	   	log.warn("?????????????????????????????????????????????????????????????????????");
	   	log.warn("?????????????????????????????????????????????????????????????????????");
	   	log.warn("?????????????????????????????????????????????????????????????????????");
	   	
	   	log.warn("requesting performance audit from performance auditor....");
	   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
	   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
	   	performance_insight_actor.tell(page_state, ActorRef.noSender());

	   	for(AuditCategory audit_category : AuditCategory.values()) {
   			List<Audit> rendered_audits_executed = audit_factory.executePageAudits(audit_category, page_state);

   			rendered_audits_executed = audit_service.saveAll(rendered_audits_executed);

   			audits.addAll(rendered_audits_executed);
   		}
	   	
	   	for(Audit audit : audits){
			audit = audit_service.save(audit);
			audit_record_service.addAudit( audit_record.getKey(), audit.getKey() );
			((PageAuditRecord)audit_record).addAudit(audit);
			//send pusher message to clients currently subscribed to domain audit channel
			//MessageBroadcaster.broadcastAudit(domain.getHost(), audit);
		}		//crawl site and retrieve all page urls/landable pages
	    //Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);
	   	audit_record.setStatus(ExecutionStatus.COMPLETE);
	   	audit_record.setEndTime(LocalDateTime.now());
	   	audit_record_service.save(audit_record);
	   	SimplePage simple_page = new SimplePage(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), page_state.getFullPageWidth(), page_state.getFullPageHeight(), null, null, page_state.getId());
	   	
	   	//Map audits to page states
    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits);
    	Set<IssueElementMap> issue_element_map = audit_service.generateIssueElementMap(audits);
    	AuditScore score = AuditUtils.extractAuditScore(audits);
    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
	   	
   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issue_element_map, element_issue_map, score, page_src);
   		
	   	return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page, audit_record.getId());
	}

	
	@RequestMapping("/stop")
	public @ResponseBody void stopAudit(HttpServletRequest request, 
				@RequestParam(value="url", required=true) String url)
			throws MalformedURLException, UnknownAccountException 
	{
	   	Principal principal = request.getUserPrincipal();
	   	String id = principal.getName().replace("auth0|", "");
	   	Account acct = account_service.findByUserId(id);
	
	   	if(acct == null){
	   		throw new UnknownAccountException();
	   	}
	   	else if(acct.getSubscriptionToken() == null){
	   		throw new MissingSubscriptionException();
	   	}
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class PageNotFoundError extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public PageNotFoundError() {
		super("Oh no! We couldn't find the page you want to audit.");
	}
}

