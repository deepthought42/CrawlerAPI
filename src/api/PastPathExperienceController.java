package api;

import structs.Path;

import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
public class PastPathExperienceController {
	
	public PastPathExperienceController() {
		// TODO Auto-generated constructor stub
	}
	
    @SubscribeMapping("/topic/pathRecord")
	public @ResponseBody Path broadcastPathExperience(Path path) {
		System.err.println("BROADCASTING PATH");
		return path;
	}
}
