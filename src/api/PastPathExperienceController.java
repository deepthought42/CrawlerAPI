package api;

import structs.Path;

import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
public class PastPathExperienceController {
	
	@SendTo("/path/results")
	public Path broadcastPathExperience(Path path) {
		return path;
	}
}
