package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.config.SpringExtension;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestStatus;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.TestRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.TestService;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
@Component
@Scope("prototype")
public class BrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(BrowserActor.class.getName());

	private static Random rand = new Random();
	private UUID uuid = null;

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private DomainRepository domain_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;

	@Autowired
	private TestRepository test_repo;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private TestService test_service;
	
	@Autowired
	private Crawler crawler;
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

			Browser browser = null;
			if (acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();
				assert(test.getPathKeys() != null);
				assert(!test.getPathKeys().isEmpty());
				assert(test.getPathObjects() != null);
				assert(!test.getPathObjects().isEmpty());
				
				browser = new Browser(acct_msg.getOptions().get("browser").toString());
				traverse_path(browser, test.getPathKeys(), test.getPathObjects(), acct_msg);
			  	browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){

				try{
					browser = new Browser(acct_msg.getOptions().get("browser").toString());
				}
				catch(NullPointerException e){
					log.error(e.getMessage(), "Failed to open connection to browser");
					return;
				}
				
				try{
					generate_landing_page_test(browser, acct_msg);
				}catch(Exception e){
					log.error(e.getMessage(), "Error occurred while generating landing page test");
				}
				browser.close();
		   }
			//log.warn("Total Test execution time (browser open, crawl, build test, save data) : " + browserActorRunTime);

		}else unhandled(message);
	}
	
	/**
	 * Generates {@link Test Tests} for path
	 * @param path
	 * @param result_page
	 * @throws JsonProcessingException 
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) throws JsonProcessingException {
		Test test = new Test(path_keys, path_objects, result_page, null);							
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);

		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);

		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
		
		//tell memory worker of test
		/*final ActorRef memory_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
				  .props("memoryRegistryActor"), "memory_registration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf());
		*/
		test_service.save(test, acct_msg.getOptions().get("host").toString());
		final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
				  .props("pathExpansionActor"), "path_expansion"+UUID.randomUUID());
		path_expansion_actor.tell(test_msg, getSelf());
		
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
					test_repo.save(test);
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
	public void generate_landing_page_test(Browser browser, Message<?> msg) throws MalformedURLException, IOException, NullPointerException{
		assert browser != null;
		assert msg != null;
		
	  	PageState page_obj = browser_service.buildPage(browser);
	  	page_obj.setLandable(true);

	  	PageState page_record = page_state_repo.findByKey(page_obj.getKey());
	  	if(page_record == null){
	  		page_obj = page_state_repo.save(page_obj);
	  		MessageBroadcaster.broadcastPathObject(page_obj, msg.getOptions().get("host").toString());
	  	}
	  	else{
	  		page_obj = page_record;
	  	}
	  	
	  	List<String> path_keys = new ArrayList<String>();
	  	path_keys.add(page_obj.getKey());
	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
		path_objects.add(page_obj);
		
		DiscoveryRecord discovery_record = discovery_repo.findByKey(msg.getOptions().get("discovery_key").toString());
		discovery_record.setLastPathRanAt(new Date());
		discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
		discovery_repo.save(discovery_record);
		
		Domain domain = domain_repo.findByHost((new URL(page_obj.getUrl())).getHost());
		//domain.addPageState(page_obj);
		//domain_repo.save(domain);
		
		Test test = createTest(path_keys, path_objects, page_obj, 1L, domain, msg, discovery_record);

		Test new_test = Test.clone(test);
		Message<Test> test_msg = new Message<Test>(msg.getAccountKey(), new_test, msg.getOptions());

		final ActorRef path_expansion_actor = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
				  .props("pathExpansionActor"), "path_expansion");
		path_expansion_actor.tell(test_msg, getSelf() );

		//domain_repo.save(domain);
				
		discovery_record = discovery_repo.findByKey(msg.getOptions().get("discovery_key").toString());
		discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
		discovery_repo.save(discovery_record);
		MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);

	}
	
	/**
	 * 
	 * 
	 * @param browser
	 * @param path
	 * @param acct_msg
	 * @throws NoSuchElementException
	 * @throws IOException
	 */
	public void traverse_path(Browser browser, List<String> path_keys, List<PathObject> path_objects, Message<?> acct_msg) throws NoSuchElementException, IOException{

		PageState result_page = null;
		long crawl_time_in_ms = -1L;
		final long pathCrawlStartTime = System.currentTimeMillis();
		int tries = 0;
		do{
			result_page = crawler.crawlPath(path_keys, path_objects, browser, acct_msg.getOptions().get("host").toString());
			tries++;
			result_page.setLandable(browser_service.checkIfLandable(acct_msg.getOptions().get("browser").toString(), result_page));
			
			PageState page_state_record = page_state_repo.findByKey(result_page.getKey());
		  	if(page_state_record != null){
		  		result_page= page_state_record;
		  	}
		  	else{
		  		page_state_repo.save(result_page);
		  		MessageBroadcaster.broadcastPathObject(result_page, acct_msg.getOptions().get("host").toString());
		  	}
		  	
		}while(result_page == null && tries < 5);
		final long pathCrawlEndTime = System.currentTimeMillis();
		
		crawl_time_in_ms = pathCrawlEndTime - pathCrawlStartTime;
				
		int last_idx = path_keys.size()-1;
		if(last_idx < 0){
			last_idx = 0;
		}

		if(!ExploratoryPath.hasCycle(path_objects, result_page)){
			/*path_keys.setIsUseful(false);
	  	}
	  	else{*/			
			Domain domain = domain_repo.findByHost((new URL(browser_service.buildPage(browser).getUrl())).getHost());
			//domain.addPageState(result_page);
			//domain_repo.save(domain);
			
			DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());
			discovery_record.setTestCount(discovery_record.getTestCount()+1);
			discovery_record.setLastPathRanAt(new Date());
			discovery_repo.save(discovery_record);

			createTest(path_keys, path_objects, result_page, crawl_time_in_ms, domain, acct_msg, discovery_record);
	  	}
	}
}