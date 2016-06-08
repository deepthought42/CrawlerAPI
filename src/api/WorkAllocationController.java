package api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import WorkManagement.WorkAllowanceStatus;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

import actors.WorkAllocationActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import structs.Message;
import structs.SessionSequenceTracker;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@CrossOrigin(origins = "http://localhost:8001")
@RequestMapping("/work")
public class WorkAllocationController {

	/**
	 * 
	 * @param request
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody WorkAllocationActor startWork(HttpServletRequest request, @RequestParam(value="url", required=true) String url, @RequestParam(value="account_key", required=true) String account_key) throws MalformedURLException {
		
//		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();

		//String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		
		//THIS SHOULD BE REPLACED WITH AN ACTUAL ACCOUNT ID ONCE AUTHENTICATION IS IMPLEMENTED
		//String account_key = ""+UUID.randomUUID().toString();
		//System.out.println("ACCOUNT KEY :: "+account_key);
		SessionSequenceTracker seqTracker = SessionSequenceTracker.getInstance();
		seqTracker.addSessionSequences("SESSION_KEY_HERE");
		
		WorkAllowanceStatus.register(account_key); 
		System.out.println("WORK ALLOWANCE STATUS :: "+WorkAllowanceStatus.checkStatus(account_key));
		
		System.out.print("Compiling work to be allocated to work allocator...");

		ActorSystem actor_system = ActorSystem.create("MinionActorSystem");
		Message<URL> message = new Message<URL>(account_key, new URL(url));
		ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor");
		workAllocationActor.tell(message, ActorRef.noSender());
	
		return null;
	}

	/**
	 * 
	 * @param request
	 * @param account_key key of account to stop work for
	 * @return
	 * @throws MalformedURLException
	 */
	@RequestMapping("/stop")
	public @ResponseBody WorkAllocationActor stopWorkForAccount(HttpServletRequest request, @RequestParam(value="account_key", required=true) String account_key) throws MalformedURLException {
		
		System.out.println("STOP! ACCOUNT KEY :: "+account_key);

		WorkAllowanceStatus.haltWork(account_key); 
		System.out.println("WORK ALLOWANCE STATUS :: "+WorkAllowanceStatus.checkStatus(account_key));
		
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
