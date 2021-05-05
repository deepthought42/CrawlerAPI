package com.looksee.api;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.auth.Auth0Client;
import com.looksee.security.SecurityConfig;
import com.looksee.services.SendGridMailService;
import com.looksee.utils.BrowserUtils;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/reports")
public class ReportController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    protected SecurityConfig appConfig;
    
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
    /*
    @RequestMapping(method = RequestMethod.GET, path="/excel")
    public XSSFWorkbook getExcelReport() throws IOException {
    	
    	XSSFWorkbook workbook = new XSSFWorkbook();
        //CellStyle cellStyle = workbook.createCellStyle();

        XSSFSheet sheet = workbook.createSheet("Java Books");
        Object[][] bookData = {
                {"Head First Java", "Kathy Serria", 79},
                {"Effective Java", "Joshua Bloch", 36},
                {"Clean Code", "Robert martin", 42},
                {"Thinking in Java", "Bruce Eckel", 35},
        };
 
        int rowCount = 0;
        
        for (Object[] aBook : bookData) {
            Row row = sheet.createRow(++rowCount);
             
            int columnCount = 0;
             
            for (Object field : aBook) {
                Cell cell = row.createCell(++columnCount);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                }
            }
             
        }
         
         
        try (FileOutputStream outputStream = new FileOutputStream("JavaBooks.xlsx")) {
            workbook.write(outputStream);
        }
        
        return workbook;
        
    }
    */
    
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
    	
	  String email_msg = "A UX audit has been requested by \n\n email : " + user_email + " \n\n webpage url : "+sanitized_url;
    	sendgrid_service.sendMail(email_msg);
    	//send email with user email and url that they want to have audited
    
    	
    	
    	return sanitized_url.toString();
    }
}