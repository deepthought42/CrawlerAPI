package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import com.qanairy.api.exceptions.FreeTrialExpiredException;
import com.qanairy.api.exceptions.MissingSubscriptionException;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.DiscoveryRecordPOJO;
import com.qanairy.models.StripeClient;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.impl.AccountDaoImpl;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;

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
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DiscoveryController.class);

    @Autowired
    protected AccountService accountService;
    
    private StripeClient stripeClient;

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

    	Account acct = accountService.find(username);

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
										   	  				throws MalformedURLException, UnknownAccountException, DiscoveryLimitReachedException, AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
		Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();

    	Account acct = accountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	else if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Subscription subscription = stripeClient.getSubscription(acct.getSubscriptionToken());
    	Plan plan = subscription.getPlan();

    	if(subscription.getTrialEnd() < (new Date()).getTime()/1000){
    		throw new FreeTrialExpiredException();
    	}
    	
    	String plan_name = plan.getId();
    	int disc_index = plan_name.indexOf("-disc");
    	int allowed_discovery_cnt = Integer.parseInt(plan_name.substring(0, disc_index));
    	
    	int monthly_discovery_count = 0;
    	//check if account has exceeded allowed discovery threshold
    	for(DiscoveryRecord record : acct.getDiscoveryRecords()){
    		Calendar cal = Calendar.getInstance(); 
    		cal.setTime(record.getStartTime()); 
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);
   
    		Calendar c = Calendar.getInstance();
    		int month_now = c.get(Calendar.MONTH);
    		int year_now = c.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_discovery_count++;
    		}
    	}
    	
		DiscoveryRecord last_discovery_record = null;

    	if(monthly_discovery_count > allowed_discovery_cnt){
        	Map<String, String> traits = new HashMap<String, String>();
            traits.put("email", username);     
            traits.put("discovery_limit_reached", plan.getId());
        	analytics.enqueue(IdentifyMessage.builder()
        		    .userId(acct.getKey())
        		    .traits(traits)
        		);
        	
        	throw new DiscoveryLimitReachedException();
    	}
    	else{
    		Date started_date = new Date(0L);
    		for(DiscoveryRecord record : acct.getDiscoveryRecords()){
    			if(record.getStartTime().compareTo(started_date) > 0 && record.getDomainUrl().equals(url)){
    				started_date = record.getStartTime();
    				last_discovery_record = record;
    			}
    		}
    	}
    	
    	DomainDao domain_dao = new DomainDaoImpl();
    	Domain domain = domain_dao.find(url); 

    	Date now = new Date();
    	long diffInMinutes = 10000;
    	if(last_discovery_record != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_discovery_record.getStartTime().getTime()) / (1000 * 60) ));
    	}
    	String domain_url = domain.getUrl();
    	String protocol = domain.getProtocol();
        
		if(diffInMinutes > 1440){
        	//set discovery path count to 0 in case something happened causing the count to be greater than 0 for more than 24 hours
				
			DiscoveryRecord discovery_record = new DiscoveryRecordPOJO(now, domain.getDiscoveryBrowserName(), domain_url, now, 0, 1, 0);
        	acct.getDiscoveryRecords().add(discovery_record);
        	
        	AccountDao acct_dao = new AccountDaoImpl();
        	acct_dao.save(acct);
                	
			WorkAllowanceStatus.register(acct.getKey());
			DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("browser", domain.getDiscoveryBrowserName());
	        options.put("discovery_key", discovery_record.getKey());

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

    	Account acct = accountService.find(username);
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