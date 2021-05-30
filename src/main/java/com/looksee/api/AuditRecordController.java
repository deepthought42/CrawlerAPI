package com.looksee.api;

import java.net.MalformedURLException;
import java.util.HashSet;
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
import com.looksee.models.AuditStats;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.ElementIssueMap;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.IssueElementMap;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
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
    	acct = account_service.save(acct);
    	
    	log.warn("adding audit record with id :: "+audit_record_id + " to account :: "+acct.getId());
    	account_service.addAuditRecord(acct.getId(), audit_record_id);
    	
    	log.warn("sending email for user ...."+acct.getEmail());
    	//Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	String email_msg = "A UX audit has been requested by \n\n email : " + acct.getEmail() + " \n\n audit record id = "+audit_record_id;
    	sendgrid_service.sendMail(email_msg);
    	
    	log.warn("email sent!!");
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
    													 page_state.getFullPageScreenshotUrl(), 
    													 page_state.getViewportWidth(),
    													 page_state.getViewportHeight(), 
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
	    													 page_state.getFullPageScreenshotUrl(), 
	    													 page_state.getViewportWidth(),
	    													 page_state.getViewportHeight(), 
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
    	//Map audits to page states
    	Set<ElementIssueMap> element_issue_map = audit_service.generateElementIssueMap(audits);
    	
    	//generate IssueElementMap
    	Set<IssueElementMap> issue_element_map = audit_service.generateIssueElementMap(audits);
    	
    	AuditScore score = AuditUtils.extractAuditScore(audits);
    	String page_src = audit_record_service.getPageStateForAuditRecord(audit_record_id).getSrc();
    	
    	//package both elements into an object definition
    	return new ElementIssueTwoWayMapping(issue_element_map, element_issue_map, score, page_src);
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
    	Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	
    	if(audit_record.isPresent()) {
    		AuditRecord audit = audit_record.get();
    		long content_audits_complete = 0;
    		long info_arch_audits_complete = 0;
    		long aesthetic_audits_complete = 0;
    		
    		if( audit instanceof PageAuditRecord ) {

    	    	//get Page Count
				long page_count = 1;
				
				//get total content audit pages
				boolean is_content_audit_complete = isContentAuditComplete(audit_record_service.getAllContentAudits(audit.getId())); // getContentAudit(audit_record.getId(), page_state_msg.getAuditRecordId()).size();//getAuditCount(AuditCategory.CONTENT, audit_records);
				if(is_content_audit_complete) {
					content_audits_complete++;
				}
				
				//get total information architecture audit pages
				boolean is_info_arch_audit_complete = isInformationArchitectureAuditComplete(audit_record_service.getAllInformationArchitectureAudits(audit.getId()));
				if(is_info_arch_audit_complete) {
					info_arch_audits_complete++;
				}
				
				//get total aesthetic audit pages
				boolean is_aesthetic_audit_complete = isAestheticsAuditComplete(audit_record_service.getAllAestheticAudits(audit.getId()));
				if(is_aesthetic_audit_complete) {
					aesthetic_audits_complete++;
				}

		    	//build stats object
				AuditStats audit_stats = new AuditStats(audit.getId(), 
														audit.getStartTime(), 
														audit.getEndTime(), 
														page_count, 
														content_audits_complete,
														audit.getContentAuditProgress(),
														audit.getContentAuditMsg(),
														info_arch_audits_complete, 
														audit.getInfoArchAuditProgress(),
														audit.getInfoArchMsg(),
														aesthetic_audits_complete,
														audit.getAestheticAuditProgress(),
														audit.getAestheticMsg());
				return audit_stats;				
    		}
    		else {
    			
    			Set<PageAuditRecord> audit_records = audit_record_service.getPageAuditRecords(audit_record_id);
				//get Page Count
				long page_count = audit_records.size();
				
				for(PageAuditRecord page_audit : audit_records) {
					//get total content audit pages
					boolean is_content_audit_complete = isContentAuditComplete(audit_record_service.getAllContentAudits(page_audit.getId())); // getContentAudit(audit_record.getId(), page_state_msg.getAuditRecordId()).size();//getAuditCount(AuditCategory.CONTENT, audit_records);
					if(is_content_audit_complete) {
						content_audits_complete++;
					}
					
					//get total information architecture audit pages
					boolean is_info_arch_audit_complete = isInformationArchitectureAuditComplete(audit_record_service.getAllInformationArchitectureAudits(page_audit.getId()));
					if(is_info_arch_audit_complete) {
						info_arch_audits_complete++;
					}
					
					//get total aesthetic audit pages
					boolean is_aesthetic_audit_complete = isAestheticsAuditComplete(audit_record_service.getAllAestheticAudits(page_audit.getId()));
					if(is_aesthetic_audit_complete) {
						aesthetic_audits_complete++;
					}
				}
				
				//build stats object
				AuditStats audit_stats = new AuditStats(audit.getId(), 
														audit.getStartTime(), 
														audit.getEndTime(), 
														page_count, 
														content_audits_complete,
														audit.getContentAuditProgress(),
														audit.getContentAuditMsg(),
														info_arch_audits_complete, 
														audit.getInfoArchAuditProgress(),
														audit.getInfoArchMsg(),
														aesthetic_audits_complete,
														audit.getAestheticAuditProgress(),
														audit.getAestheticMsg());
				
				return audit_stats;
    		}
    	}
    	else {
    		throw new AuditRecordNotFoundException();
    	}
    }

	private boolean isAestheticsAuditComplete(Set<Audit> audits) {
		return audits.size() == 2;
	}

	private boolean isContentAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
	}
	
	private boolean isInformationArchitectureAuditComplete(Set<Audit> audits) {
		return audits.size() == 3;
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
