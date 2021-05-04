package com.looksee.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.looksee.auth.Auth0Client;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.PageAudits;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.security.SecurityConfig;
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
    
    /**
     * Retrieves {@link Action account} with a given key
     * 
     * @param key account key
     * @return {@link Action account}
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
    public void getReport(HttpServletRequest request,
    		@RequestParam("url") String url
	) throws MalformedURLException {
    	URL sanitized_url = new URL(BrowserUtils.sanitizeUrl(url));
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	
    	Auth0Client auth_client = new Auth0Client();
    	auth_client.getUsername(principal.getName());
    	
    	auth_client.getUsername(request.getHeader("Authorization"));
    	
    }
}