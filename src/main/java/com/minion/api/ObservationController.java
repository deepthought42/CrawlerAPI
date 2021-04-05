package com.minion.api;


import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.ObservationType;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.services.ObservationService;

/**
 *	API for interacting with {@link Observation} objects 
 */
@RestController
@RequestMapping("/observations")
public class ObservationController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

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
    /*
    @RequestMapping(method = RequestMethod.POST, path="/$key/recommendations/add")
    public Observation addRecommendation(HttpServletRequest request,
			final @PathVariable(value="key", required=true) String key,
    		final @RequestBody(required=true) String recommendation
	) throws UnknownAccountException {
    
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}

    	log.warn("Recommendation string to be saved :: "+recommendation);
    	//find audit by key and add recommendation
    	Set<String> labels = new HashSet<>();
    	Observation observation = observation_service.addRecommendation(key, recommendation);
    	if(ObservationType.ELEMENT.equals(observation.getType())){
    		return new Observation(
    							observation.getDescription(), 
    							observation.getWhyItMatters(), 
    							observation.getAdaCompliance(), 
    							observation.getPriority(), 
    							observation.getKey(), 
    							observation.getRecommendations(),
    							observation.getType(), 
    							labels);
    	}
    	
    	return observation;
    }
     */
    
    /**
     * Adds recommendation to @link Audit audit}
     * 
     * @param key key for audit that recommendation should be added to
     * @param recommendation the expert opinion that should be added to the audit
     * 
     * @return {@link Audit audit} with given ID
     */
/*
    @RequestMapping(path="{observation_key}/recommendations", method = RequestMethod.DELETE)
    public @ResponseBody void deleteRecommendation(
    		HttpServletRequest request,
    		final @PathVariable String observation_key,
    		@RequestParam(required=true) String recommendation
	) {
    	//find audit by key and add recommendation
    	Observation observation = observation_service.findByKey(observation_key);
    	observation.removeRecommendation(recommendation);
       	
       	//save and return
       	observation_service.save(observation);
    }
    
    @RequestMapping(method = RequestMethod.POST, path="/$key/priority")
    public Observation updatePriority(HttpServletRequest request,
			final @PathVariable(value="key", required=true) String key,
			final @RequestParam(value="priority", required=true) String priority
	) throws UnknownAccountException {

    	return observation_service.updatePriority(key, priority);
    }
    */
}