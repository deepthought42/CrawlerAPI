package com.minion.api;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minion.api.exception.RuleValueRequiredException;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.RuleService;

import io.swagger.annotations.ApiOperation;


/**
 *	API for interacting with {@link User} data
 */
@RestController
public class ElementController {
	private static Logger log = LoggerFactory.getLogger(ElementController.class);

	@Autowired
	private ElementStateService element_service;
	
	@Autowired
	private RuleService rule_service;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Adds {@link Rule} to {@link Element element} with a given id
     * 
     * @param id element id
     * @return {@link Element element}
     */
    @ApiOperation(value = "adds Rule to Element with given id", response = Iterable.class)
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/elements/{id}/rules", method = RequestMethod.POST)
    public ElementState addRule(
    		HttpServletRequest request,
			@PathVariable(value="id", required=true) long id,
			@RequestParam(value="type", required=true) String type,
			@RequestParam(value="value", required=false) String value) throws RuleValueRequiredException 
    {  
        ElementState element = element_service.findById(id);
        element.addRule(rule_service.findByType(type, value));
        return element_service.save(element);
    }
    
    /**
     * Adds {@link Rule} to {@link Element element} with a given id
     * 
     * @param id element id
     * @return {@link Element element}
     */
    @ApiOperation(value = "updates given Element", response = Iterable.class)
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/elements", method = RequestMethod.PUT)
    public ElementState update(
    		HttpServletRequest request,
    		@RequestBody ElementState element_state) 
    {
    	//validate that element does not have duplicate rules
    	System.err.println("updating element state");
    	log.warn("element update state experienced");
        return element_service.save(element_state);
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