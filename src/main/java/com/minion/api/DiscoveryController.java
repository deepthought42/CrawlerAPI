package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
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
import com.minion.WorkManagement.WorkAllowanceStatus;
import com.minion.api.exception.PaymentDueException;
import com.minion.structs.Message;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.SubscriptionService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.StripeException;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;



/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@Controller
@RequestMapping("/discovery")
public class DiscoveryController {
	private static Logger log = LoggerFactory.getLogger(DiscoveryController.class);
    
    @Autowired
    private AccountRepository account_repo;
    
    @Autowired
    private DomainRepository domain_repo;
    
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
    	Account acct = account_repo.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	return domain_repo.getMostRecentDiscoveryRecord(url, acct.getUserId());
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
    	Account acct = account_repo.findByUserId(id);
    	
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
    	
    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
    		throw new PaymentDueException("Your plan has 0 discovered tests left. Please upgrade to run a discovery");
    	}
    	
    	Domain domain = domain_repo.findByHost(url); 

		DiscoveryRecord last_discovery_record = null;
		Date started_date = new Date(0L);
		for(DiscoveryRecord record : domain_repo.getDiscoveryRecords(url)){
			if(record.getStartTime().compareTo(started_date) > 0 && record.getDomainUrl().equals(url)){
				started_date = record.getStartTime();
				last_discovery_record = record;
			}
		}
    	

    	Date now = new Date();
    	long diffInMinutes = 10000;
    	if(last_discovery_record != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_discovery_record.getStartTime().getTime()) / (1000 * 60) ));
    	}
    	String domain_url = domain.getUrl();
    	String protocol = domain.getProtocol();
        
		if(diffInMinutes > 1440){
			//set discovery path count to 0 in case something happened causing the count to be greater than 0 for more than 24 hours
			DiscoveryRecord discovery_record = new DiscoveryRecord(now, domain.getDiscoveryBrowserName(), domain_url, now, 0, 1, 0);
        	
			acct.addDiscoveryRecord(discovery_record);
			acct = account_repo.save(acct);
			
			domain.addDiscoveryRecord(discovery_record);
			domain_repo.save(domain);
                	
			WorkAllowanceStatus.register(acct.getUsername());
			//ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("browser", domain.getDiscoveryBrowserName());
	        options.put("discovery_key", discovery_record.getKey());
	        options.put("host", domain.getUrl());
			Message<URL> message = new Message<URL>(acct.getUsername(), new URL(protocol+"://"+domain_url), options);

			ActorRef workAllocationActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());
			
		    //Fire discovery started event	
			Map<String, String> traits = new HashMap<String, String>();
	        traits.put("user_id", id);    
	        traits.put("url", url);
	    	traits.put("browser", domain.getDiscoveryBrowserName());
	        traits.put("discovery_started", "true");
	    	traits.put("discovery_key", discovery_record.getKey());
	        analytics.enqueue(TrackMessage.builder("Started Discovery")
	    		    .userId(acct.getUsername())
	    		    .properties(traits)
	    		);

			Timeout timeout = new Timeout(Duration.create(60, "seconds"));
			Future<Object> future = Patterns.ask(workAllocationActor, message, timeout);

			try {
				Await.result(future, timeout.duration());
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			
			return discovery_record;

		}
        else{
        	//Throw error indicating discovery has been or is running
        	//return new ResponseEntity<String>("Discovery is already running", HttpStatus.INTERNAL_SERVER_ERROR);
        	//Fire discovery started event	
	    	Map<String, String> discovery_started_props = new HashMap<String, String>();
	    	discovery_started_props.put("url", url);
	    	discovery_started_props.put("browser", domain.getDiscoveryBrowserName());
	    	discovery_started_props.put("already_running", "true");
	    	
	    	analytics.enqueue(TrackMessage.builder("Existing discovery found")
	    		    .userId(acct.getUsername())
	    		    .properties(discovery_started_props)
	    		);

        	throw new ExistingDiscoveryFoundException();
        }
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
	public @ResponseBody void stopWorkForAccount(HttpServletRequest request) 
			throws MalformedURLException, UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	String id = principal.getName().replace("auth0|", "");
    	Account acct = account_repo.findByUserId(id);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		WorkAllowanceStatus.haltWork(acct.getUsername()); 
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