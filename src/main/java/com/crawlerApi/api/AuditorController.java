package com.crawlerApi.api;

import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.crawlerApi.service.Auth0Service;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.exceptions.UnknownAccountException;
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
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API for interacting with audit functionality
 */
@Controller
@RequestMapping(path = "v1/auditor", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auditor V1", description = "Auditor API")
public class AuditorController extends BaseApiController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final Auth0Service auth0Service;
	private final com.crawlerApi.service.AuditService auditService;
	private final PageStateService pageService;
	
	@Autowired
	public AuditorController(
			Auth0Service auth0Service,
			com.crawlerApi.service.AuditService auditService,
			PageStateService pageService) {
		this.auth0Service = auth0Service;
		this.auditService = auditService;
		this.pageService = pageService;
	}
	
	/**
	 * Starts an audit on the provided URL based on the audit level.
	 * If it is a Page audit, only a single page is audited. 
	 * If it is a domain audit, then the entire domain is crawled and audited.
	 * 
	 * @param request HTTP request containing the authenticated user
	 * @param auditRequest The audit request containing URL and level
	 * @return A new audit record DTO
	 * @throws UnknownAccountException if the user account cannot be found
	 * @throws AuditCreationException if the audit cannot be created
	 */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	@Operation(summary = "Start audit", description = "Start an audit on the provided URL based on the audit level")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully started audit", content = @Content(schema = @Schema(type = "object", implementation = AuditRecordDto.class))),
		@ApiResponse(responseCode = "400", description = "Invalid audit request"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public @ResponseBody ResponseEntity<AuditRecordDto> startAudit(
			HttpServletRequest request,
			@RequestBody(required=true) AuditStartRequest auditRequest
	) throws UnknownAccountException {
		
		log.info("Received audit start request for URL: {} with level: {}",
				auditRequest.getUrl(), auditRequest.getLevel());
		// Get the authenticated user's account
		Principal principal = request.getUserPrincipal();
		Account account = accountService.findByUserId(principal.getName());

    	String lowercase_url = auditRequest.getUrl().toLowerCase();
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url ));
		JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    	
	   	//create new audit record
		if(AuditLevel.PAGE.equals(auditRequest.getLevel())){
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
			audit_record = (PageAuditRecord)auditRecordService.save(audit_record, null, null);
			accountService.addAuditRecord(account.getId(), audit_record.getId());
			log.warn("Initiating single page audit = "+sanitized_url);

			AuditStartMessage audit_start_msg = new AuditStartMessage(sanitized_url.toString(),
																		BrowserType.CHROME,
																		audit_record.getId(),
																		AuditLevel.PAGE,
																		account.getId());

			String url_msg_str = mapper.writeValueAsString(audit_start_msg);
			urlTopic.publish(url_msg_str);

			return auditRecordService.buildAudit(audit_record);
		}
		else if(AuditLevel.DOMAIN.equals(audit_start.getLevel())){
			Domain domain = domainService.createDomain(sanitized_url, account.getId());
			
			// create new audit record
			Set<AuditName> audit_list = getAuditList();
			AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.RUNNING, audit_list);
			audit_record.setUrl(domain.getUrl());
			audit_record = auditRecordService.save(audit_record, account.getId(), domain.getId());
			
			domainService.addAuditRecord(domain.getId(), audit_record.getKey());
			accountService.addAuditRecord(account.getId(), audit_record.getId());

			AuditStartMessage audit_start_msg = new AuditStartMessage(sanitized_url.toString(),
																	BrowserType.CHROME,
																	audit_record.getId(),
																	AuditLevel.DOMAIN,
																	account.getId());

			String url_msg_str = mapper.writeValueAsString(audit_start_msg);
			urlTopic.publish(url_msg_str);
			return auditRecordService.buildAudit(audit_record);
		}
		

		throw new AuditCreationException("Unsupported audit level: " + auditRequest.getLevel());
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
    	PageState page = pageService.findByUrl(url);
    	
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
     * Retrieves performance insights for a given page
     * 
     * @param request HTTP request containing the authenticated user
     * @param pageKey The key of the page to get insights for
     * @return Performance insights for the page
     * @throws UnknownAccountException if the user account cannot be found
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/{page_key}/insights")
	@Operation(summary = "Get page insights", description = "Retrieve performance insights for the given page")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved page insights", content = @Content(schema = @Schema(type = "object", implementation = PerformanceInsight.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
    public ResponseEntity<PerformanceInsight> getInsights(
    		HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String pageKey
	) throws UnknownAccountException {
    	
    	// Verify the user is authenticated
    	Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);

    	if (accountOpt.isEmpty()) {
    		log.warn("Unknown account for principal: {}", principal.getName());
    		throw new UnknownAccountException();
    	}
    	
    	log.info("Finding page insights for page key: {}", pageKey);
    	
    	// TODO: Implement insights retrieval
    	// PerformanceInsight insights = pageService.getAuditInsights(pageKey);
    	
    	return ResponseEntity.notFound().build(); // Temporary until implementation
    }
}