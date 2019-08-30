package com.minion.api;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
import com.qanairy.models.rules.RuleType;
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
    	Rule min_value_rule = null;
    	Rule max_value_rule = null;
    	Rule min_length_rule = null;
    	Rule max_length_rule = null;
    	Map<String, Integer> rule_duplicate_map = new HashMap<>();
    	for(Rule rule : element_state.getRules()){
    		if(!rule_duplicate_map.containsKey(rule.getKey())){
    			rule_duplicate_map.put(rule.getKey(), 0);
    		}
    		rule_duplicate_map.put(rule.getKey(), rule_duplicate_map.get(rule.getKey())+1);
    		if(rule.getType().equals(RuleType.MIN_VALUE)){
    			min_value_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MAX_VALUE)){
    			max_value_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
    			min_length_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
				max_length_rule = rule;
			}
    	}

    	for(int value : rule_duplicate_map.values()){
    		if(value > 1){
    			throw new DuplicatesNotAllowedException();
    		}
    	}
    	//check that min/max rules are valid
    	if( min_value_rule != null && (min_value_rule.getValue().isEmpty()
    			|| !StringUtils.isNumeric(min_value_rule.getValue())
    			|| Integer.parseInt(min_value_rule.getValue()) <= 0)){
    		throw new MinValueMustBePositiveNumber();
    	}
		if( max_value_rule != null && (max_value_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_value_rule.getValue())
				|| Integer.parseInt(max_value_rule.getValue()) <= 0)){
			throw new MaxValueMustBePositiveNumber();
    	}
		if( min_length_rule != null && (min_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(min_length_rule.getValue())
    			|| Integer.parseInt(min_length_rule.getValue()) <= 0)){
			throw new MinLengthMustBePositiveNumber();
		}
		if( max_length_rule != null && (max_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_length_rule.getValue())
    			|| Integer.parseInt(max_length_rule.getValue()) <= 0)){
			throw new MaxLengthMustBePositiveNumber();
		}


		if(min_value_rule != null && max_value_rule != null){
			int min_value = Integer.parseInt(min_value_rule.getValue());
			int max_value = Integer.parseInt(max_value_rule.getValue());
			if(min_value > max_value){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}
		if(min_length_rule != null && max_length_rule != null){
			int min_length = Integer.parseInt(min_length_rule.getValue());
			int max_length = Integer.parseInt(max_length_rule.getValue());
			if(min_length > max_length){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}

    	//check that min/max length rules are valid
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

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinValueMustBePositiveNumber extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 4419265853468867824L;

	public MinValueMustBePositiveNumber() {
		super("Minimum value rule must contain a positive number");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinLengthMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -2254262252488883657L;

	public MinLengthMustBePositiveNumber() {
		super("Minimum length rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MaxLengthMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4334601359263388271L;

	public MaxLengthMustBePositiveNumber() {
		super("Max length value rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MaxValueMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 250328142799757755L;

	public MaxValueMustBePositiveNumber() {
		super("Max value rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinCannotBeGreaterThanMaxException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4423969190558092393L;

	public MinCannotBeGreaterThanMaxException() {
		super("Minimum value cannot be greater than max value");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class DuplicatesNotAllowedException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 6335991211635956501L;

	public DuplicatesNotAllowedException() {
		super("Elements cannot have duplcate rules");
	}
}
