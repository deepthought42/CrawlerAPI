package com.minion.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.rest.Pusher;
import com.qanairy.models.PageStatePOJO;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;
import com.qanairy.persistence.PageElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

/**
 * Defines methods for emitting data to subscribed clients
 */
public class MessageBroadcaster {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
    /**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastDiscoveredTest(Test test) throws JsonProcessingException {	
		List<PathObject> path_list = new ArrayList<PathObject>();
		for(PathObject obj : test.getPath().getPath()){
			if(obj != null && obj.getType().equals("PageState")){
				PageState page_obj = (PageState)obj;
								
				PageState page;
				try {
					page = new PageStatePOJO( "", page_obj.getUrl().toString(), page_obj.getBrowserScreenshots(), new ArrayList<PageElement>(), page_obj.isLandable());
					path_list.add(page);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(obj != null){
				path_list.add(obj);
			}
		}

		Path path = new Path(test.getPath().getKey(), test.getPath().isUseful(), test.getPath().getSpansMultipleDomains(), path_list);
		PageState result_page = null;
		try {
			result_page = new PageStatePOJO("", test.getResult().getUrl().toString(), test.getResult().getBrowserScreenshots(), new ArrayList<PageElement>());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Test new_test = new Test(test.getKey(), path, result_page, test.getDomain(), test.getName());
		new_test.setBrowserPassingStatuses(test.getBrowserPassingStatuses());
		new_test.setLastRunTimestamp(test.getLastRunTimestamp());
		new_test.setRunTime(test.getRunTime());
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

		/*Gson gson = new Gson();
        String test_json = gson.toJson(new_test);
        */

		String host = new_test.getDomain().getUrl();
        
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(new_test);
        
		pusher.trigger(host, "test-discovered", test_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastTest(Test test) throws JsonProcessingException {	
		List<PathObject> path_list = new ArrayList<PathObject>();
		Path path_clone = Path.clone(test.getPath());
		
		for(PathObject obj : path_clone.getPath()){
			if(obj != null && obj.getType().equals("PageState")){
				PageState page_obj = (PageState)obj;
								
				PageState page;
				try {
					page = new PageStatePOJO( "", page_obj.getUrl().toString(), page_obj.getBrowserScreenshots(), new ArrayList<PageElement>(), page_obj.isLandable());
					path_list.add(page);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(obj != null){
				path_list.add(obj);
			}
		}

		Path path = new Path(test.getPath().getKey(), test.getPath().isUseful(), test.getPath().getSpansMultipleDomains(), path_list);
		Test new_test = new Test(test.getKey(), path, test.getResult(), test.getDomain(), test.getName());
		new_test.setBrowserPassingStatuses(test.getBrowserPassingStatuses());
		new_test.setLastRunTimestamp(test.getLastRunTimestamp());
		new_test.setRunTime(test.getRunTime());
		try {
			PageState result_page = new PageStatePOJO("", test.getResult().getUrl().toString(), test.getResult().getBrowserScreenshots(), new ArrayList<PageElement>());
			new_test.setResult(result_page);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

		String host = new_test.getDomain().getUrl();
        
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(new_test);
        
		pusher.trigger(host, "test-run", test_json);
	}
	
	/**
     * Message emitter that sends {@link TestStatus to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastTestStatus(String host, TestRecord record) throws JsonProcessingException {
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        
        String test_json = mapper.writeValueAsString(record);
        
		pusher.trigger(host, "test-status", test_json);
	}
	
	/**
     * Message emitter that sends {@link DiscoveryRecord} to all registered clients
     * 
     * @param record {@link Record} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastDiscoveryStatus(String host, DiscoveryRecord record) throws JsonProcessingException {
		
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String discovery_json = mapper.writeValueAsString(record);
        
		pusher.trigger(host, "discovery-status", discovery_json);
	}
}
