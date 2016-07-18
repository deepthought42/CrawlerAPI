package com.minion.api.auth;

import com.minion.structs.Path;
import com.minion.tester.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller that defines endpoints for user authentication
 * 
 * @author Brandon Kindred
 */
@Controller
public class AuthenticationController {
	
    private static final Logger log = Logger.getLogger(AuthenticationController.class);

    private static final Map<String, SseEmitter> emitters = new HashMap<String, SseEmitter>();
    
    @CrossOrigin(origins = "*")
	@RequestMapping(path = "/login", method = RequestMethod.POST)
    public String stream(@RequestParam(value="username", required=true) String username,
    						 @RequestParam(value="password", required=true) String password,
    						 @RequestParam(value="remember_me", required=true) String remember_me) throws IOException {
    	
        
        return emitter;
    }
	
    /**
     * Message emitter that sends path to all registered clients
     * 
     * @param path
     */
    @CrossOrigin(origins = "*")
	public static void broadcastTestExperience(Test test) {
		
		log.info("Emitters available to send to : " + emitters.size());
        Iterator<String> iter = emitters.keySet().iterator();
        
        while(iter.hasNext()){
        	String acct_key = iter.next();
        	log.info("Broadcasting path to account -> "+acct_key);
        	SseEmitter emit = emitters.get(acct_key);
        	 try {
                 emit.send(test, MediaType.APPLICATION_JSON);
             } catch (IOException e) {
                 log.error("Error sending message to client");
                 emit.complete();
                 emitters.remove(acct_key);
                 e.printStackTrace();
             }
        }
	}
    
    /**
     * Message emitter that sends path to all registered clients
     * 
     * @param path
     */
    @CrossOrigin(origins = "*")
	public static void broadcastPathExperience(Path path) {
		
		log.info("Emitters available to send to : " + emitters.size());
        Iterator<String> iter = emitters.keySet().iterator();
        
        while(iter.hasNext()){
        	String acct_key = iter.next();
        	log.info("Broadcasting path to account -> "+acct_key);
        	SseEmitter emit = emitters.get(acct_key);
        	 try {
                 emit.send(path, MediaType.APPLICATION_JSON);
             } catch (IOException e) {
                 log.error("Error sending message to client");
                 emit.complete();
                 emitters.remove(acct_key);
                 e.printStackTrace();
             }
        }
	}

}
