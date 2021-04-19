package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.SimplePage;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditFactory;
import com.qanairy.models.audit.IssueElementMap;
import com.qanairy.models.audit.PageAuditRecord;
import com.qanairy.models.audit.PageAudits;
import com.qanairy.models.audit.UXIssueMessage;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.audit.DomainAuditRecord;
import com.qanairy.models.audit.ElementIssueMap;
import com.qanairy.models.audit.ElementIssueTwoWayMapping;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.enums.ExecutionStatus;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageVersionService;
import com.qanairy.services.UXIssueMessageService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping("/audits")
public class AuditController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

   	public final static long SECS_PER_HOUR = 60 * 60;

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageVersionService page_service;
	
	@Autowired
	private AccountService account_service;
	
    @Autowired
    private DomainService domain_service;
    
    @Autowired
    protected WebSecurityConfig appConfig;
    
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
        domain_host = domain_host.replace("/", "").trim();
    	Domain domain = domain_service.findByHost(domain_host);
    	Optional<DomainAuditRecord> audit_record = domain_service.getMostRecentAuditRecord(domain.getHost()); 
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
    	audit.setMessages( audit_service.getIssues(audit.getKey()) );
        
    	audit_set.add(audit);
    	
        return audit_set;
    }
    
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    @RequestMapping(method= RequestMethod.GET, path="/pages")
    public @ResponseBody Set<PageAudits> getAuditsByPage(
    		HttpServletRequest request,
    		@RequestParam("url") String url
	) throws MalformedURLException {
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(url));
    	//Get most recent audits
    	Set<PageAuditRecord> audit_records = domain_service.getMostRecentPageAuditRecords(sanitized_url.getHost());

    	Set<PageAudits> page_audit_set = new HashSet<>();
	   	
	   	for(PageAuditRecord audit_record : audit_records) {
	   		//PageState page_state = page_state_service.findByUrl(page_url);
	   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getKey());
	   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getKey());

		   	SimplePage simple_page = new SimplePage(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), page_state.getFullPageWidth(), page_state.getFullPageHeight());
	   		PageAudits page_audits = new PageAudits( audit_record.getStatus(), audits, simple_page);
	   		page_audit_set.add(page_audits);
	   	}
    	
    	return page_audit_set;
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    @RequestMapping(method= RequestMethod.GET, path="/elements")
    public @ResponseBody ElementIssueTwoWayMapping getPageAuditElements(
    		HttpServletRequest request,
    		@RequestParam("page_url") String page_url
	) throws MalformedURLException {
    	URL url = new URL(BrowserUtils.sanitizeUrl(page_url));
    	String page_url_without_protocol = url.getHost() + url.getPath();
    	//Get most recent audits
    	Optional<PageAuditRecord> page_audit = audit_record_service.getMostRecentPageAuditRecord(page_url_without_protocol);
    	
    	Set<Audit> audits = new HashSet<>();
    	if( page_audit.isPresent() ) {
    		PageAuditRecord page_audit_record = page_audit.get();
    		audits = audit_record_service.getAllAuditsForPageAuditRecord(page_audit_record.getKey());    		
    	}
    	log.warn("processing audits :: "+audits.size());
    	//Map audits to page states
    	Set<IssueElementMap> issue_element_map = audit_service.generateIssueElementMap(audits, url);
    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits, url);
    
    	//package both elements into an object definition
    	return new ElementIssueTwoWayMapping(issue_element_map, element_issue_map);
    }
    
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
	@RequestMapping(path="/start-individual", method = RequestMethod.POST)
	public @ResponseBody PageAudits startSinglePageAudit(
			HttpServletRequest request,
			@RequestBody(required=true) PageVersion page
	) throws Exception {
    	String lowercase_url = page.getUrl().toLowerCase();

    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));
	   //	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+page.getUrl()));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain(sanitized_url.getProtocol(), sanitized_url.getHost(), sanitized_url.getPath(), "");
	   		domain = domain_service.save(domain);
	   	}

   		String page_url = sanitized_url.getHost()+sanitized_url.getPath();
	   	Optional<PageAuditRecord> audit_record_optional = audit_record_service.getMostRecentPageAuditRecord(page_url);
	   	
	   	if(audit_record_optional.isPresent()) {
	   		PageAuditRecord audit_record = audit_record_optional.get();
	   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getKey());
	   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getKey());
		   	SimplePage simple_page = new SimplePage(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), page_state.getFullPageWidth(), page_state.getFullPageHeight());
	   		PageAudits page_audits = new PageAudits( audit_record.getStatus(), audits, simple_page);
	   		page_audits.addAudits(audits);
	   		return page_audits;
	   	}
	   	
	   	//create new audit record
	   	AuditRecord audit_record = new PageAuditRecord();

	   	audit_record = audit_record_service.save(audit_record);
	   	
	   	domain_service.addAuditRecord(domain.getKey(), audit_record.getKey());
	   	
	   	Document doc = Jsoup.connect(sanitized_url.toString()).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get();
		page = browser_service.buildPage(doc.outerHtml(), sanitized_url.toString(), doc.title());
		page = page_service.save( page );

		//domain.addPage(page);
		domain_service.addPage(domain.getHost(), page.getKey());
	   	/*
	   	log.warn("telling audit manager about crawl action");
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, domain, "temp-account", audit_record, true, sanitized_url);
		audit_manager.tell(crawl_action, null);
	   	*/
	   	PageState page_state = browser_service.buildPageState(page);
		
	   	Set<Audit> audits = new HashSet<>();
	   	
	   	for(AuditCategory audit_category : AuditCategory.values()) {
	   		//check if page state already
   			//perform audit and return audit result
   			List<Audit> rendered_audits_executed = audit_factory.executePostRenderPageAudits(audit_category, page_state);

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
	   	SimplePage simple_page = new SimplePage(page_state.getUrl(), page_state.getViewportScreenshotUrl(), page_state.getFullPageScreenshotUrl(), page_state.getFullPageWidth(), page_state.getFullPageHeight());
	   	PageAudits page_audits = new PageAudits( audit_record.getStatus(), audits, simple_page);
	   	
	   	//if request is from www.look-see.com then return redirect response
	   	
   		return page_audits;
	}
	
    /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody AuditRecord startAudit(
			HttpServletRequest request,
			@RequestBody(required=true) PageVersion page
	) throws Exception {
    	String lowercase_url = page.getUrl().toLowerCase();
    	if(!lowercase_url.contains("http")) {
    		lowercase_url = "http://" + lowercase_url;
    	}
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));
	   //	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+page.getUrl()));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain(sanitized_url.getProtocol(), sanitized_url.getHost(), sanitized_url.getPath(), "");
	   		domain = domain_service.save(domain);
	   	}

	   	//create new audit record
	   	AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.IN_PROGRESS);

	   	audit_record = audit_record_service.save(audit_record);
	   	
	   	domain_service.addAuditRecord(domain.getKey(), audit_record.getKey());
	   	
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, domain, "temp-account", audit_record, false, sanitized_url);
		audit_manager.tell(crawl_action, null);
	   	//crawl site and retrieve all page urls/landable pages
	    //Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);

	   	return audit_record;
	}

	
	@RequestMapping("/stop")
	public @ResponseBody void stopAudit(HttpServletRequest request, @RequestParam(value="url", required=true) String url)
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