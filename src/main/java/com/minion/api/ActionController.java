package com.minion.api;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Action;
import com.qanairy.models.repository.ActionRepository;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/actions")
public class ActionController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ActionRepository action_repo;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves {@link Action account} with a given key
     * 
     * @param key account key
     * @return {@link Action account}
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET)
    public List<Action> getAll() {
        logger.info("get invoked");
        return IterableUtils.toList(action_repo.findAll());
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class ActionExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public ActionExistsException() {
		super("This account already exists.");
	}
}