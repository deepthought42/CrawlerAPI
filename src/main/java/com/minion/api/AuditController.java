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
import com.qanairy.models.AuditStats;
import com.qanairy.models.CrawlStat;
import com.qanairy.models.Domain;
import com.qanairy.models.PageVersion;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditStage;
import com.qanairy.models.enums.CrawlAction;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.message.CrawlActionMessage;
import com.qanairy.models.message.DomainAuditMessage;
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
    public @ResponseBody Set<Audit> getAudits(HttpServletRequest request,
    											@RequestParam(value="domain_host", required=true) String domain_host
	) {
    	log.warn("finding all recent audits for url :: "+domain_host);
        
    	Domain domain = domain_service.findByHost(domain_host);
    	AuditRecord audit_record = domain_service.getMostRecentAuditRecord(domain.getHost());
    	return audit_record_service.getAllAudits(audit_record.getKey());
    }

    /**
     * Retrieves {@link Audit audit} with given ID
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/{id}")
    public @ResponseBody Set<Audit> getAudit(HttpServletRequest request,
    											@PathVariable("id") @NotBlank long id
	) {
    	log.warn("finding element with ID  :: "+id);

    	Set<Audit> audit_set = new HashSet<Audit>();
    	
    	Audit audit = audit_service.findById(id).get();
    	audit.setObservations( audit_service.getObservations(audit.getKey()) );
        
    	audit_set.add(audit);
    	
        return audit_set;
    }

    /**
     * Retrieves set of {@link Audit audits} that have a type of 'color management'
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/color")
    public @ResponseBody Set<Audit> getColorManagementAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding element with ID  :: "+host);
        return audit_record_service.getAllColorManagementAudits(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of 'color management'
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/color/palette")
    public @ResponseBody Set<Audit> getColorManagementPaletteAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding element with ID  :: "+host);
    	//get all pages
    	return domain_service.getMostRecentAuditRecordColorPaletteAudits(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of 'color management'
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/color/textcontrast")
    public @ResponseBody Set<Audit> getColorManagementTextContrastAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding audits for host  :: "+host);
    	//get all pages
    	return domain_service.getMostRecentAuditRecordTextColorContrast(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of 'color management'
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/color/nontextcontrast")
    public @ResponseBody Set<Audit> getColorManagementNonTextContrastAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding audits for host  :: "+host);
    	//get all pages
    	return domain_service.getMostRecentAuditRecordNonTextColorContrast(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of typography
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/typography")
    public @ResponseBody Set<Audit> getTypographyAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return audit_record_service.getAllTypographyAudits(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of typoface
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/typography/typeface")
    public @ResponseBody Set<Audit> getTypofaceAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return domain_service.getMostRecentAuditRecordTypeface(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of information architecture
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/information_architecture")
    public @ResponseBody Set<Audit> getInformationArchitectureAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return audit_record_service.getAllInformationArchitectureAudits(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of information architecture
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/information_architecture/link")
    public @ResponseBody Set<Audit> getInformationArchitectureLinkAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return domain_service.getMostRecentAuditRecordLinks(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of information architecture
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/information_architecture/title")
    public @ResponseBody Set<Audit> getInformationArchitectureTitleAndHeaderAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return domain_service.getMostRecentAuditRecordTitleAndHeader(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of information architecture
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/information_architecture/margin")
    public @ResponseBody Set<Audit> getInformationArchitectureMarginAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return domain_service.getMostRecentAuditRecordMargins(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of information architecture
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/information_architecture/padding")
    public @ResponseBody Set<Audit> getInformationArchitecturePaddingAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding branding audits for domain with host  :: "+host);
        return domain_service.getMostRecentAuditRecordPadding(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/visuals")
    public @ResponseBody Set<Audit> getVisualAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding visual audits for domain with host  :: "+host);
        return audit_record_service.getAllVisualAudits(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/visuals/alttext")
    public @ResponseBody Set<Audit> getVisualAltTextAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding visual audits for domain with host  :: "+host);
    	return domain_service.getMostRecentAuditRecordAltText(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/written_content/paragraphs")
    public @ResponseBody Set<Audit> getWrittenContentParagraphAudits(HttpServletRequest request,
    											@PathParam("host") @NotBlank String host
	) {
    	log.warn("finding visual audits for domain with host  :: "+host);
    	return domain_service.getMostRecentAuditRecordParagraphing(host);
    }
    
    /**
     * Retrieves set of {@link Audit audits} that have a type of visuals
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(method= RequestMethod.GET, path="/visuals/paragraphs")
    public @ResponseBody Set<Audit> getAudits(
    		HttpServletRequest request,
    		@PathVariable("host") @NotBlank String host,
    		@PathVariable("category") @NotBlank String category
	) {
    	log.warn("finding visual audits for domain with host  :: "+host);
    	return domain_service.getMostRecentAuditRecord(host, AuditCategory.create(category));
    }
    
    /**
     * Adds recommendation to @link Audit audit}
     * 
     * @param key key for audit that recommendation should be added to
     * @param recommendation the expert opinion that should be added to the audit
     * 
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(path="{key}/recommendations/add", method = RequestMethod.POST)
    public @ResponseBody Audit addRecommendation(
    		HttpServletRequest request,
    		final @PathVariable String key,
    		final @RequestBody(required=true) String recommendation
	) {
    	//find audit by key and add recommendation
    	Audit audit = audit_service.findByKey(key);
       	audit.addRecommendation(recommendation);
       	
       	//save and return
       	return audit_service.save(audit);    	
    }
    
    /**
     * Adds recommendation to @link Audit audit}
     * 
     * @param key key for audit that recommendation should be added to
     * @param recommendation the expert opinion that should be added to the audit
     * 
     * @return {@link Audit audit} with given ID
     */
    @RequestMapping(path="{key}/recommendations", method = RequestMethod.DELETE)
    public @ResponseBody Audit deleteRecommendation(
    		HttpServletRequest request,
    		final @PathVariable String key,
    		@RequestParam(required=true) String recommendation
	) {
    	//find audit by key and add recommendation
    	Audit audit = audit_service.findByKey(key);
       	audit.removeRecommendation(recommendation);
       	
       	//save and return
       	return audit_service.save(audit);    	
    }
    
    /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody AuditStats startAudit(
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
	   		log.warn("saving domain");
	   		domain = new Domain(sanitized_url.getProtocol(), sanitized_url.getHost(), sanitized_url.getPath(), "");
	   		domain = domain_service.save(domain);
	   	}

	   	log.warn("creating audit record");
	   	//create new audit record
	   	AuditRecord audit_record = new AuditRecord(new AuditStats(domain.getHost()));

	   	log.warn("Saving audit Record");
	   	audit_record = audit_record_service.save(audit_record);
	   	
	   	log.warn("Adding audit record to domain");
	   	domain_service.addAuditRecord(domain.getKey(), audit_record.getKey());
	   	
	   	log.warn("telling audit manager about crawl action");
	   	ActorRef audit_manager = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditManager"), "auditManager"+UUID.randomUUID());
		CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START_LINK_ONLY, domain, "temp-account", audit_record);
		audit_manager.tell(crawl_action, null);
	   	//crawl site and retrieve all page urls/landable pages
	    //Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);

	   	return audit_record.getAuditStats();
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
	
	 /**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/buildDomainAudits", method = RequestMethod.GET)
	public @ResponseBody CrawlStat performDomainAudit(HttpServletRequest request,
													@PathParam("host") @NotBlank String host) throws Exception {

	   	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl("http://"+host));
	   	Domain domain = domain_service.findByHost(sanitized_url.getHost());
	   	log.warn("host :: "+sanitized_url.getHost());
	   	log.warn("domain :: "+domain);
	   	DomainAuditMessage domain_msg = new DomainAuditMessage( domain, AuditStage.RENDERED);
		log.warn("Audit Manager is now ready to perform a domain audit");
		//AuditSet audit_record_set = new AuditSet(audits);
		ActorRef auditor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("auditor"), "auditor"+UUID.randomUUID());
		auditor.tell(domain_msg, null);
	   	//crawl site and retrieve all page urls/landable pages
	   /*	
	    Map<String, Page> page_state_audits = crawler.crawlAndExtractData(domain);
		LocalDateTime end_time = LocalDateTime.now();

		long total_seconds = (end_time.toEpochSecond(ZoneOffset.UTC)-start_time.toEpochSecond(ZoneOffset.UTC));
	   	return new CrawlStats(start_time, end_time, total_seconds, page_state_audits.size(), total_seconds/((double)page_state_audits.size()));
	   	*/
		return null;
	}
}