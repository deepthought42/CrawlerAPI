package com.minion.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.CrawlStats;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.services.AccountService;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping("/audits")
public class AuditController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;
	
    @Autowired
    private DomainService domain_service;
    
    @Autowired
    protected WebSecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    private ActorSystem actor_system;
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @PreAuthorize("hasAuthority('read:audits')")
    @RequestMapping(method = RequestMethod.GET, path="/{audit_key}/insights")
    public AuditRecord getMostRecentAudit(HttpServletRequest request,
			@PathVariable(value="audit_key", required=true) String audit_key
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        logger.info("finding all page insights :: "+audit_key);
        return null; //page_service.findLatestInsight(audit_key);
    }
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody List<Audit> getAudits(HttpServletRequest request) {
        return audit_service.findAll();
    }
    
    
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody CrawlStats startAudit(HttpServletRequest request,
											   	  		@RequestParam(value="url", required=true) String url,
											   	  		@RequestParam(value="audits", required=true) List<String> audit_types) throws Exception {
	   	/*
			Principal principal = request.getUserPrincipal();
		   	String id = principal.getName().replace("auth0|", "");
		   	Account acct = account_service.findByUserId(id);
		
		   	if(acct == null){
		   		throw new UnknownAccountException();
		   	}
	   	 */
		
		LocalDateTime start_time = LocalDateTime.now();
		
	   	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(url));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain();
	   		domain.setHost(sanitized_url.getHost());
	   		domain.setUrl(sanitized_url.toString());
	   		domain = domain_service.save(domain);
	   	}
	   
	   	List<AuditCategory> audit_categories = new ArrayList<AuditCategory>();
	   	for(String type : audit_types) {
	   		audit_categories.add(AuditCategory.create(type));
	   	}
	   	//crawl site and retrieve all page urls/landable pages
	   	Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);
		LocalDateTime end_time = LocalDateTime.now();

		long total_seconds = (end_time.toEpochSecond(ZoneOffset.UTC)-start_time.toEpochSecond(ZoneOffset.UTC));
	   	CrawlStats stats = new CrawlStats(start_time, end_time, total_seconds, page_state_audits.size(), total_seconds/page_state_audits.size());
	   	/*
	   	List<AuditRecord> audit_records = new ArrayList<AuditRecord>();
	   	//generate audit report
	   	List<Audit> audits = new ArrayList<>();
   		for(PageState page_state : page_states) {
   			for(String audit_type : audit_types) {
	   			//perform audit and return audit result
	   			List<Audit> audits_executed = AuditFactory.execute(AuditCategory.create(audit_type), page_state, "Look-See-admin");
	   			
	   			audits_executed = audit_service.saveAll(audits_executed);
	   			audits.addAll(audits_executed);
	   		}
	   		
   			AuditRecord audit_record = new AuditRecord(audits);
   			audit_records.add(audit_record_service.save(audit_record));
   			
	   		//use audit results to generate report
	   		//add report to audit reports list
	   		//save report to DB
	   	}
	   	*/
	   	
	   	//package reports into pdf document as full report
	   	//return full report to user
		
		/*
	   	AuditActionMessage audit_action_msg = new AuditActionMessage(DiscoveryAction.START, domain, acct.getUserId());
	   	actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("auditActor"), "audit_actor"+UUID.randomUUID()).tell(audit_action_msg, null);
	   	*/
	   	return stats;
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