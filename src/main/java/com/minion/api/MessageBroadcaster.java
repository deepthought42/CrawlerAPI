package com.minion.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.rest.Pusher;
import com.qanairy.dto.TestCreatedDto;
import com.qanairy.dto.TestDto;
import com.qanairy.dto.TestRecordDto;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines methods for emitting data to subscribed clients
 */
public class MessageBroadcaster {
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private static Pusher pusher = new Pusher("402026", "77fec1184d841b55919e", "5bbe37d13bed45b21e3a");
	
	static{
		pusher.setCluster("us2");
		pusher.setEncrypted(true);
	}
	
    /**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastDiscoveredTest(Test test, String host) throws JsonProcessingException {	
        //Object to JSON in String        
        ObjectMapper mapper = new ObjectMapper();
        String test_json = mapper.writeValueAsString(test);

		pusher.trigger(host, "test-discovered", test_json);
	}

    /**
     * Message emitter that sends {@link Form} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastDiscoveredForm(Form form, String host) throws JsonProcessingException {	
		log.info("Broadcasting discovered form !!!");
		
        //Object to JSON in String        
        ObjectMapper mapper = new ObjectMapper();
        String form_json = mapper.writeValueAsString(form);
        log.info("host ::   "+host);
		pusher.trigger(host.trim(), "discovered-form", form_json);
		log.info("broadcasted a discovered form");
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastTest(Test test, String host) throws JsonProcessingException {	
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(test);
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("host :: " + host);
        log.warn("TEST JSON :: " + test_json);
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		
        pusher.trigger(host, "test", test_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastPathObject(PathObject path_object, String host) throws JsonProcessingException {	
        ObjectMapper mapper = new ObjectMapper();
        //Object to JSON in String
        String path_object_json = mapper.writeValueAsString(path_object);
        
		pusher.trigger(host, "path_object", path_object_json);
	}
	
	/**
     * Message emitter that sends {@link TestStatus to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastTestStatus(String host, TestRecord record, Test test) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        
        String test_json = mapper.writeValueAsString(new TestRecordDto(record, test.getKey()));
        
		pusher.trigger(host, "test-run", test_json);
	}
	
	/**
     * Message emitter that sends {@link DiscoveryRecord} to all registered clients
     * 
     * @param record {@link DiscoveryRecord} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public static void broadcastDiscoveryStatus(DiscoveryRecord record) throws JsonProcessingException {
		log.info("broadcasting discovery status");
		
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String discovery_json = mapper.writeValueAsString(record);
        
		pusher.trigger(record.getDomainUrl(), "discovery-status", discovery_json);
	}

	/**
	 * Uses Pusher service to broadcast json representing {@link Test} to browser extension for the user with
	 *   the given username
	 * @param test_dto representation of {@link Test} that complies with format for browser extensions
	 * @param username username of User
	 * @throws JsonProcessingException
	 */
	public static void broadcastIdeTest(TestDto test_dto, String username) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(test_dto);
        
		pusher.trigger(username, "edit-test", test_json);
	}

	public static void broadcastTestCreatedConfirmation(Test test, String username) throws JsonProcessingException {
		TestCreatedDto test_created_dto = new TestCreatedDto(test);
		
        ObjectMapper mapper = new ObjectMapper();
		String test_confirmation_json = mapper.writeValueAsString(test_created_dto);
		pusher.trigger(username, "test-created", test_confirmation_json);
	}
}
