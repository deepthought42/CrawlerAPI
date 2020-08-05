package com.minion.api;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

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
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.AuditService;
import com.qanairy.services.DomainService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.BrowserUtils;

import akka.actor.ActorRef;
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
    private PageService page_service;
    
    @Autowired
    private PageStateService page_state_service;
    
    @Autowired
    protected WebSecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
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
    public @ResponseBody List<Audit> getAudits(HttpServletRequest request,
    											@RequestParam(value="domain_host", required=true) String domain_host
	) {
    	logger.warn("finding all recent audits for url :: "+domain_host);
        
        //look up domain by url
    	List<Page> pages = domain_service.getPages(domain_host);
        
        logger.warn("Pages found :: "+pages.size());
        //get most recent page states for pages
        List<PageState> recent_page_states = new ArrayList<>();
        for(Page page : pages) {
        	PageState page_state = page_service.getMostRecentPageState(page.getKey());
        	recent_page_states.add(page_state);
        }
        
        logger.warn("recent page states identified :: "+recent_page_states.size());
        
        List<Audit> recent_audits = new ArrayList<>();
        for(PageState page_state : recent_page_states) {
        	recent_audits.addAll(page_state_service.getAudits(page_state.getKey()));
        }
        logger.warn("recent audits :: " + recent_audits.size());
        
        return recent_audits;
    }

    /**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/{id}")
    public @ResponseBody Audit getAudits(HttpServletRequest request,
    											@PathVariable("id") @NotBlank long id
	) {
    	logger.warn("finding element with ID  :: "+id);
        
        return audit_service.findById(id).get();
    }
    
    
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody CrawlStats startAudit(HttpServletRequest request,
											   @RequestBody(required=true) Page page) throws Exception {
	   	/*
			Principal principal = request.getUserPrincipal();
		   	String id = principal.getName().replace("auth0|", "");
		   	Account acct = account_service.findByUserId(id);
		
		   	if(acct == null){
		   		throw new UnknownAccountException();
		   	}
	   	 */
		
	   	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+page.getUrl()));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	System.out.println("domain returned from db ...."+domain);
	   	//next 2 if statements are for conversion to primarily use url with path over host and track both in domains. 
	   	//Basically backwards compatibility. if they are still here after June 2020 then remove it
	   	if(domain == null) {
	   		domain = new Domain();
	   		domain.setProtocol(sanitized_url.getProtocol());
	   		domain.setHost(sanitized_url.getHost());
	   		domain.setEntryPath(sanitized_url.getPath());
	   		domain = domain_service.save(domain);
	   	}

	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START_LINK_ONLY, domain, "temp-account");
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