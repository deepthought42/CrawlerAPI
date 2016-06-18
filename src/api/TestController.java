package api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import actors.TestCoordinatorActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import structs.Path;
import tester.Test;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/test")
public class TestController {

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody TestCoordinatorActor startTestingCoordinator(@RequestParam(value="url", required=true) String url) {
//		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();

		//String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		System.out.print("INITIALIZING TESTING ACTOR...");
		//System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		ActorSystem actor_system = ActorSystem.create("TesterSystem");
				
		ActorRef testingCoordinatorActor = actor_system.actorOf(Props.create(TestCoordinatorActor.class), "browserActor");
		testingCoordinatorActor.tell(new Path(), ActorRef.noSender());
		
		return null;
	}

	@RequestMapping(method = RequestMethod.PUT);
	public @ResponseBody Test updateTestCorrectness(@RequestParam(value="test", required=true) String test){
		Memory
		//Find test
		//update test correctness
		// return test
	}

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class TestCoordinatorNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716215L;

	public TestCoordinatorNotFoundException() {
		super("could not find user .");
	}
}
