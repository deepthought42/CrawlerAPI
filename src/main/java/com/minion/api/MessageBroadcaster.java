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
	public static void broadcastDiscoveredTest(Test test, String host) throws JsonProcessingException {	
		List<PathObject> path_list = new ArrayList<PathObject>();
		for(PathObject obj : test.getPathObjects()){
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

		/*PageState result_page = null;
		try {
			result_page = new PageStatePOJO("", test.getResult().getUrl().toString(), test.getResult().getBrowserScreenshots(), new ArrayList<PageElement>());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Test new_test = new TestPOJO(test.getPathKeys(), test.getPathObjects(), result_page, test.getName());
		new_test.setBrowserStatuses(test.getBrowserStatuses());
		new_test.setLastRunTimestamp(test.getLastRunTimestamp());
		new_test.setRunTime(test.getRunTime());
		new_test.setCorrect(test.getCorrect());
		new_test.setRecords(test.getRecords());
		new_test.setSpansMultipleDomains(test.getSpansMultipleDomains());
*/
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);

        //Object to JSON in String        
        ObjectMapper mapper = new ObjectMapper();
        String test_json = mapper.writeValueAsString(test);

		pusher.trigger(host, "test-discovered", test_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastTest(Test test, String host) throws JsonProcessingException {	
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);
        
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(test);
        
		pusher.trigger(host, "test", test_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastPageElement(PageElement page_element, String host) throws JsonProcessingException {	
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);
        
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String page_element_json = mapper.writeValueAsString(page_element);
        
		pusher.trigger(host, "page_element", page_element_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastPageState(PageState page_state, String host) throws JsonProcessingException {	
		Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
		pusher.setCluster("us2");
		pusher.setEncrypted(true);
        
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String page_state_json = mapper.writeValueAsString(page_state);
        
		pusher.trigger(host, "page_state", page_state_json);
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
