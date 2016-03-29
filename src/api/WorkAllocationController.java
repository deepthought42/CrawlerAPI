package api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.http.HttpStatus;

import actors.BrowserActor;
import actors.ResourceManagementActor;
import actors.WorkAllocationActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import structs.Path;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/workAllocation")
public class WorkAllocationController {

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody WorkAllocationActor startWorkAllocator(@RequestParam(value="url", required=true) String url) throws MalformedURLException {

//		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();

		//String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		System.out.print("INITIALIZING ACTOR...");
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		ActorSystem actor_system = ActorSystem.create("ActorSystem");
		System.out.print("Initializing page monitor...");
		//WorkAllocationActor workAllocator = new WorkAllocationActor(actor_system, resourceManager, url);
		
		ActorRef workAllocationActor = actor_system.actorOf(Props.create(WorkAllocationActor.class), "workAllocationActor");
		workAllocationActor.tell(new URL(url), ActorRef.noSender());

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
