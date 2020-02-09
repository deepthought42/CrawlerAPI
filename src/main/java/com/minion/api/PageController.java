package com.minion.api;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Account;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.services.AccountService;
import com.qanairy.services.PageService;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/pages")
public class PageController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;
	
	@Autowired
	private PageService page_service;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/{page_key}/insights")
    public PerformanceInsight getInsights(HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String page_key
	) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
        logger.info("finding all page insights :: "+page_key);
        return page_service.findLatestInsight(page_key);
    }
}