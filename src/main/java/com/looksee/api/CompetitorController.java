package com.looksee.api;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.looksee.api.exception.InsufficientSubscriptionException;
import com.looksee.models.Account;
import com.looksee.models.Competitor;
import com.looksee.models.audit.performance.PerformanceInsight;
import com.looksee.models.dto.exceptions.UnknownAccountException;
import com.looksee.models.message.CompetitorMessage;
import com.looksee.services.AccountService;
import com.looksee.services.CompetitorService;
import com.looksee.services.SubscriptionService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import org.springframework.http.MediaType;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "/competitors", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompetitorController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ActorSystem actor_system;
	
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
    		
    		ActorRef competitive_analyzer = actor_system.actorOf(SpringExtProvider.get(actor_system).props("competitiveAnalysisActor"),
    				"competitiveAnalysisActor" + UUID.randomUUID());
    		CompetitorMessage competitor_message = new CompetitorMessage(competitor.getId(), account.getId(), competitor);
    		competitive_analyzer.tell(competitor_message, null);
    	}
    }
}