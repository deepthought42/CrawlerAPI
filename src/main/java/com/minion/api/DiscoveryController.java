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
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

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

    @Autowired
    protected AccountService accountService;
    
    /**
	 * 
	 * @param request
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('start:discovery')")
	@RequestMapping(path="/start", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<String> startDiscovery(HttpServletRequest request, 
													   	  		@RequestParam(value="url", required=true) String url) 
													   	  				throws MalformedURLException, UnknownAccountException {

    	String auth_access_token = request.getHeader("Authorization").replace("Bearer ", "");
    	Auth0Client auth = new Auth0Client();
    	String username = auth.getUsername(auth_access_token);
    	String nickname = auth.getNickname(auth_access_token);

    	Account acct = accountService.find(username);
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
    	Analytics analytics = Analytics.builder("TjYM56IfjHFutM7cAdAEQGGekDPN45jI").build();
    	Map<String, String> traits = new HashMap<String, String>();
        traits.put("name", nickname);
        traits.put("email", username);        
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
    	String domain_url = domain.getUrl();
    	String protocol = domain.getProtocol();
    	Date now = new Date();
    	long diffInMinutes = 1000;
    	if(last_ran_date != null){
    		diffInMinutes = Math.abs((int)((now.getTime() - last_ran_date.getTime())/ (1000 * 60) ));
    	}
		connection.close();
        
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", domain.getDiscoveryBrowserName());
        if(diffInMinutes > 60){
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
	private static final long serialVersionUID = 7200878662560716215L;

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