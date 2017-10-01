package com.minion.api;

import com.google.gson.Gson;
import com.pusher.rest.Pusher;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        System.out.println("Adding emitter");

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
		
		List<PathObject> path_list = new ArrayList<PathObject>();
		System.out.println("Broadcasting test...");
		for(PathObject obj : test.getPath().getPath()){
			if(obj.getType().equals("Page")){
				System.out.println("Adding page to broadcast path");
				Page page_obj = (Page)obj;
								
				Page page;
				try {
					page = new Page("", page_obj.getUrl().toString(), page_obj.getScreenshot(), new ArrayList<PageElement>());
					path_list.add(page);
					System.err.println("Page added... src :: "+page.getSrc());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				System.out.println("Adding pathobject to broadcast path");
				path_list.add(obj);
			}
		}

		System.out.println("Setting Path list");
		Path path = new Path(test.getPath().getKey(), test.getPath().isUseful(), test.getPath().getSpansMultipleDomains(), path_list);
		test.setPath(path);

		try {
			Page result_page = new Page("", test.getResult().getUrl().toString(), test.getResult().getScreenshot(), new ArrayList<PageElement>());
			test.setResult(result_page);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

		Gson gson = new Gson();
        String test_json = gson.toJson(test);
        String host = null;
        try {
			host = new URL(test.getDomain().getUrl()).getHost();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			host = test.getDomain().getUrl();
		}
		System.out.println("test domain url :: "+test.getDomain().getUrl());
		System.out.println("test domain url host :: " + host + " :: DATA :: "+test_json);
		pusher.trigger(host, "test-discovered", test_json);
	}
}
