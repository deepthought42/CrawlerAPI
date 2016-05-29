package api;

import structs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import browsing.Page;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
public class PastPathExperienceController {
	
    private static final Logger log = Logger.getLogger(PastPathExperienceController.class);

    private static final List<SseEmitter> emitters = new ArrayList<SseEmitter>();
    
    @CrossOrigin(origins = "*")
	@RequestMapping(path = "/streamPathExperience", method = RequestMethod.GET)
    public SseEmitter stream() throws IOException {

        SseEmitter emitter = new SseEmitter();
        log.info("Adding emitter");
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));

        return emitter;
    }
	
    /**
     * Message emitter that sends path to all registered clients
     * 
     * @param path
     */
    @CrossOrigin(origins = "*")
	public static void broadcastPathExperience(Path path) {
		
		log.info("Emitters available to send to : " + emitters.size());
        emitters.forEach((SseEmitter emitter) -> {
            try {
                emitter.send(path, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                log.error("Error sending message to client");
                emitter.complete();
                emitters.remove(emitter);
                e.printStackTrace();
            }
        });
	}
}
