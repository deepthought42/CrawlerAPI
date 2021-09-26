package com.looksee.api;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.looksee.browsing.Crawler;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.AuditStats;
import com.looksee.models.audit.DomainAuditStats;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.TargetAudienceSettings;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

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
    protected SendGridMailService sendgrid_service;
    
	@Autowired
	private SendGridMailService email_service;
	
	/**
     * Creates a new {@link Observation observation} 
     * 
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.POST, value="/{audit_record_id}/report")
    public @ResponseBody void requestReport(
							    		HttpServletRequest request,
										@PathVariable("audit_record_id") long audit_record_id,
							    		@RequestBody Account acct
	) throws UnknownAccountException {
    	log.warn("requesting report and saving account....");
    	//create an account
    	Account acct_record = account_service.findByEmail(acct.getEmail());
    	if(acct_record == null) {
    		acct_record = account_service.save(acct);
    	}
    	log.warn("adding audit record with id :: "+audit_record_id + " to account :: "+acct_record.getId());
    	account_service.addAuditRecord(acct_record.getId(), audit_record_id);
    	
    	PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_record_id);
    	log.warn("sending email for user ...."+acct_record.getEmail());
    	//Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	/*
    	String email_msg = "<html>"
    			+ "<body>"
    			+ "Hey there!"
    			+ "<br />"
    			+ "Your UX audit results for "+ page_state.getUrl() + " are ready."
    			+ "<br /><br />"
    			+ "You can <a href='https://app.look-see.com/quick-audit?audit_record_id="+ audit_record_id +"'>access the results here</a>."
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
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET, value="/{audit_record_id}/pages")
    public @ResponseBody Set<SimplePage> getPages(
						    		HttpServletRequest request,
									@PathVariable("audit_record_id") long audit_record_id
	) throws UnknownAccountException {
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
    													 page_state.getKey(), page_state.getId());
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
	    													 page_state.getKey(), page_state.getId());
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
     * 
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
    	log.warn("page audit record id :: "+ audit_record_id);
    	//Get most recent audits
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record_id);    		
    	log.warn("processing audits :: "+audits.size());
    	
    	//retrieve element set
    	Collection<UXIssueMessage> issues = audit_service.retrieveUXIssues(audits);
    	
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
     * 
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException 
     */
    @RequestMapping(method= RequestMethod.POST, path="/{audit_record_id}/persona/education")
    public @ResponseBody void updateTargetUserEducation(
    		HttpServletRequest request,
    		@PathVariable("audit_record_id") long audit_record_id,
    		@RequestBody TargetAudienceSettings audit_record_education
	) throws MalformedURLException {
    	log.warn("page audit record id :: "+ audit_record_id);
    	log.warn("education level :: "+audit_record_education.getTargetUserEducation());
    	//Get most recent audits
    	AuditRecord audit_record = audit_record_service.findById(audit_record_id).get();
    	audit_record.setTargetUserEducation(audit_record_education.getTargetUserEducation());
    	audit_record_service.save(audit_record);
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
    		long content_audits_complete = 0;
    		long info_arch_audits_complete = 0;
    		long aesthetic_audits_complete = 0;
    		
    		double content_score = 0.0;
    		double written_content_score = 0.0;
    		double imagery_score = 0.0;
    		double videos_score = 0.0;
    		double audio_score = 0.0;
    		
    		double info_arch_score = 0.0;
    		double seo_score = 0.0;
    		double menu_analysis_score = 0.0;
    		double performance_score = 0.0;
    		
    		double aesthetic_score = 0.0;
    		double color_score = 0.0;
    		double typography_score = 0.0;
    		double whitespace_score = 0.0;
    		double branding_score = 0.0;
    		
    		if( audit_record instanceof PageAuditRecord ) {
				
				Set<Audit> content_audits = audit_record_service.getAllContentAudits(audit_record.getId());
				content_score = AuditUtils.calculateScore(content_audits);
				written_content_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.WRITTEN_CONTENT);
				imagery_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.IMAGERY);
				videos_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.VIDEOS);
				audio_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.AUDIO);

				//get total content audit pages
				boolean is_content_audit_complete = AuditUtils.isContentAuditComplete(content_audits); // getContentAudit(audit_record.getId(), page_state_msg.getAuditRecordId()).size();//getAuditCount(AuditCategory.CONTENT, audit_records);
				if(is_content_audit_complete) {
					content_audits_complete++;
				}
				
				
				Set<Audit> info_architecture_audits = audit_record_service.getAllContentAudits(audit_record.getId());
				content_score = AuditUtils.calculateScore(info_architecture_audits);
				seo_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.SEO);
				menu_analysis_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.MENU_ANALYSIS);
				performance_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.PERFORMANCE);

				//get total information architecture audit pages
				boolean is_info_arch_audit_complete = AuditUtils.isInformationArchitectureAuditComplete(info_architecture_audits);
				if(is_info_arch_audit_complete) {
					info_arch_audits_complete++;
				}
				
				Set<Audit> aesthetic_audits = audit_record_service.getAllContentAudits(audit_record.getId());
				content_score = AuditUtils.calculateScore(aesthetic_audits);
				color_score = AuditUtils.calculateSubcategoryScore(aesthetic_audits, AuditSubcategory.COLOR_MANAGEMENT);
				typography_score = AuditUtils.calculateSubcategoryScore(aesthetic_audits, AuditSubcategory.TYPOGRAPHY);
				whitespace_score = AuditUtils.calculateSubcategoryScore(aesthetic_audits, AuditSubcategory.WHITESPACE);
				branding_score = AuditUtils.calculateSubcategoryScore(aesthetic_audits, AuditSubcategory.BRANDING);
				
				//get total aesthetic audit pages
				boolean is_aesthetic_audit_complete = AuditUtils.isAestheticsAuditComplete(aesthetic_audits);
				if(is_aesthetic_audit_complete) {
					aesthetic_audits_complete++;
				}

		    	//build stats object
				AuditStats audit_stats = new AuditStats(audit_record.getId(), 
														audit_record.getStartTime(), 
														audit_record.getEndTime(), 
														content_audits_complete,
														audit_record.getContentAuditProgress(),
														content_score,
														audit_record.getContentAuditMsg(), 
														written_content_score,
														imagery_score,
														videos_score,
														audio_score,
														info_arch_audits_complete, 
														audit_record.getInfoArchAuditProgress(), 
														info_arch_score, 
														audit_record.getInfoArchMsg(), 
														seo_score, 
														menu_analysis_score, 
														performance_score, 
														aesthetic_audits_complete, 
														audit_record.getAestheticAuditProgress(), 
														aesthetic_score, 
														audit_record.getAestheticMsg(), 
														color_score, 
														typography_score, 
														whitespace_score, 
														branding_score,
														((PageAuditRecord) audit_record).getElementsReviewed(),
														((PageAuditRecord) audit_record).getElementsFound(),
														audit_record.getDataExtractionProgress(),
														audit_record.getDataExtractionMsg());
				return audit_stats;				
    		}
    		else {
    			
    			Set<PageAuditRecord> audit_records = audit_record_service.getPageAuditRecords(audit_record_id);
				//get Page Count
				long pages_found = audit_records.size();
				long pages_reviewed = 0;//getCompletedPageAuditCount(audit_records);

				double score = 0;
				int audit_count = 0;
				long high_issue_count=0;
				long mid_issue_count=0;
				long low_issue_count=0;
				
				long elements_reviewed = 0;
				long elements_found = 0;
				
				for(PageAuditRecord page_audit : audit_records) {
					elements_reviewed += page_audit.getElementsReviewed();
					elements_found += page_audit.getElementsFound();
					
					//get total content audit pages
					Set<Audit> content_audits = audit_record_service.getAllContentAudits(page_audit.getId());
					written_content_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.WRITTEN_CONTENT);
					imagery_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.IMAGERY);
					videos_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.VIDEOS);
					audio_score = AuditUtils.calculateSubcategoryScore(content_audits, AuditSubcategory.AUDIO);

					for(Audit content_audit: content_audits) {
						//get issues
						Set<UXIssueMessage> issues = audit_service.getIssues(content_audit.getId());
						for( UXIssueMessage issue: issues ) {
							if(Priority.HIGH.equals(issue.getPriority())) {
								high_issue_count++;
							}
							else if(Priority.MEDIUM.equals(issue.getPriority())) {
								mid_issue_count++;
							}
							else if(Priority.LOW.equals(issue.getPriority())) {
								low_issue_count++;
							}
						}
						
						
						score += (content_audit.getPoints() / (double)content_audit.getTotalPossiblePoints());
						audit_count++;
					}
					boolean is_content_audit_complete = AuditUtils.isContentAuditComplete(content_audits); // getContentAudit(audit_record.getId(), page_state_msg.getAuditRecordId()).size();//getAuditCount(AuditCategory.CONTENT, audit_records);
					if(is_content_audit_complete) {
						content_audits_complete++;
					}
					
					//get total information architecture audit pages
					Set<Audit> info_architecture_audits = audit_record_service.getAllInformationArchitectureAudits(page_audit.getId());
					seo_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.SEO);
					menu_analysis_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.MENU_ANALYSIS);
					performance_score = AuditUtils.calculateSubcategoryScore(info_architecture_audits, AuditSubcategory.PERFORMANCE);
					
					for(Audit ia_audit: info_architecture_audits) {
						//get issues
						Set<UXIssueMessage> issues = audit_service.getIssues(ia_audit.getId());
						for( UXIssueMessage issue: issues ) {
							if(Priority.HIGH.equals(issue.getPriority())) {
								high_issue_count++;
							}
							else if(Priority.MEDIUM.equals(issue.getPriority())) {
								mid_issue_count++;
							}
							else if(Priority.LOW.equals(issue.getPriority())) {
								low_issue_count++;
							}
						}
						
						score += (ia_audit.getPoints() / (double)ia_audit.getTotalPossiblePoints());
						audit_count++;
					}
					boolean is_info_arch_audit_complete = AuditUtils.isInformationArchitectureAuditComplete(info_architecture_audits);
					if(is_info_arch_audit_complete) {
						info_arch_audits_complete++;
					}
					
					
					//get total aesthetic audit pages
					Set<Audit> aesthetics_audits = audit_record_service.getAllAestheticAudits(page_audit.getId());
					color_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.COLOR_MANAGEMENT);
					typography_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.TYPOGRAPHY);
					whitespace_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.WHITESPACE);
					branding_score = AuditUtils.calculateSubcategoryScore(aesthetics_audits, AuditSubcategory.BRANDING);

					for(Audit aesthetic_audit: aesthetics_audits) {
						//get issues
						Set<UXIssueMessage> issues = audit_service.getIssues(aesthetic_audit.getId());
						for( UXIssueMessage issue: issues ) {
							if(Priority.HIGH.equals(issue.getPriority())) {
								high_issue_count++;
							}
							else if(Priority.MEDIUM.equals(issue.getPriority())) {
								mid_issue_count++;
							}
							else if(Priority.LOW.equals(issue.getPriority())) {
								low_issue_count++;
							}
						}
						
						score += (aesthetic_audit.getPoints() / (double)aesthetic_audit.getTotalPossiblePoints());
						audit_count++;
					}
					boolean is_aesthetic_audit_complete = AuditUtils.isAestheticsAuditComplete(aesthetics_audits);
					if(is_aesthetic_audit_complete) {
						aesthetic_audits_complete++;
					}
				}
				
				double overall_score = ( score / audit_count ) * 100 ;
				

				
				//build stats object
				AuditStats audit_stats = new DomainAuditStats(audit_record.getId(),
															audit_record.getStartTime(),
															audit_record.getEndTime(),
															pages_reviewed, 
															pages_found,
															content_audits_complete,
															content_audits_complete / (double)audit_records.size(),
															written_content_score,
															imagery_score,
															videos_score,
															audio_score,
															audit_record.getContentAuditMsg(),
															info_arch_audits_complete,
															info_arch_audits_complete / (double)audit_records.size(),
															seo_score,
															menu_analysis_score,
															performance_score,
															audit_record.getInfoArchMsg(),
															aesthetic_audits_complete,
															aesthetic_audits_complete / (double)audit_records.size(),
															color_score,
															typography_score,
															whitespace_score,
															branding_score,
															audit_record.getAestheticMsg(),
															overall_score,
															high_issue_count,
															mid_issue_count,
															low_issue_count, 
															elements_reviewed,
															elements_found);
				
				return audit_stats;
    		}
    	}
    	else {
    		throw new AuditRecordNotFoundException();
    	}
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
