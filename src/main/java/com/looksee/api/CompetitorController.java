package com.looksee.api;

import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.looksee.models.Competitor;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.services.CompetitorService;
import com.looksee.services.DomainService;

import org.springframework.http.MediaType;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "competitors", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompetitorController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainService domain_service;
	

}