package com.crawlerApi.api;

import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.browsing.Crawler;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.gcp.PubSubUrlMessagePublisherImpl;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditStartMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

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
	private PageStateService page_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected AuditService audit_service;
	
	@Autowired 
	private DomainService domain_service;

	@Autowired
	private PubSubUrlMessagePublisherImpl url_topic;
	
	/**
	 * Starts an audit on the provided URL based on the {@AuditLevel level}. If
	 * it is a Page audit, only a single page is audited. If it is a domain audit,
	 * then the entire domain is crawled and audited.
	 * 
	 * @param request
	 * @param audit_start
	 * @return	A new {@link AuditRecord audit record}
	 * 
	 * @throws Exception
	 */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	public @ResponseBody AuditRecordDto startAudit(
			HttpServletRequest request,
			@RequestBody(required=true) AuditRecord audit_start
	) throws Exception {
		Principal principal = request.getUserPrincipal();
		log.info("Looking up user: " + principal.getName());
		Account account = account_service.findByUserId(principal.getName());
		log.info("Found account: " + account.getId());

    	String lowercase_url = audit_start.getUrl().toLowerCase();
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));
		log.info("Sanitized URL: " + sanitized_url);
		JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    	
	   	//create new audit record
		if(AuditLevel.PAGE.equals(audit_start.getLevel())){
			log.warn("creating new page audit record...");
			PageAuditRecord audit_record = new PageAuditRecord(ExecutionStatus.IN_PROGRESS,
																new HashSet<>(),
																null,
																false,
																new HashSet<>());
			audit_record.setUrl(sanitized_url.toString());
			audit_record.setDataExtractionProgress(1.0/50.0);
			audit_record.setAestheticScore(0.0);
			audit_record.setAestheticAuditProgress(0.0);
			audit_record.setContentAuditScore(0.0);
			audit_record.setContentAuditProgress(0.0);
			audit_record.setInfoArchScore(0.0);
			audit_record.setInfoArchitectureAuditProgress(0.0);
			audit_record = (PageAuditRecord)audit_record_service.save(audit_record, null, null);
			account_service.addAuditRecord(account.getId(), audit_record.getId());
			log.warn("Initiating single page audit = "+sanitized_url);

			AuditStartMessage audit_start_msg = new AuditStartMessage(sanitized_url.toString(),
																		BrowserType.CHROME,
																		audit_record.getId(),
																		AuditLevel.PAGE,
																		account.getId());

			String url_msg_str = mapper.writeValueAsString(audit_start_msg);
			url_topic.publish(url_msg_str);

			return audit_record_service.buildAudit(audit_record);
		}
		else if(AuditLevel.DOMAIN.equals(audit_start.getLevel())){
			Domain domain = domain_service.createDomain(sanitized_url, account.getId());
			
			// create new audit record
			Set<AuditName> audit_list = getAuditList();
			AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.RUNNING, audit_list);
			audit_record.setUrl(domain.getUrl());
			audit_record = audit_record_service.save(audit_record, account.getId(), domain.getId());
			
			domain_service.addAuditRecord(domain.getId(), audit_record.getKey());
			account_service.addAuditRecord(account.getId(), audit_record.getId());

			AuditStartMessage audit_start_msg = new AuditStartMessage(sanitized_url.toString(),
																	BrowserType.CHROME,
																	audit_record.getId(),
																	AuditLevel.DOMAIN,
																	account.getId());

			String url_msg_str = mapper.writeValueAsString(audit_start_msg);
			url_topic.publish(url_msg_str);
			return audit_record_service.buildAudit(audit_record);
		}
		

		throw new AuditRunException();
	}
    
    private Set<AuditName> getAuditList() {
		Set<AuditName> audit_list = new HashSet<>();
		//VISUAL DESIGN AUDIT
		audit_list.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		audit_list.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		
		//INFO ARCHITECTURE AUDITS
		audit_list.add(AuditName.LINKS);
		audit_list.add(AuditName.TITLES);
		audit_list.add(AuditName.ENCRYPTED);
		audit_list.add(AuditName.METADATA);
		
		//CONTENT AUDIT
		audit_list.add(AuditName.ALT_TEXT);
		audit_list.add(AuditName.READING_COMPLEXITY);
		audit_list.add(AuditName.PARAGRAPHING);
		audit_list.add(AuditName.IMAGE_COPYRIGHT);
		audit_list.add(AuditName.IMAGE_POLICY);

		return audit_list;
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
    	PageState page = page_service.findByUrl(url);
    	
        log.info("finding page :: "+page.getKey());
        
        SimplePage simple_page = new SimplePage(
										page.getUrl(),
										page.getViewportScreenshotUrl(),
										page.getFullPageScreenshotUrl(),
										page.getFullPageWidth(),
										page.getFullPageHeight(),
										page.getSrc(),
										page.getKey(), page.getId());
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
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        log.info("finding all page insights :: "+page_key);
        return null; //page_service.getAuditInsights(page_state_key);
    }
    
}


@ResponseStatus(HttpStatus.SEE_OTHER)
class AccountLimitExceededException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8549092797919036363L;

	public AccountLimitExceededException() {
		super("You have exceeded the number of single page audits that are available for your plan. Upgrade to get access to more audits.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class AuditRunException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8549092797919036363L;

	public AuditRunException() {
		super("There was an unexpected error while starting the audit. Please try again.");
	}
}