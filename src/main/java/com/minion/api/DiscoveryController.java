package com.minion.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.auth0.spring.security.api.Auth0UserDetails;
import com.minion.WorkManagement.WorkAllowanceStatus;
import com.minion.actors.WorkAllocationActor;
import com.minion.structs.Message;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.dto.exceptions.UnknownAccountException;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.services.AccountService;
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
@RestController
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
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(method = RequestMethod.GET)
    @Deprecated
	public @ResponseBody ResponseEntity<String> startWork(HttpServletRequest request, 
													   	  @RequestParam(value="url", required=true) String url,
													   	  @RequestParam(value="browser", required=true) String browser) 
															   throws MalformedURLException, UnknownAccountException {
		
		//THIS SHOULD BE REPLACED WITH AN ACTUAL ACCOUNT ID ONCE AUTHENTICATION IS IMPLEMENTED
		//String account_key = ""+UUID.randomUUID().toString();
		
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
    	
    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
		
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
		options.put("browser", browser);
        if(diffInMinutes > 60){
			WorkAllowanceStatus.register(acct.getKey()); 
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Message<URL> message = new Message<URL>(acct.getKey(), new URL(protocol+"://"+domain_url), options);
			ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor");
			//workAllocationActor.tell(message, ActorRef.noSender());
			Timeout timeout = new Timeout(Duration.create(10, "seconds"));
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
        	log.info("Account: " + acct.getKey() + " attempted to run discovery " + diffInMinutes + " minutes of last discovery" );
        	//return new ResponseEntity<String>("Discovery is already running", HttpStatus.INTERNAL_SERVER_ERROR);
        	throw new ExistingDiscoveryFoundException();
        }
        

	}

    /**
	 * 
	 * @param request
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('user') or hasAuthority('qanairy')")
	@RequestMapping(path="/start", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<String> startDiscovery(HttpServletRequest request, 
													   	  @RequestParam(value="url", required=true) String url,
													   	  @RequestParam(value="browsers", required=true) List<String> browsers) 
															   throws MalformedURLException, UnknownAccountException {
    	
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
    	
    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
		
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
		options.put("browsers", browsers);
        if(diffInMinutes > 60){
        	DiscoveryRecord discovery_record = new DiscoveryRecord(now, "chrome");
        	acct.getDiscoveryRecords().add(discovery_record);
        	
			WorkAllowanceStatus.register(acct.getKey()); 
			ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
			Message<URL> message = new Message<URL>(acct.getKey(), new URL(protocol+"://"+domain_url), options);
			ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor");
			//workAllocationActor.tell(message, ActorRef.noSender());
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
        	log.info("Account: " + acct.getKey() + " attempted to run discovery " + diffInMinutes + " minutes of last discovery" );
        	//return new ResponseEntity<String>("Discovery is already running", HttpStatus.INTERNAL_SERVER_ERROR);
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
    @PreAuthorize("hasAuthority('qanairy')")
	@RequestMapping("/stop")
	public @ResponseBody WorkAllocationActor stopWorkForAccount(HttpServletRequest request) 
			throws MalformedURLException, UnknownAccountException {
		
    	final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
    	
    	Account acct = accountService.find(currentUser.getUsername());
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