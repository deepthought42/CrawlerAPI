package com.looksee.api;

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

import com.looksee.browsing.Crawler;
import com.looksee.models.Account;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.SendGridMailService;
import com.looksee.services.UXIssueMessageService;

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

}

