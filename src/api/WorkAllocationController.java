package api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import actors.ResourceManagementActor;
import actors.WorkAllocationActor;


/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/workAllocation")
public class WorkAllocationController {

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody WorkAllocationActor startWorkAllocator(@RequestParam(value="url", required=true) String url) {
		ResourceManagementActor resourceManager = new ResourceManagementActor(5);
//		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();

		//String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		System.out.print("INITIALIZING ACTOR...");
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		
		System.out.print("Initializing page monitor...");
		WorkAllocationActor workAllocator = new WorkAllocationActor(resourceManager, url);
		
		return workAllocator;
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
