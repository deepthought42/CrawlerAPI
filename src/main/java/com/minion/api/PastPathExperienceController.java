package com.minion.api;

import com.google.gson.Gson;
import com.pusher.rest.Pusher;
import com.qanairy.models.Test;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 */
@CrossOrigin(origins = "http://alpha.qanairy.com")
@RestController
public class PastPathExperienceController {
	private static Logger log = LogManager.getLogger(PastPathExperienceController.class);

    private static final Map<String, SseEmitter> emitters = new HashMap<String, SseEmitter>();
    
    @PreAuthorize("hasAuthority('qanairy')")
	@RequestMapping("/realtime/streamPathExperience" )
    public SseEmitter stream(HttpServletRequest request,
    		 				 @RequestParam(value="account_key", required=true) String account_key,
    		 				 Principal principal) throws IOException {
		SseEmitter emitter = new SseEmitter();
        System.err.println("Adding emitter");

        //find emitter for account before broadcasting data
        if(!emitters.containsKey(account_key)){
        	emitters.put(account_key, emitter);
        }
        emitter.onCompletion(() -> emitters.remove(account_key));

        return emitter;
    }
	
    /**
     * Message emitter that sends path to all registered clients
     * 
     * @param test
     */
	public static void broadcastTestExperience(Test test) {
		Pusher pusher = new Pusher("384928", "5103e64528e1579e78e3", "33cc2853b73ba2d0befb");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

		Gson gson = new Gson();
        String test_json = gson.toJson(test);
		pusher.trigger(test.getDomain().getUrl(), "test-discovered ", Collections.singletonMap("message", test_json));
		
	}
}
