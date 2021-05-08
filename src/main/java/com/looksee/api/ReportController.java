package com.looksee.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.auth.Auth0Client;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.security.SecurityConfig;
import com.looksee.services.AuditService;
import com.looksee.services.PageStateService;
import com.looksee.services.ReportService;
import com.looksee.services.SendGridMailService;
import com.looksee.utils.BrowserUtils;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping(path = "reports")
public class ReportController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    protected SecurityConfig appConfig;
    
    @Autowired
    private PageStateService page_service;
    
    @Autowired
    private AuditService audit_service;
    
    @Autowired
    protected SendGridMailService sendgrid_service;
    /**
     * Retrieves {@link Action account} with a given key
     * 
     * @param key account key
     * @return {@link Action account}
     * @throws MessagingException 
     * @throws IOException 
     */
    
    @RequestMapping(method = RequestMethod.GET, path="/excel")
    public ResponseEntity<Resource> getExcelReport(
    		HttpServletRequest request,
    		@RequestParam("page_state_key") String page_state_key
	) throws IOException {
    	PageState page_state = page_service.findByKey(page_state_key);
    	List<Audit> audits = page_service.getAudits(page_state_key);
    	List<UXIssueMessage> ux_issues = new ArrayList<>();
    	for(Audit audit : audits) {
    		log.warn("audit key :: "+audit.getKey());
    		Set<UXIssueMessage> messages = audit_service.getIssues(audit.getKey());
    		log.warn("audit issue messages size ...."+messages.size());
    		ux_issues.addAll(messages);
    	}
    	log.warn("UX audits :: "+ux_issues.size());
    	
    	XSSFWorkbook workbook = ReportService.generateExcelSpreadsheet(ux_issues, new URL(BrowserUtils.sanitizeUrl(page_state.getUrl())));
         
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);

    		// return IOUtils.toByteArray(in);
    		
    		HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; " + new URL(BrowserUtils.sanitizeUrl(page_state.getUrl())).getHost()+".xlsx");
            
            return ResponseEntity.ok()
            		.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            		.cacheControl(CacheControl.noCache())
            		.headers(headers)
            		.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
        }
        
        //return workbook;
    }
    
    
    @RequestMapping(method = RequestMethod.GET)
    public String getReport(HttpServletRequest request,
    		@RequestParam("url") String url
	) throws  GeneralSecurityException, IOException {
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(url));
    	
    	String token = request.getHeader("Authorization").split(" ")[1];
    	Auth0Client auth_client = new Auth0Client();
    	//Auth0ManagementApi auth0_mgmt = new Auth0ManagementApi(token);
    	//auth_client.getUsername(principal.getName());

    	String user_email = auth_client.getEmail(token);
    	log.warn("user email :: "+user_email);
    	
    	String email_msg = "A UX audit has been requested by \n\n email : " + user_email + " \n\n webpage url : <a href='"+sanitized_url + "'>"+sanitized_url+"</a>";
    	sendgrid_service.sendMail(email_msg);
    	//send email with user email and url that they want to have audited
    
    	
    	
    	return sanitized_url.toString();
    }
}