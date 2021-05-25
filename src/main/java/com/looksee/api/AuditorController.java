package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.net.URL;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.browsing.Crawler;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditFactory;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.ElementIssueMap;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.IssueElementMap;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.PageAudits;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/auditor")
public class AuditorController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_service;
    
    @Autowired
    private ActorSystem actor_system;
    
    @Autowired
	private AuditFactory audit_factory;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected AuditService audit_service;
    
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
			@RequestBody(required=true) PageState page
	) throws Exception {    	
    	String lowercase_url = page.getUrl().toLowerCase();

    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));
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
    	/*
   		String page_url = sanitized_url.getHost()+sanitized_url.getPath();
	   	Optional<PageAuditRecord> audit_record_optional = audit_record_service.getMostRecentPageAuditRecord(page_url);
	   	
	   	if(audit_record_optional.isPresent()) {
	   		PageAuditRecord audit_record = audit_record_optional.get();
	   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
	   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getId());
		   	SimplePage simple_page = new SimplePage(
		   									page_state.getUrl(), 
		   									page_state.getViewportScreenshotUrl(), 
		   									page_state.getFullPageScreenshotUrl(), 
		   									page_state.getFullPageWidth(), 
		   									page_state.getFullPageHeight(),
		   									page_state.getSrc(),
		   									page_state.getKey(), 
		   									page_state.getId());
		   	
			//Map audits to page states
	    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits);
	    	Set<UXIssueMessage> issues = audit_record_service.getIssues(audit_record.getId());
	    	AuditScore score = AuditUtils.extractAuditScore(audits);
	    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
		   	
	   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issues, element_issue_map, score, page_src);
	   		
	   		return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page);
	   	}
	   	*/
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
	   	
		//Map audits to page states
    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits);
    	Set<IssueElementMap> issue_element_map = audit_service.generateIssueElementMap(audits);

    	AuditScore score = AuditUtils.extractAuditScore(audits);
    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
	   	
   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issue_element_map, element_issue_map, score, page_src);
   		
	   	SimplePage simple_page = new SimplePage(page_state.getUrl(), 
	   											page_state.getViewportScreenshotUrl(), 
	   											page_state.getFullPageScreenshotUrl(), 
	   											page_state.getFullPageWidth(), 
	   											page_state.getFullPageHeight(), 
	   											page_state.getSrc(), 
	   											page_state.getKey(), 
	   											page_state.getId());	   	
	   	//if request is from www.look-see.com then return redirect response
	   	
   		return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page);
	}
	
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public SimplePage getPage(HttpServletRequest request,
			@RequestParam(value="url", required=true) String url
	)  {
    	/*
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	*/
    	PageState page = page_service.findByUrl(url);
    	
        log.info("finding page :: "+page.getKey());
        
        SimplePage simple_page = new SimplePage(
        								page.getUrl(), 
        								page.getViewportScreenshotUrl(), 
        								page.getFullPageScreenshotUrl(), 
        								page.getFullPageWidth(), 
        								page.getFullPageHeight(),
        								page.getSrc(),
        								page.getKey(), 
        								page.getId());	
        return simple_page;
    }
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/$page_key/insights")
    public PerformanceInsight getInsights(HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String page_key
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        log.info("finding all page insights :: "+page_key);
        return null; //page_service.getAuditInsights(page_state_key);
    }
    
}