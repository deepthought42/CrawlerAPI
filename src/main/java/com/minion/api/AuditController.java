package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.websocket.server.PathParam;

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
import com.qanairy.models.CrawlStats;
import com.qanairy.models.Domain;
import com.qanairy.models.PageVersion;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping("/audits")
public class AuditController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

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
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Set<AuditRecord> getAuditRecords(HttpServletRequest request,
    											@RequestParam(value="domain_host", required=true) String domain_host
	) {
    	log.warn("finding all recent audits for url :: "+domain_host);
        
    	Domain domain = domain_service.findByHost(domain_host);
    	return domain_service.getAuditRecords(domain.getKey());
    }

    /**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/{id}")
    public @ResponseBody Set<Audit> getAudits(HttpServletRequest request,
    											@PathVariable("id") @NotBlank long id
	) {
    	log.warn("finding element with ID  :: "+id);
        //AuditRecord record = audit_record_service.findById(id).get();
        //return audit_record_service.getAllAudits(record.getKey());
    	Set<Audit> audit_set = new HashSet<Audit>();
    	audit_set.add(audit_service.findById(id).get());
    	
        return audit_set;
    }
    

    /**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/color")
    public @ResponseBody Set<Audit> getColorManagementAudits(HttpServletRequest request,
    											@PathParam("domain") @NotBlank String domain
	) {
    	log.warn("finding element with ID  :: "+domain);
        return audit_record_service.getAllColorManagementAudits(domain);
    }
    
    /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody CrawlStats startAudit(HttpServletRequest request,
											   @RequestBody(required=true) PageVersion page) throws Exception {

	   	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+page.getUrl()));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		log.warn("saving domain");
	   		domain = new Domain(sanitized_url.getProtocol(), sanitized_url.getHost(), sanitized_url.getPath(), "chrome", "");
	   		domain = domain_service.save(domain);
	   	}

	   	log.warn("creating audit record");
	   	//create new audit record
	   	//AuditRecord audit_record = new AuditRecord();
	   	
	   	//log.warn("Saving audit Record");
	   	//audit_record = audit_record_service.save(audit_record);
	   	
	   	//log.warn("Adding audit record to domain");
	   	//domain_service.addAuditRecord(domain.getKey(), audit_record.getKey());
	   	
	   	log.warn("telling audit manager about crawl action");
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START_LINK_ONLY, domain, "temp-account", null);
		audit_manager.tell(crawl_action, null);
	   	//crawl site and retrieve all page urls/landable pages
	   /*	
	    Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);
		LocalDateTime end_time = LocalDateTime.now();

		long total_seconds = (end_time.toEpochSecond(ZoneOffset.UTC)-start_time.toEpochSecond(ZoneOffset.UTC));
	   	return new CrawlStats(start_time, end_time, total_seconds, page_state_audits.size(), total_seconds/((double)page_state_audits.size()));
	   	*/
		return null;
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