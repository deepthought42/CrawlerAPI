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

import com.minion.actors.MemoryRegistryActor;
import com.minion.api.MessageBroadcaster;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.Domain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.Test;
import com.qanairy.persistence.TestRecord;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.GroupPOJO;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.TestRecordPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.models.dao.impl.PageStateDaoImpl;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class BrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(BrowserActor.class.getName());

	private static Random rand = new Random();
	private UUID uuid = null;

		
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
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		Test test = new TestPOJO(path_keys, path_objects, result_page, "Test #"+domain.getTestCount());							
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);

		TestRecord test_record = new TestRecordPOJO(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
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
					test.addGroup(new GroupPOJO("form"));
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
		
	  	PageState page_obj = browser.buildPage();

	  	List<String> path_keys = new ArrayList<String>();
	  	path_keys.add(page_obj.getKey());
	  	
	  	List<PathObject> path_objects = new ArrayList<PathObject>();
		path_objects.add(page_obj);
	  		  	
	  	page_obj.setLandable(true);
		OrientConnectionFactory conn = new OrientConnectionFactory();

		DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
		DiscoveryRecord discovery_record = discovery_repo.find(msg.getOptions().get("discovery_key").toString());
		discovery_record.setLastPathRanAt(new Date());
		discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
		discovery_repo.save(discovery_record);
		
		PageStateDao page_state_dao = new PageStateDaoImpl();

		DomainDao domain_repo = new DomainDaoImpl();
		Domain domain = domain_repo.find(page_obj.getUrl().getHost());
		domain.setTestCount(domain.getTestCount()+1);
		domain.addPageState(page_state_dao.save(page_obj));
		domain_repo.save(domain);
		
		MessageBroadcaster.broadcastPageState(page_obj, domain.getUrl());
		
		Test test = createTest(path_keys, path_objects, page_obj, 1L, domain, msg, discovery_record);
		MessageBroadcaster.broadcastDiscoveryStatus(domain.getUrl(), discovery_record);

		discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
		discovery_repo.save(discovery_record);
		
		Test new_test = TestPOJO.clone(test);
		Message<Test> path_msg = new Message<Test>(msg.getAccountKey(), new_test, msg.getOptions());
		
		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
		path_expansion_actor.tell(path_msg, getSelf() );
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
			result_page = Crawler.crawlPath(path_keys, path_objects, browser);
			tries++;
			result_page.setLandable(Browser.checkIfLandable(acct_msg.getOptions().get("browser").toString(), result_page));
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
			PageStateDao page_state_dao = new PageStateDaoImpl();

	  		DomainDao domain_repo = new DomainDaoImpl();
			Domain domain = domain_repo.find(browser.buildPage().getUrl().getHost());
			domain.setTestCount(domain.getTestCount()+1);
			domain.addPageState(page_state_dao.save(result_page));
			domain_repo.save(domain);
			
			MessageBroadcaster.broadcastPageState(result_page, domain.getUrl());

			DiscoveryRecordDao discovery_repo = new DiscoveryRecordDaoImpl();
			DiscoveryRecord discovery_record = discovery_repo.find(acct_msg.getOptions().get("discovery_key").toString());
			discovery_record.setTestCount(discovery_record.getTestCount()+1);
			discovery_record.setLastPathRanAt(new Date());
			discovery_repo.save(discovery_record);

			createTest(path_keys, path_objects, result_page, crawl_time_in_ms, domain, acct_msg, discovery_record);
	  	}
	}
}