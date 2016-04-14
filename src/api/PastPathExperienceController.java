package api;

import structs.Path;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
@RequestMapping("/experience")
public class PastPathExperienceController {
	
	@CrossOrigin(origins = "http://localhost:8000")
    @MessageMapping("/path")
	@SendTo("/pathRecord")
	public static Path broadcastPathExperience(Path path) {
		return path;
	}
}
