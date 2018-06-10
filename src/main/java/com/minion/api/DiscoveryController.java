package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.minion.actors.WorkAllocationActor;
import com.minion.structs.Message;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.StripeClient;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.UsageRecord;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@Controller
@RequestMapping("/discovery")
public class DiscoveryController {
	private static Logger log = LoggerFactory.getLogger(DiscoveryController.class);
    
    private StripeClient stripeClient;

    @Autowired
    AccountRepository account_repo;
    
    @Autowired
    DomainRepository domain_repo;
    
    @Autowired
    DiscoveryRecordRepository discovery_repo; 
    
    @Autowired
    DiscoveryController(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
	@RequestMapping(path="/status", method = RequestMethod.GET)
    public @ResponseBody DiscoveryRecord isDiscoveryRunning(HttpServletRequest request, 
    												@RequestParam(value="url", required=true) String url) 
    														throws UnknownAccountException{
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

    	DiscoveryRecord last_discovery_record = null;
    	Date last_ran_date = new Date(0L);
		for(DiscoveryRecord record : acct.getDiscoveryRecords()){
			if(record.getStartTime().compareTo(last_ran_date) > 0 && record.getDomainUrl().equals(url)){
				last_ran_date = record.getStartTime();
				last_discovery_record = record;
			}
		}

		return last_discovery_record;
    }
	
    /**
	 * 
	 * @param request
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException 
     * @throws APIException 
     * @throws CardException 
     * @throws APIConnectionException 
     * @throws InvalidRequestException 
     * @throws AuthenticationException 
	 */
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping(path="/start", method = RequestMethod.GET)
	public @ResponseBody DiscoveryRecord startDiscovery(HttpServletRequest request, 
											   	  		@RequestParam(value="url", required=true) String url) 
										   	  				throws MalformedURLException, 
										   	  						UnknownAccountException, 
										   	  						DiscoveryLimitReachedException, 
										   	  						AuthenticationException, 
										   	  						InvalidRequestException, 
										   	  						APIConnectionException, 
										   	  						CardException, APIException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
		Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();

    	Account acct = account_repo.findByUsername(username);
    	
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	Subscription subscription = stripeClient.getSubscription(acct.getSubscriptionToken());
    	String subscription_item = null;
    	for(SubscriptionItem item : subscription.getSubscriptionItems().getData()){
    		if(item.getPlan().getNickname().equals("Discovery")){
    			subscription_item = item.getId();
    		}
    	}
    	
    	if(subscription_item==null){
    		throw new MissingDiscoveryPlanException();
    	}
    	
		DiscoveryRecord last_discovery_record = null;
		Date started_date = new Date(0L);
		for(DiscoveryRecord record : acct.getDiscoveryRecords()){
			if(record.getStartTime().compareTo(started_date) > 0 && record.getDomainUrl().equals(url)){
				started_date = record.getStartTime();
				last_discovery_record = record;
			}
		}
    	
    	Domain domain = domain_repo.findByUrl(url); 

    	Date now = new Date();
    	long diffInMinutes = 10000;
    	if(last_discovery_record != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_discovery_record.getStartTime().getTime()) / (1000 * 60) ));
    	}
    	String domain_url = domain.getUrl();
    	String protocol = domain.getProtocol();
        
		if(diffInMinutes > 1440){
			Date date = new Date();
			long date_millis = date.getTime();
			Map<String, Object> usageRecordParams = new HashMap<String, Object>();
	    	usageRecordParams.put("quantity", 1);
	    	usageRecordParams.put("timestamp", date_millis/1000);
	    	usageRecordParams.put("subscription_item", subscription_item);
	    	usageRecordParams.put("action", "increment");

	    	UsageRecord.create(usageRecordParams, null);
        	//set discovery path count to 0 in case something happened causing the count to be greater than 0 for more than 24 hours
			DiscoveryRecord discovery_record = new DiscoveryRecord(now, domain.getDiscoveryBrowserName(), domain_url, now, 0, 1, 0);
        	
	    	acct = account_repo.findByUsername(username);
			acct.addDiscoveryRecord(discovery_repo.save(discovery_record));
        	//AccountDao acct_dao = new AccountDaoImpl();
        	//acct_dao.save(acct);
                	
			WorkAllowanceStatus.register(acct.getKey());
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("browser", domain.getDiscoveryBrowserName());
	        options.put("discovery_key", discovery_record.getKey());
	        options.put("host", domain.getUrl());
	        
			Message<URL> message = new Message<URL>(acct.getKey(), new URL(protocol+"://"+domain_url), options);
			ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor"+UUID.randomUUID());

		    //Fire discovery started event	
			Map<String, String> traits = new HashMap<String, String>();
	        traits.put("email", username);    
	        traits.put("url", url);
	    	traits.put("browser", domain.getDiscoveryBrowserName());
	        traits.put("discovery_started", "true");
	    	traits.put("discovery_key", discovery_record.getKey());
	        analytics.enqueue(TrackMessage.builder("Started Discovery")
	    		    .userId(acct.getKey())
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
	    		    .userId(acct.getKey())
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
	public @ResponseBody WorkAllocationActor stopWorkForAccount(HttpServletRequest request) 
			throws MalformedURLException, UnknownAccountException {
		
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = account_repo.findByUsername(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}

		WorkAllowanceStatus.haltWork(acct.getKey()); 
		
		return null;
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

@ResponseStatus(HttpStatus.CONFLICT)
class PaymentDueException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public PaymentDueException() {
		super("There was an issue processing your payment. Please update your payment details");
	}
}