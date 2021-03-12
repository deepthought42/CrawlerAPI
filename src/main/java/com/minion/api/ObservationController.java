package com.minion.api;


import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.services.ObservationService;

/**
 *	API for interacting with {@link Observation} objects 
 */
@RestController
@RequestMapping("/observations")
public class ObservationController {
	@Autowired
	private ObservationService observation_service;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
   // @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.POST, path="/{key}/recommendations/add")
    public Observation addRecommendation(HttpServletRequest request,
			final @PathVariable(value="key", required=true) String key,
    		final @RequestBody(required=true) String recommendation
	) throws UnknownAccountException {
    	/*
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	*/
    	
    	return observation_service.addRecommendation(key, recommendation);
    }
    
    @RequestMapping(method = RequestMethod.POST, path="/{key}/priority")
    public Observation updatePriority(HttpServletRequest request,
			final @PathVariable(value="key", required=true) String key,
			final @RequestParam(value="priority", required=true) String priority
	) throws UnknownAccountException {
    	/*
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	*/
    	
    	return observation_service.updatePriority(key, priority);
    }
}