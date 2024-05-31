package com.looksee.api;

import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.api.exception.MissingSubscriptionException;
import com.looksee.browsing.Crawler;
import com.looksee.dto.AuditRecordDto;
import com.looksee.models.Account;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.AuditStats;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.DomainAuditStats;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.PageAuditStats;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "auditrecords", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditRecordController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

   	public final static long SECS_PER_HOUR = 60 * 60;
	
	@Autowired
	private AccountService account_service;
	
    @Autowired
    protected SecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected UXIssueMessageService issue_message_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected PageStateService page_state_service;
    
    @Autowired
    protected SendGridMailService sendgrid_service;

	@Autowired
	protected JourneyService journey_service;
	

	/**
     * Creates a new {@link Observation observation} 
     * 
     * @return returns a List of {@link AuditRecord audits}
	 * 
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody List<AuditRecordDto> getAuditRecords(
												HttpServletRequest request
	) throws UnknownAccountException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);
		
		if(acct == null){
			throw new UnknownAccountException();
		}
		else if(acct.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}

		List<AuditRecord> audits_records = audit_record_service.findByAccountId(acct.getId());
		return buildAudits(audits_records);
    }
	
	/**
     * Requests a an audit report by creating a user account and adding the audit record to the account.
	 * 
     */
    @RequestMapping(method = RequestMethod.POST, value="/{audit_record_id}/report")
    public @ResponseBody void requestReport(
										HttpServletRequest request,
										@PathVariable("audit_record_id") long audit_record_id,
										@RequestBody Account acct
	) {
		log.warn("requesting report and saving account....");
    	//create an account
    	Account acct_record = account_service.findByEmail(acct.getEmail());
    	if(acct_record == null) {
    		acct_record = account_service.save(acct);
    	}
    	log.warn("adding audit record with id :: "+audit_record_id + " to account :: "+acct_record.getId());
    	account_service.addAuditRecord(acct_record.getId(), audit_record_id);
    	
    	//PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record_id);
    	//log.warn("sending email for user ...."+acct_record.getEmail());
    	//Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	/*
    	String email_msg = "<html>"
    			+ "<body>"
    			+ "Hey there!"
    			+ "<br />"
    			+ "Your UX audit results for "+ page_state.getUrl() + " are ready."
    			+ "<br /><br />"
    			+ "You can <a href='https://app.look-see.com/?audit_record_id="+ audit_record_id +"'>access the results here</a>."
    			+ "</body>"
    			+ "</html>";
    	
    	Email from = new Email("bkindred@look-see.com");
    	String subject = "Requesting audit report";
    	Email to = new Email("support@look-see.com");
    	*/
    	//sendgrid_service.sendMail(to, from, subject, email_msg);
    	//sendgrid_service.sendMail(email_msg);
    	
    	
       	//send request to support@look-see.com to send email once audit is complete
    }

    /**
     * Creates a new {@link Observation observation} 
     * 
     * @return returns set of {@link SimplePage pages}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET, value="/{audit_record_id}/pages")
    public @ResponseBody Set<SimplePage> getPages(
						    		HttpServletRequest request,
									@PathVariable("audit_record_id") long audit_record_id
	) {
    	//get audit record
    	Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	
    	if(audit_record.isPresent()) {
    		AuditRecord audit = audit_record.get();
    		if(audit instanceof PageAuditRecord) {
    			PageState page_state = audit_record_service.getPageStateForAuditRecord(audit.getId());
    			
    			Set<SimplePage> pages = new HashSet<>();
    			
    			SimplePage simple_page = new SimplePage( page_state.getUrl(), 
    													 page_state.getViewportScreenshotUrl(), 
    													 page_state.getFullPageScreenshotUrlOnload(), 
    													 page_state.getFullPageScreenshotUrlComposite(),
    													 page_state.getFullPageWidth(), 
    													 page_state.getFullPageHeight(),
    													 page_state.getSrc(), 
    													 page_state.getKey(), 
    													 page_state.getId());
    			pages.add(simple_page);
    			return pages;
    		}
    		else {
    			//get all page states for domain audit record
    			Set<PageState> page_states = audit_record_service.getPageStatesForDomainAuditRecord(audit_record_id);
    			
    			
    			Set<SimplePage> pages = new HashSet<>();
    			
    			for(PageState page_state: page_states) {
	    			SimplePage simple_page = new SimplePage( page_state.getUrl(), 
	    													 page_state.getViewportScreenshotUrl(), 
	    													 page_state.getFullPageScreenshotUrlOnload(), 
	    													 page_state.getFullPageScreenshotUrlComposite(),
	    													 page_state.getFullPageWidth(),
	    													 page_state.getFullPageHeight(),
	    													 page_state.getSrc(), 
	    													 page_state.getKey(), 
	    													 page_state.getId());
	    			pages.add(simple_page);
    			}
    			
    			return pages;
    		}
    	}
    	else {
    		throw new AuditRecordNotFoundException();
    	}
    }
    
    /**
     * Retrieves a dataset consisting of {@link ElementState elements} and {@link UXIssueMessage issues}
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    @RequestMapping(method= RequestMethod.GET, path="/{audit_record_id}/elements")
    public @ResponseBody ElementIssueTwoWayMapping getPageAuditElements(
														HttpServletRequest request,
														@PathVariable("audit_record_id") long audit_record_id
	) throws MalformedURLException {
    	//Get most recent audits
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record_id);

    	//retrieve element set
		Collection<? extends UXIssueMessage> issues = audit_service.retrieveUXIssues(audits);

    	//retrieve issue set
		Collection<SimpleElement> elements = audit_service.retrieveElementSet(issues);

    	//Map audits to page states
		Map<String, Set<String>> element_issue_map = audit_service.generateElementIssuesMap(audits);

    	//generate IssueElementMap
		Map<String, String> issue_element_map = audit_service.generateIssueElementMap(audits);

		AuditScore score = AuditUtils.extractAuditScore(audits);
		String page_src = audit_record_service.getPageStateForAuditRecord(audit_record_id).getSrc();

    	//package both elements into an object definition
		return new ElementIssueTwoWayMapping(issues,
												elements,
												issue_element_map,
												element_issue_map,
												score,
												page_src);
    }
    
    /**
     * Creates a new {@link Observation observation} 
     * 
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET, value="/{audit_record_id}/stats")
    public @ResponseBody AuditStats getAuditStat(
									HttpServletRequest request,
									@PathVariable("audit_record_id") long audit_record_id
	) throws UnknownAccountException {
    	//get audit record
		Optional<AuditRecord> audit_record_opt = audit_record_service.findById(audit_record_id);

		if(audit_record_opt.isPresent()) {
			AuditRecord audit_record = audit_record_opt.get();

			long page_count = 1;
			double score = 0.0;
			int audit_count = 0;

			double written_content_score = 0.0;
			double imagery_score = 0.0;
			double videos_score = 0.0;
			double audio_score = 0.0;

			double seo_score = 0.0;
			double menu_analysis_score = 0.0;
			double performance_score = 0.0;

			//double aesthetic_score = 0.0;
			double text_color_contrast_score = 0.0;
			double non_text_color_contrast_score = 1.0;
			
			double typography_score = 0.0;
			double whitespace_score = 0.0;
			double branding_score = 0.0;

			Set<Audit> audits = new HashSet<>();
			if(audit_record instanceof DomainAuditRecord){
				audits = audit_record_service.getAllAuditsForDomainAudit(audit_record.getId());
			}
			else if(audit_record instanceof PageAuditRecord){
				audits = audit_record_service.getAllAudits(audit_record.getId());
			}
			
			written_content_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WRITTEN_CONTENT);
			imagery_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.IMAGERY);
			videos_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.VIDEOS);
			audio_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.AUDIO);

			seo_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.SEO);
			menu_analysis_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.MENU_ANALYSIS);
			performance_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.PERFORMANCE);
			double link_score = AuditUtils.calculateScoreByName(audits, AuditName.LINKS);

			//aesthetic_score = AuditUtils.calculateScore(audits);
			log.warn("audits = "+audits);
			text_color_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
			log.warn("text color contrast score = "+text_color_contrast_score);

			non_text_color_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
			typography_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.TYPOGRAPHY);
			whitespace_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WHITESPACE);
			branding_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.BRANDING);

			for (Audit audit : audits) {
				// get issues
				if (audit.getTotalPossiblePoints() == 0) {
					score += 1;
				} else {
					score += (audit.getPoints() / (double) audit.getTotalPossiblePoints());
				}
			}
			audit_count += audits.size();

			log.debug("audit record id = "+audit_record.getId());
			log.debug("aesthetic audit progress = "+audit_record.getAestheticAuditProgress());
			double overall_score = ( score / audit_count ) * 100 ;

			//build stats object
			log.debug("page count = " + page_count);
			log.debug("---------------------------------------------");
			Set<AuditName> audit_labels = new HashSet<AuditName>();
			audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
			audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
			
			audit_labels.add(AuditName.IMAGE_COPYRIGHT);
			audit_labels.add(AuditName.IMAGE_POLICY);
			audit_labels.add(AuditName.ALT_TEXT);
			audit_labels.add(AuditName.READING_COMPLEXITY);
			audit_labels.add(AuditName.PARAGRAPHING);
			
			audit_labels.add(AuditName.TITLES);
			audit_labels.add(AuditName.LINKS);
			audit_labels.add(AuditName.METADATA);
			audit_labels.add(AuditName.ENCRYPTED);
			
			ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
			
			double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS,
																	1,
																			audits,
																			audit_labels);
			
			double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT,
																	1,
																	audits,
																	audit_labels);

			double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE,
																			1,
																			audits,
																			audit_labels);
															
			double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
			double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
			double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
			double a11y_score = AuditUtils.calculateAccessibilityScore(audits);
			
			log.warn("Accessibility score = "+a11y_score);
			if(content_progress >= 1 && info_architecture_progress >= 1 && visual_design_progress >= 1) {
				execution_status = ExecutionStatus.COMPLETE;
			}
			else {
				execution_status = ExecutionStatus.IN_PROGRESS;
			}
			
			// get Page Count
			if(audit_record instanceof DomainAuditRecord){
				page_count = audit_record_service.getPageAuditCount(audit_record.getId());
				int journeysExplored = audit_record_service.getNumberOfJourneysWithStatus(audit_record.getId(), JourneyStatus.VERIFIED);
				int journeysTotal = audit_record_service.getNumberOfJourneys(audit_record.getId());
				DomainAuditStats audit_stats = new DomainAuditStats(audit_record.getId(),
														journeysExplored,
														journeysTotal,
														a11y_score,
														content_score,
														written_content_score,
														imagery_score,
														info_architecture_score,
														seo_score,
														visual_design_score,
														text_color_contrast_score,
														non_text_color_contrast_score,
														execution_status,
														link_score);
					return audit_stats;
			}
			else if(audit_record instanceof PageAuditRecord){
				page_count = 1;
				PageAuditStats audit_stats = new PageAuditStats(audit_record.getId(),
														audit_record.getStartTime(),
														audit_record.getEndTime(),
														overall_score,
														content_score,
														written_content_score,
														imagery_score,
														videos_score,
														audio_score,
														info_architecture_score,
														seo_score,
														menu_analysis_score,
														performance_score,
														visual_design_score,
														text_color_contrast_score,
														non_text_color_contrast_score,
														typography_score,
														whitespace_score,
														branding_score,
														execution_status,
														link_score);
					return audit_stats;
			}
			
			return null;
		}
		else {
			throw new AuditRecordNotFoundException();
		}
    }
    
    
	/**
	 * Retrieves journeys from the domain audit and calculates a value between 0 and 1 that indicates the progress
	 * based on the number of journey's that are still in the CANDIDATE status vs the journeys that don't have the CANDIDATE STATUS
	 * 
	 * NOTE : Progress is based on a magic number(10000). Be aware that all progress will be based on an assumed maximum element 
	 *        count of 1000
	 * 
	 * @param audit_record_id
	 * 
	 * @return progress percentage as a value between 0 and 1
	 */
	private double getPageDataExtractionProgress(long audit_record_id) {
		double milestone_count = 1.0;
		
		PageState page = audit_record_service.findPage(audit_record_id);
		
		int audit_count = audit_record_service.getAllAudits(audit_record_id).size();
		//if the audit_record has audits return 1
		if(audit_count > 0) {
			return 1.0;
		}
		
		//if audit_record has page associated with it add 1 point
		if(page != null) {
			milestone_count += 1;
		}
		else {
			return 0.0;
		}
		
		int element_count = page_state_service.getElementStateCount(page.getId());
		
		//if the associated page has elements add 1000/element_count
		int max_elements = 1000;
		if(element_count > 0) {
			if(element_count > max_elements) {
				max_elements = element_count;
			}
			milestone_count += max_elements / (double)element_count;
		}
		
		return milestone_count / 3.0;
	}


	/**
	 * Convert list of {@link AuditRecord audit_records} to list of {@link AuditDTO}
	 * 
	 * @param audits_records
	 * @return
	 */
	private List<AuditRecordDto> buildAudits(List<AuditRecord> audits_records) {
		List<AuditRecordDto> auditDtoList = new ArrayList<>();
		for(AuditRecord audit_record: audits_records){

			Set<Audit> audits = new HashSet<>();
			if(audit_record instanceof DomainAuditRecord){
				audits = audit_record_service.getAllAuditsForDomainAudit(audit_record.getId());
			}
			else if(audit_record instanceof PageAuditRecord){
				audits = audit_record_service.getAllAudits(audit_record.getId());
			}

			double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
			double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
			double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);

			auditDtoList.add(new AuditRecordDto(audit_record.getId(),
									audit_record.getStatus(),
									audit_record.getType(),
									audit_record.getStartTime(),
									visual_design_score,
									content_score,
									info_architecture_score,
									audit_record.getCreatedAt(),
									audit_record.getEndTime(),
									audit_record.getUrl()));
		}

		return auditDtoList;
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class AuditRecordNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public AuditRecordNotFoundException() {
		super("Oh no! We couldn't find the audit record you asked for.");
	}
}
