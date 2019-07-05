package com.minion.api;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.RuleService;


/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/elements")
public class ElementController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ElementStateService element_service;
	
	@Autowired
	private RuleService rule_service;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves {@link Action account} with a given key
     * 
     * @param key account key
     * @return {@link Action account}
     */
    @PreAuthorize("hasAuthority('add:rule')")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @RequestMapping(path="/{element_key}/rule/{rule_type", method = RequestMethod.PUT)
    public void addRule(HttpServletRequest request,
			@PathVariable(value="element_key", required=true) String element_key,
			@PathVariable(value="rule_type", required=true) String rule_type,
			@RequestParam(value="value", required=false) String value) {
        logger.info("finding all actions");
        
        ElementState element = element_service.findByKey(element_key);
        element.addRule(rule_service.findByType(rule_type, value));
        element_service.save(element);
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RuleExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public RuleExistsException() {
		super("The rule is already associated with the requested element.");
	}
}