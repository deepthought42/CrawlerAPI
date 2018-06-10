package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.actors.MemoryRegistryActor;
import com.minion.api.MessageBroadcaster;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class UrlBrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(UrlBrowserActor.class.getName());

	private static Random rand = new Random();
	private UUID uuid = null;

	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	DomainRepository domain_repo;
	
	@Autowired
	PageStateRepository page_state_repo;
	
	/**
	 * Gets a random number between 0 and size
	 * @param size
	 * @return
	 */
	public int getRandomElementIndex(int size){
		return rand.nextInt(size);
	}
	
	/**
	 * Get the UUID for this Agent
	 */
	public UUID getActorId(){
		return uuid;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;

			System.err.println("Recieved data of type :: "+acct_msg.getData().getClass().getSimpleName());
			if(acct_msg.getData() instanceof URL){
				boolean test_generated_successfully = false;
				do{
					try{
						System.err.println("URL received, browser opening  ::   "+acct_msg.getOptions().get("browser").toString());
						generate_landing_page_test(acct_msg);
						break;
					}
					catch(Exception e){
						e.printStackTrace();
						log.error(e.getMessage());
						System.err.println("Failed to create landing page test");
					}
				}while(!test_generated_successfully);
		   }
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		assert path_keys != null;
		assert path_objects != null;
		
		Test test = new Test(path_keys, path_objects, result_page, "Test #"+domain.getTestCount());							
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);

		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
		
		//tell memory worker of test
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf());
		
		return test;
	}

	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(PageElement.class)){
				PageElement elem = (PageElement)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					break;
				}
			}
		}
	}

	/**
	 * Generates a landing page test based on a given URL
	 * 
	 * @param browser
	 * @param msg
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 * 
	 * @pre browser != null
	 * @pre msg != null
	 */
	public void generate_landing_page_test(Message<?> msg) throws MalformedURLException, IOException, NullPointerException{
		assert msg != null;
		
		Browser browser = new Browser(msg.getOptions().get("browser").toString());

		browser.getDriver().get(((URL)msg.getData()).toString());
		System.err.println("building page");
	  	PageState page_obj = browser.buildPage();
	  	System.err.println("Page built");
	  	browser.close();
	  	page_obj.setLandable(true);
	  	
	  	List<String> path_keys = new ArrayList<String>();
	  	path_keys.add(page_obj.getKey());
	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
	  	path_objects.add(page_obj);
	  	
	  	System.err.println("saving discovery record and domain");
		DiscoveryRecord discovery_record = discovery_repo.findByKey( msg.getOptions().get("discovery_key").toString());
		discovery_record.setLastPathRanAt(new Date());
		discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
		discovery_record.setTestCount(discovery_record.getTestCount()+1);
		//discovery_repo.save(discovery_record);

		System.err.println("Page url host :: "  + msg.getOptions().get("host").toString());
		Domain domain = domain_repo.findByUrl( msg.getOptions().get("host").toString());
		System.err.println("domain :: "+domain);
		System.err.println("Domain test count   ::   "+domain.getTestCount());
		domain.addPageState(page_state_repo.save(page_obj));
		
		for(PageElement element : page_obj.getElements()){
			try {
				MessageBroadcaster.broadcastPageElement(element, domain.getUrl() );
			} catch (JsonProcessingException e) {
			}
		}
		//domain_dao.save(domain);
		
		System.err.println("broadcasting page state");
		MessageBroadcaster.broadcastPageState(page_obj, domain.getUrl());
		MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

		System.err.println("Broadcasting discovery status");
		Test test = createTest(path_keys, path_objects, page_obj, 1L, domain, msg, discovery_record);

		Message<Test> test_msg = new Message<Test>(msg.getAccountKey(), test, msg.getOptions());

		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
		path_expansion_actor.tell(test_msg, getSelf() );

		final ActorRef form_test_discoverer = this.getContext().actorOf(Props.create(FormTestDiscoveryActor.class), "FormTestDiscoveryActor"+UUID.randomUUID());
		form_test_discoverer.tell(test_msg, getSelf() );
	}
}