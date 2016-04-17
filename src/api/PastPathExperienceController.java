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
	
    @CrossOrigin(origins = "*")
	public static Path broadcastPathExperience(Path path) {
		log.info("Got message" + path);
		log.info("Emitters available to send to : " + emitters.size());
        emitters.forEach((SseEmitter emitter) -> {
            try {
                log.info("Sending message to client");

                emitter.send(path, MediaType.APPLICATION_JSON);
                log.info("Sent message to client");
            } catch (IOException e) {
                log.info("Error sending message to client");
                emitter.complete();
                emitters.remove(emitter);
                e.printStackTrace();
            }
        });
       
		return path;
	}
}
