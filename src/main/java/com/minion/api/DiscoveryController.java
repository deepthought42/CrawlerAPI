package com.minion.api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

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
import com.qanairy.models.dto.exceptions.UnknownAccountException;
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
@CrossOrigin(origins = "http://localhost:8001")
@RequestMapping("/discovery")
public class DiscoveryController {

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
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<String> startWork(HttpServletRequest request, 
													   @RequestParam(value="url", required=true) String url) 
															   throws MalformedURLException, UnknownAccountException {
		
		//ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();
		//THIS SHOULD BE REPLACED WITH AN ACTUAL ACCOUNT ID ONCE AUTHENTICATION IS IMPLEMENTED
		//String account_key = ""+UUID.randomUUID().toString();
		
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Auth0UserDetails currentUser = (Auth0UserDetails) authentication.getPrincipal();
    	
    	Account acct = accountService.find(currentUser.getUsername());
    	if(acct == null){
    		throw new UnknownAccountException();
    	}
		
		WorkAllowanceStatus.register(acct.getKey()); 

		ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
		Message<URL> message = new Message<URL>(acct.getKey(), new URL(url));
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

	/**
	 * 
	 * @param request
	 * @param account_key key of account to stop work for
	 * @return
	 * @throws MalformedURLException
	 * @throws UnknownAccountException 
	 */
    @PreAuthorize("hasAuthority('trial') or hasAuthority('qanairy')")
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
