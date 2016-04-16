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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * 
 * @author Brandon Kindred
 */
@Controller
public class PastPathExperienceController {
	
    private static final Logger log = Logger.getLogger(PastPathExperienceController.class);

    private final List<SseEmitter> emitters = new ArrayList<SseEmitter>();
    
    @CrossOrigin(origins = "*")
	@RequestMapping(path = "/streamPathExperience", method = RequestMethod.GET)
    public SseEmitter stream() throws IOException {

        SseEmitter emitter = new SseEmitter();

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));

        return emitter;
    }
	
    @CrossOrigin(origins = "*")
	public Path broadcastPathExperience(Path path) {
		log.info("Got message" + path);

        emitters.forEach((SseEmitter emitter) -> {
            try {
                emitter.send(path, MediaType.APPLICATION_JSON);
                log.info("Sent message to client");
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
                e.printStackTrace();
            }
        });
       
		return path;
	}
}
