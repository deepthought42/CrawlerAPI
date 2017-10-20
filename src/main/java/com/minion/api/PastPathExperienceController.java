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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Defines methods for emitting data to subscribed clients
 */
public class PastPathExperienceController {
	private static Logger log = LogManager.getLogger(PastPathExperienceController.class);
	
    /**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     */
	public static void broadcastTestExperience(Test test) {	
		List<PathObject> path_list = new ArrayList<PathObject>();
		Path path_clone = Path.clone(test.getPath());
		for(PathObject obj : path_clone.getPath()){

			if(obj.getType().equals("Page")){
				Page page_obj = (Page)obj;
								
				Page page;
				try {
					page = new Page("", page_obj.getUrl().toString(), page_obj.getScreenshot(), new ArrayList<PageElement>());
					path_list.add(page);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else{
				log.info("Adding pathobject to broadcast path : "+path_clone.getKey());
				path_list.add(obj);
			}
		}

		Path path = new Path(test.getPath().getKey(), test.getPath().isUseful(), test.getPath().getSpansMultipleDomains(), path_list);
		Test new_test = new Test(test.getKey(), path, test.getResult(), test.getDomain());

		try {
			Page result_page = new Page("", test.getResult().getUrl().toString(), test.getResult().getScreenshot(), new ArrayList<PageElement>());
			new_test.setResult(result_page);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

		Gson gson = new Gson();
        String test_json = gson.toJson(new_test);
        String host = null;
        try {
			host = new URL(test.getDomain().getUrl()).getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			host = test.getDomain().getUrl();
		}
		pusher.trigger(host, "test-discovered", test_json);
	}
}
