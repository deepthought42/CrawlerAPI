package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.minion.actors.MemoryRegistryActor;
import com.minion.api.MessageBroadcaster;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DiscoveryRecordRepository;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.OrientConnectionFactory;
import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

/**
 * Manages a browser instance and sets a crawler upon the instance using a given path to traverse 
 *
 */
public class UrlBrowserActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(UrlBrowserActor.class.getName());

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
			if(acct_msg.getData() instanceof URL){

				try{
					browser = new Browser(acct_msg.getOptions().get("browser").toString());
				}
				catch(NullPointerException e){
					log.error(e.getMessage());
					System.err.println("Failed to open connection to browser");
					return;
				}
				
				try{
					generate_landing_page_test(browser, acct_msg);
				}catch(Exception e){
					log.error(e.getMessage());
					e.printStackTrace();
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
	private void createTest(Path path, Page result_page, long crawl_time, Domain domain, Message<?> acct_msg, DiscoveryRecord discovery ) {
		path.setIsUseful(true);
		Test test = new Test(path, result_page, domain, "Test #"+domain.getTestCount());							
		TestRepository test_repo = new TestRepository();
		test.setKey(test_repo.generateKey(test));
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);

		Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test, acct_msg.getOptions());
		
		//tell memory worker of test
		final ActorRef memory_actor = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "MemoryRegistration"+UUID.randomUUID());
		memory_actor.tell(test_msg, getSelf());
	}

	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 * 
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPath().getPath()){
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
	public void generate_landing_page_test(Browser browser, Message<?> msg) throws MalformedURLException, IOException, NullPointerException{
		assert browser != null;
		assert msg != null;
		
		browser.getDriver().get(((URL)msg.getData()).toString());

	  	Path path = new Path();
	  	Page page_obj = browser.buildPage();
	  	page_obj.setLandable(true);
	  	path.getPath().add(page_obj);
		PathRepository path_repo = new PathRepository();
		path.setKey(path_repo.generateKey(path));
		OrientConnectionFactory conn = new OrientConnectionFactory();

		DiscoveryRecordRepository discovery_repo = new DiscoveryRecordRepository();
		DiscoveryRecord discovery_record = discovery_repo.find(conn, msg.getOptions().get("discovery_key").toString());
		discovery_record.setLastPathRanAt(new Date());
		discovery_record.setExaminedPathCount(discovery_record.getExaminedPathCount()+1);
		discovery_record.setTestCount(discovery_record.getTestCount()+1);
		discovery_repo.save(conn, discovery_record);

		DomainRepository domain_repo = new DomainRepository();
		Domain domain = domain_repo.find(conn, page_obj.getUrl().getHost());
		domain.setTestCount(domain.getTestCount()+1);
		domain_repo.save(conn, domain);
		
		createTest(path, page_obj, 1L, domain, msg, discovery_record);
		MessageBroadcaster.broadcastDiscoveryStatus(domain.getUrl(), discovery_record);

		Path new_path = Path.clone(path);
		Message<Path> path_msg = new Message<Path>(msg.getAccountKey(), new_path, msg.getOptions());

		final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
		path_expansion_actor.tell(path_msg, getSelf() );

		final ActorRef form_test_discoverer = this.getContext().actorOf(Props.create(FormTestDiscoveryActor.class), "FormTestDiscoveryActor"+UUID.randomUUID());
		form_test_discoverer.tell(path_msg, getSelf() );
	}
}