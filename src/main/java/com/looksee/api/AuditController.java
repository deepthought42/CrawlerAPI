package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.api.exception.MissingSubscriptionException;
import com.looksee.browsing.Crawler;
import com.looksee.models.Account;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.UXIssueReportDto;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditFactory;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.PageAudits;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.ObservationType;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.services.ReportService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    protected SecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected UXIssueMessageService issue_message_service;
    
    @Autowired
    protected PageStateService page_state_service;
    
    @Autowired
    protected ElementStateService element_state_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    private ActorSystem actor_system;
    
    @Autowired
	private AuditFactory audit_factory;
    
	@Autowired
	private UXIssueMessageService ux_issue_service;
    
    /**
     * Retrieves list of audits {@link Audit audits} from last 30 days
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws MalformedURLException 
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody Set<PageAuditRecord> getAudits(HttpServletRequest request) throws MalformedURLException, UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName();
    	Account acct = account_service.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}    	
    	
    	return account_service.findMostRecentPageAudits(acct.getId()); 
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
     * Performs a single page audit on a page with the given page_id
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
	  
   		String page_url = sanitized_url.getHost()+sanitized_url.getPath();
	   	Optional<PageAuditRecord> audit_record_optional = audit_record_service.getMostRecentPageAuditRecord(page_url);
	   	
	   	if(audit_record_optional.isPresent()) {
	   		PageAuditRecord audit_record = audit_record_optional.get();
	   		
	   		PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record.getId());
	   		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record.getId());
	   		
	    	log.warn("processing audits for page quick audit :: "+audits.size());
	    	//Map audits to page states
	    	//retrieve element set
	    	Collection<UXIssueMessage> issues = audit_service.retrieveUXIssues(audits);
	    	
	    	//retrieve issue set
	    	Collection<SimpleElement> elements = audit_service.retrieveElementSet(issues);

	    	//Map audits to page states
	    	Map<String, Set<String>> element_issue_map = audit_service.generateElementIssuesMap(audits);
	    	
	    	//generate IssueElementMap
	    	Map<String, String> issue_element_map = audit_service.generateIssueElementMap(audits);
	    	
	    	AuditScore score = AuditUtils.extractAuditScore(audits);
	    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
		   	
	   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issues,
	   																					 elements,
	   																					 issue_element_map, 
	   																					 element_issue_map, 
	   																					 score, 
	   																					 page_src);
	   		
	   		SimplePage simple_page = new SimplePage(
		   									page_state.getUrl(), 
		   									page_state.getViewportScreenshotUrl(), 
		   									page_state.getFullPageScreenshotUrlOnload(), 
		   									page_state.getFullPageScreenshotUrlComposite(), 
		   									page_state.getFullPageWidth(),
		   									page_state.getFullPageHeight(),
		   									page_state.getSrc(), 
		   									page_state.getKey(), page_state.getId());
		   	
	   		return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page, audit_record.getId());
	   	}
	   	
	   	AuditRecord audit_record = new PageAuditRecord(ExecutionStatus.IN_PROGRESS, new HashSet<>(), null, false);
   		long audit_record_id = audit_record.getId();
	   	audit_record_service.save(audit_record);
		

		//update audit record with progress
	   	PageState page_state = browser_service.buildPageState(sanitized_url);
	   	PageState page_state_record = page_service.save(page_state);
	   	
	   	audit_record = audit_record_service.findById(audit_record.getId()).get();
	   	audit_record.setDataExtractionProgress(1.0/3.0);
	   	audit_record = audit_record_service.save(audit_record);
	   	audit_record_service.addPageToAuditRecord(audit_record.getId(), page_state_record.getId());
	   	
	   	//generate unique xpaths for all elements on page
	   	List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state_record.getSrc());
		
		//update audit record with progress
		audit_record.setDataExtractionProgress(2.0/3.0);
		audit_record_service.save(audit_record);
	   	int start_xpath_index = 0;
	   	int last_xpath_index = 0;

		//List<CompletableFuture<List<ElementState>>> futures_list = new ArrayList<>();
		List<List<String>> xpath_lists = new ArrayList<>();
	   	while(start_xpath_index < (xpaths.size()-1)) {
	   		last_xpath_index = (start_xpath_index + 100);
	   		if(last_xpath_index >= xpaths.size()) {
	   			last_xpath_index = xpaths.size()-1;
	   		}
	   		List<String> xpath_subset = xpaths.subList(start_xpath_index, last_xpath_index);
	   		xpath_lists.add(xpath_subset);
	   		/*	
	   		ElementExtractionMessage element_extraction_msg = 
		   								new ElementExtractionMessage(page_state_record, 
		   															 audit_record, 
		   															 xpath_subset);
			log.warn("Running element extraction from page state");
			ActorRef element_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
		   		.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
			CompletableFuture<List<ElementState>> fut = browser_service.buildPageElementFuture(page_state, audit_record, xpath_subset);					
			futures_list.add(fut);
	   		 */
			start_xpath_index = last_xpath_index;
	   	}
		BufferedImage page_screenshot = ImageIO.read(new URL(page_state.getFullPageScreenshotUrlOnload()));

	   	//parallel stream get all elements since order doesn't matter
	   	xpath_lists.parallelStream().forEach(xpath_list -> {
	   		List<ElementState> elements = browser_service.buildPageElements(page_state, xpath_list, audit_record_id, sanitized_url, page_screenshot.getHeight());
			for(ElementState element: elements) {
				element = element_state_service.save(element);
				page_state_service.addElement(page_state.getId(), element.getId());
			}
	   		page_state.addElements(elements);
	   	});
	   	
	   	
			audit_record.setDataExtractionProgress(3.0/3.0);
			audit_record_service.save(audit_record);
			
		   	Principal principal = request.getUserPrincipal();
			if(principal != null) {
				String user_id = principal.getName();
		    	Account account = account_service.findByUserId(user_id);
		    	account_service.addAuditRecord(account.getEmail(), audit_record.getId());
			}
		   	
		   	Set<Audit> audits = new HashSet<>();
		   	
		   	//check if page state already
		   	//perform audit and return audit result
		   	log.warn("?????????????????????????????????????????????????????????????????????");
		   	log.warn("?????????????????????????????????????????????????????????????????????");
		   	log.warn("?????????????????????????????????????????????????????????????????????");
		   	
		   	log.warn("requesting performance audit from performance auditor....");
		   	ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
		   			.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
		   	performance_insight_actor.tell(page_state_record, ActorRef.noSender());
	
		   	for(AuditCategory audit_category : AuditCategory.values()) {
	   			List<Audit> rendered_audits_executed = audit_factory.executePageAudits(audit_category, page_state_record);
	
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
		   	//NOTE: nulls are present because they are no longer needed and being phased out
		   	SimplePage simple_page = new SimplePage(page_state_record.getUrl(), 
		   											page_state_record.getViewportScreenshotUrl(), 
		   											page_state_record.getFullPageScreenshotUrlOnload(), 
		   											page_state_record.getFullPageScreenshotUrlComposite(), 
		   											page_state_record.getFullPageWidth(), 
		   											page_state_record.getFullPageHeight(), 
		   											null, 
		   											page_state_record.getKey(),
		   											page_state_record.getId());
		   	
		   	//Map audits to page states
	    	//retrieve element set
		   	Collection<UXIssueMessage> issues = audit_service.retrieveUXIssues(audits);
	    	
	    	//retrieve issue set
		   	Collection<SimpleElement> elements = audit_service.retrieveElementSet(issues);

	    	//Map audits to page states
	    	Map<String, Set<String>> element_issue_map = audit_service.generateElementIssuesMap(audits);
	    	
	    	//generate IssueElementMap
	    	Map<String, String> issue_element_map = audit_service.generateIssueElementMap(audits);
	    	
	    	AuditScore score = AuditUtils.extractAuditScore(audits);
	    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record.getId()).getSrc();
		   	
	   		ElementIssueTwoWayMapping element_issues_map = new ElementIssueTwoWayMapping(issues,
	   																					 elements,
	   																					 issue_element_map, 
	   																					 element_issue_map, 
	   																					 score, page_src);
	   		
		   	return new PageAudits( audit_record.getStatus(), element_issues_map, simple_page, audit_record.getId());
		//});
		//return null;
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
	
	/**
	 * Get Excel file for {@link AuditRecord} with the given id
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownUserException
	 * @throws UnknownAccountException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
    @RequestMapping(path="/{audit_id}/excel", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Resource> exportExcelReport(HttpServletRequest request,
    									@PathVariable(value="audit_id", required=true) long audit_id) 
    											throws UnknownAccountException, 
														FileNotFoundException, IOException {
    	Optional<AuditRecord> audit_opt = audit_record_service.findById(audit_id);
    	if(!audit_opt.isPresent()) {
    		throw new AuditRecordNotFoundException();
    	}
    	
    	List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_opt.get().getId());
		PageState page = audit_record_service.getPageStateForAuditRecord(audit_opt.get().getId());	
    	for(Audit audit : audits) {
    		log.warn("audit key :: "+audit.getKey());
    		Set<UXIssueMessage> messages = audit_service.getIssues(audit.getId());
    		log.warn("audit issue messages size ...."+messages.size());
    		
    		for(UXIssueMessage message : messages) {
    			String element_selector = "";
    			if(ObservationType.ELEMENT.equals(message.getType()) 
					|| ObservationType.COLOR_CONTRAST.equals(message.getType())) {
    				element_selector = ux_issue_service.getElement(message.getId()).getCssSelector();
    			}
    			else {
    				element_selector = "No specific element is associated with this issue";
    			}
    			
    			UXIssueReportDto issue_dto = new UXIssueReportDto(message.getRecommendation(),
    															  message.getPriority(),
    															  message.getDescription(),
    															  message.getType(),
    															  message.getCategory(),
    															  message.getWcagCompliance(),
    															  message.getLabels(),
    															  audit.getWhyItMatters(),
    															  message.getTitle(),
    															  element_selector,
    															  page.getUrl());
    			ux_issues.add(issue_dto);
    		}
    		
    	}
    	log.warn("UX audits :: "+ux_issues.size());

    	URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(page.getUrl()));
    	XSSFWorkbook workbook = ReportService.generateDomainExcelSpreadsheet(ux_issues, sanitized_domain_url);
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);

    		HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost()+".xlsx");
            
            return ResponseEntity.ok()
            		.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            		.cacheControl(CacheControl.noCache())
            		.headers(headers)
            		.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
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

