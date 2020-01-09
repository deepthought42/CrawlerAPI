package com.minion.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Action;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.services.PageService;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/pages")
public class PageController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PageService page_service;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET)
    public List<PerformanceInsight> getInsights(HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String page_key
	) {
        logger.info("finding all page insights");
        return IterableUtils.toList(page_service.findAllInsights(page_key));
    }
}