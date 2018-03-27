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
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.minion.WorkManagement.WorkAllowanceStatus;
import com.minion.actors.WorkAllocationActor;
import com.minion.structs.Message;
import com.qanairy.auth.Auth0Client;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
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
    
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping(path="/status", method = RequestMethod.GET)
    public @ResponseBody Boolean isDiscoveryRunning(HttpServletRequest request, 
    												@RequestParam(value="url", required=true) String url) throws UnknownAccountException{
    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	
    	OrientConnectionFactory connection = new OrientConnectionFactory();

    	@SuppressWarnings("unchecked")
		Iterator<IDomain> domains_iter = ((Iterable<IDomain>) DataAccessObject.findByKey(url, connection, IDomain.class)).iterator();
    	IDomain domain = domains_iter.next();
    	domain.setDiscoveryStartTime(new Date());
    	Date last_ran_date = domain.getLastDiscoveryPathRanAt();

    	Date now = new Date();
    	long diffInMinutes = 1000;
    	if(last_ran_date != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_ran_date.getTime())/ (1000 * 60) ));
    	}

    	int paths_being_explored = domain.getDiscoveryPathCount();
        
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", domain.getDiscoveryBrowserName());
        
		if(paths_being_explored == 0 || diffInMinutes > 1440){
			return false;
		}
		else{
			return true;
		}
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
	public @ResponseBody ResponseEntity<String> startDiscovery(HttpServletRequest request, 
													   	  		@RequestParam(value="url", required=true) String url) 
													   	  				throws MalformedURLException, UnknownAccountException, DiscoveryLimitReachedException, AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);

    	Account acct = accountService.find(username);

    	if(acct == null){
    		throw new UnknownAccountException();
    	}

    	
    	int monthly_discovery_count = 0;
    	//check if account has exceeded allowed discovery threshold
    	for(DiscoveryRecord record : acct.getDiscoveryRecords()){
    		Calendar cal = Calendar.getInstance(); 
    		cal.setTime(record.getStartedAt()); 
    		int month_started = cal.get(Calendar.MONTH);
    		int year_started = cal.get(Calendar.YEAR);
   
    		Calendar c = Calendar.getInstance();
    		int month_now = c.get(Calendar.MONTH);
    		int year_now = c.get(Calendar.YEAR);

    		if(month_started == month_now && year_started == year_now){
    			monthly_discovery_count++;
    		}
    	}
    	    	
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("email", username);     
        traits.put("discovery_started", "true");
    	analytics.enqueue(IdentifyMessage.builder()
    		    .userId(acct.getKey())
    		    .traits(traits)
    		);
    	
    	OrientConnectionFactory connection = new OrientConnectionFactory();

    	@SuppressWarnings("unchecked")
		Iterator<IDomain> domains_iter = ((Iterable<IDomain>) DataAccessObject.findByKey(url, connection, IDomain.class)).iterator();
    	IDomain domain = domains_iter.next();
    	domain.setDiscoveryStartTime(new Date());
    	Date last_ran_date = domain.getLastDiscoveryPathRanAt();

    	Date now = new Date();
    	long diffInMinutes = 1000;
    	if(last_ran_date != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_ran_date.getTime())/ (1000 * 60) ));
    	}
    	String domain_url = domain.getUrl();
    	String protocol = domain.getProtocol();
    	int paths_being_explored = domain.getDiscoveryPathCount();
        
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", domain.getDiscoveryBrowserName());
        
		if(paths_being_explored == 0 || diffInMinutes > 1440){
        	//set discovery path count to 0 in case something happened causing the count to be greater than 0 for more than 24 hours
        	domain.setDiscoveryPathCount(0);
				
			DiscoveryRecord discovery_record = new DiscoveryRecord(now, "chrome", domain_url);
        	acct.getDiscoveryRecords().add(discovery_record);
        	
        	AccountRepository acct_repo = new AccountRepository();
        	acct_repo.save(connection, acct);
                	
			WorkAllowanceStatus.register(acct.getKey());

			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Message<URL> message = new Message<URL>(acct.getKey(), new URL(protocol+"://"+domain_url), options);
			ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor");
			
		    //Fire discovery started event	
	    	Map<String, String> discovery_started_props = new HashMap<String, String>();
	    	discovery_started_props.put("url", url);
	    	discovery_started_props.put("browser", domain.getDiscoveryBrowserName());
	    	analytics.enqueue(TrackMessage.builder("Started Discovery")
	    		    .userId(acct.getKey())
	    		    .properties(discovery_started_props)
	    		);
			
			Timeout timeout = new Timeout(Duration.create(30, "seconds"));
			Future<Object> future = Patterns.ask(workAllocationActor, message, timeout);
			connection.close();

			try {
				Await.result(future, timeout.duration());
				return new ResponseEntity<String>(HttpStatus.OK);
	
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
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
			connection.close();

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

		WorkAllowanceStatus.haltWork(acct.getKey()); 
		
		return null;
	}

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class WorkAllocatorNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716214L;

	public WorkAllocatorNotFoundException() {
		super("could not find user .");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class ExistingDiscoveryFoundException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716215L;

	public ExistingDiscoveryFoundException() {
		super("Discovery is already running");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class DiscoveryLimitReachedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public DiscoveryLimitReachedException() {
		super("Discovery limit reached. Upgrade your account now!");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class FreeTrialEndedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public FreeTrialEndedException() {
		super("Your free trial has ended. Select a package now!");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class PaymentDueException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public PaymentDueException() {
		super("There was an issue processing your payment. Please update your payment details.");
	}
}