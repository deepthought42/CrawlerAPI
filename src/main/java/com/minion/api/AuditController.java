package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.minion.api.exception.PaymentDueException;
import com.minion.browsing.Crawler;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditFactory;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.message.AuditActionMessage;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
import com.qanairy.utils.BrowserUtils;
import com.stripe.exception.StripeException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@RestController
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
    
    
	@RequestMapping(path="/start", method = RequestMethod.GET)
	public @ResponseBody List<AuditRecord> startAudit(HttpServletRequest request,
											   	  		@RequestParam(value="url", required=true) String url,
											   	  		@RequestParam(value="audits", required=true) List<String> audit_types) throws IOException, UnknownAccountException {
	   	/*
			Principal principal = request.getUserPrincipal();
		   	String id = principal.getName().replace("auth0|", "");
		   	Account acct = account_service.findByUserId(id);
		
		   	if(acct == null){
		   		throw new UnknownAccountException();
		   	}
	   	 */
	   	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(url));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost(), "Look-See-admin");
	   	
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain();
	   		domain.setHost(sanitized_url.getHost());
	   		domain = domain_service.save(domain);
	   	}
	   
	   	//crawl site and retrieve all page urls/landable pages
	   	Collection<PageState> page_states = (new Crawler()).crawlLite(domain, "Look-See-admin");
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
	   	
	   	//package reports into pdf document as full report
	   	//return full report to user
		
		/*
	   	AuditActionMessage audit_action_msg = new AuditActionMessage(DiscoveryAction.START, domain, acct.getUserId());
	   	actor_system.actorOf(SpringExtProvider.get(actor_system)
				  .props("auditActor"), "audit_actor"+UUID.randomUUID()).tell(audit_action_msg, null);
	   	*/
	   	return audit_records;
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