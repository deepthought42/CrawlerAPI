package com.minion.api;

import com.qanairy.models.Page;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
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

        if(emitters.containsKey(account_key)){
        	System.err.println("Marking emitter complete");
        	emitters.get(account_key).complete();
        }
        emitters.put(account_key, emitter);
        emitter.onCompletion(() -> emitters.remove(account_key));

        return emitter;
    }
	
    /**
     * Message emitter that sends path to all registered clients
     * 
     * @param test
     */
	public static void broadcastTestExperience(Test test) {
		
		log.info("Emitters available to send to : " + emitters.size());
        Iterator<String> iter = emitters.keySet().iterator();
        
        while(iter.hasNext()){
        	String acct_key = iter.next();
        	log.info("Broadcasting path to account -> "+acct_key);
        	SseEmitter emit = emitters.get(acct_key);
        	 try {
        		// test.getResult().setSrc("");
        		// test.getResult().setElements(new ArrayList<>());
        		 
        		 int idx=0;
        		 for(PathObject obj : test.getPath().getPath()){
        			 log.info("Type : "+obj.getType());
        			 if(obj instanceof Page){
        				 Page page = (Page)obj;
        				 //page.setSrc("");
        				 //page.setElements(new ArrayList<>());
        				 test.getPath().getPath().set(idx, page);
        			 }
        			 idx++;
        		 }
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
