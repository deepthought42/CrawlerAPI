package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import com.minion.api.exception.PaymentDueException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.message.DiscoveryActionMessage;
import com.qanairy.services.AccountService;
import com.qanairy.services.DomainService;
import com.qanairy.services.SubscriptionService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.StripeException;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;



/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@Controller
@RequestMapping("/discovery")
public class DiscoveryController {
	private static Logger log = LoggerFactory.getLogger(DiscoveryController.class);

	private static Map<String, ActorRef> domain_actors = new HashMap<>();
	
    @Autowired
    private AccountService account_service;

    @Autowired
    private DomainService domain_service;
    
    @Autowired
    private ActorSystem actor_system;

    @Autowired
    private SubscriptionService subscription_service;

	@RequestMapping(path="/status", method = RequestMethod.GET)
    public @ResponseBody DiscoveryRecord isDiscoveryRunning(HttpServletRequest request,
    												@RequestParam(value="url", required=true) String url)
    														throws UnknownAccountException{
		Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	return domain_service.getMostRecentDiscoveryRecord(url);
    }

    /**
	 *
	 * @param request
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException
     * @throws PaymentDueException
     * @throws StripeException
	 */
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping(path="/start", method = RequestMethod.GET)
	public @ResponseBody DiscoveryRecord startDiscovery(HttpServletRequest request,
											   	  		@RequestParam(value="url", required=true) String url)
										   	  				throws MalformedURLException,
										   	  						UnknownAccountException,
										   	  						DiscoveryLimitReachedException,
										   	  						PaymentDueException, StripeException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}

    	/*
    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 discovered tests left. Please upgrade to run a discovery");
    	}
    	*/

    	DiscoveryRecord last_discovery_record = domain_service.getMostRecentDiscoveryRecord(url);

    	Date now = new Date();
    	long diffInMinutes = 10000;
    	if(last_discovery_record != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_discovery_record.getStartTime().getTime()) / (1000 * 60) ));
    	}

    	Domain domain = domain_service.findByHost(url);
    	log.warn("domain retrieved from host :: " + domain + "   :   "+ url);
    	
		if(diffInMinutes > 1440){
			//set discovery path count to 0 in case something happened causing the count to be greater than 0 for more than 24 hours
			if(!domain_actors.containsKey(url)){
				ActorRef domain_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
						  .props("domainActor"), "domain_actor"+UUID.randomUUID());
				domain_actors.put(url, domain_actor);
			}
		    
			DiscoveryActionMessage discovery_action_msg = new DiscoveryActionMessage(DiscoveryAction.START, domain, acct, BrowserType.create(domain.getDiscoveryBrowserName()));
			domain_actors.get(url).tell(discovery_action_msg, null);
		}
        else{
        	//Throw error indicating discovery has been or is running
        	//return new ResponseEntity<String>("Discovery is already running", HttpStatus.INTERNAL_SERVER_ERROR);
        	//Fire discovery started event
	    	Map<String, String> discovery_started_props = new HashMap<String, String>();
	    	discovery_started_props.put("url", url);
	    	discovery_started_props.put("browser", domain.getDiscoveryBrowserName());
	    	discovery_started_props.put("already_running", "true");

	    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();

	    	analytics.enqueue(TrackMessage.builder("Existing discovery found")
	    		    .userId(acct.getUsername())
	    		    .properties(discovery_started_props)
	    		);

        	throw new ExistingDiscoveryFoundException();
        }
		
		return last_discovery_record;
	}

	/**
	 *
	 * @param request
	 * @param account_key key of account to stop work for
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException
	 */
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping("/stop")
	public @ResponseBody void stopDiscovery(HttpServletRequest request, @RequestParam(value="url", required=true) String url)
			throws MalformedURLException, UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_service.findByUserId(id);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	/*
    	DiscoveryRecord last_discovery_record = null;
		Date started_date = new Date(0L);
		for(DiscoveryRecord record : domain_service.getDiscoveryRecords(url)){
			if(record.getStartTime().compareTo(started_date) > 0 && record.getDomainUrl().equals(url)){
				started_date = record.getStartTime();
				last_discovery_record = record;
			}
		}

		last_discovery_record.setStatus(DiscoveryStatus.STOPPED);
		discovery_service.save(last_discovery_record);
		WorkAllowanceStatus.haltWork(acct.getUsername());
		*/
    	Domain domain = domain_service.findByHost(url);

    	if(!domain_actors.containsKey(domain.getUrl())){
			ActorRef domain_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("domainActor"), "domain_actor"+UUID.randomUUID());
			domain_actors.put(domain.getUrl(), domain_actor);
		}
    	
		DiscoveryActionMessage discovery_action_msg = new DiscoveryActionMessage(DiscoveryAction.STOP, domain, acct, BrowserType.create(domain.getDiscoveryBrowserName()));
		domain_actors.get(url).tell(discovery_action_msg, null);
		
	}

}

@ResponseStatus(HttpStatus.SEE_OTHER)
class ExistingDiscoveryFoundException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7200878662560716215L;

	public ExistingDiscoveryFoundException() {
		super("A discovery is already running");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MissingDiscoveryPlanException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public MissingDiscoveryPlanException() {
		super("You are not subscribed to run discoveries. Upgrade now!");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class DiscoveryLimitReachedException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public DiscoveryLimitReachedException() {
		super("You’ve reached your discovery limit. Upgrade your account now!");
	}
}
