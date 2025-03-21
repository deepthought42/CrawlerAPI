package com.crawlerApi.api;

import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.crawlerApi.api.exception.InsufficientSubscriptionException;
import com.crawlerApi.models.Account;
import com.crawlerApi.models.audit.performance.PerformanceInsight;
import com.crawlerApi.models.competitiveanalysis.Competitor;
import com.crawlerApi.models.dto.exceptions.UnknownAccountException;
import com.crawlerApi.models.message.CompetitorMessage;
import com.crawlerApi.services.AccountService;
import com.crawlerApi.services.CompetitorService;
import com.crawlerApi.services.SubscriptionService;


/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping("/competitors")
public class CompetitorController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private CompetitorService competitor_service;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	/**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET, path="/{competitor_id}")
    public void startAnalysis(HttpServletRequest request,
			@PathVariable(value="competitor_id", required=true) long competitor_id
	) throws InsufficientSubscriptionException {
    	Principal principal = request.getUserPrincipal();
		String user_id = principal.getName();
		Account account = account_service.findByUserId(user_id);
		
		//if user doesn't have the necessary subscription, then show upgradeSubscriptionException()
		if(!subscription_service.canAccessCompetitiveAnalysis(account)) {
			throw new InsufficientSubscriptionException();
		}
		
    	Optional<Competitor> competitor_opt = competitor_service.findById(competitor_id);
    	if(competitor_opt.isPresent()) {
    		Competitor competitor = competitor_opt.get();
    		
    		CompetitorMessage competitor_message = new CompetitorMessage(competitor.getId(), account.getId(), competitor);
    	}
    }
}