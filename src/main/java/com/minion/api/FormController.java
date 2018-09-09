package com.minion.api;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.repository.FormRepository;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/form")
public class FormController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FormRepository form_repo;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves all {@link Form form}s
     * 
     * @param key account key
     * @return {@link FormRecord account}
     */
    @PreAuthorize("hasAuthority('read:form')")
    @RequestMapping(method = RequestMethod.GET)
    public List<Form> getAll() {
        logger.info("finding all form records");      
        return IterableUtils.toList(form_repo.findAll());
    }
    
    /**
     * Retrieves {@link FormRecord account} with a given key
     * 
     * @param key account key
     * @return {@link FormRecord account}
     * @throws IOException 
     */

	@PreAuthorize("hasAuthority('update:form')")
    @RequestMapping(method = RequestMethod.POST)
    public List<Form> updateForm(@RequestParam(value="key", required=true) String key,
								 @RequestParam(value="form_fields", required=true) List<PageElement> form_fields) throws IOException {
    	
    	Form form_record = form_repo.findByKey(key);
    	
    	form_record = form_repo.save(form_record);
    	return null;
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class FormExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public FormExistsException() {
		super("This account already exists.");
	}
}