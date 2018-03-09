package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.minion.actors.MemoryRegistryActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Group;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;

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
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Path and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			log.info("Browser actor received message");
			Message<?> acct_msg = (Message<?>)message;

			Browser browser = null;
			if (acct_msg.getData() instanceof ExploratoryPath){
				/*
				 * tell discovery registry that we are running an exploratory path for discovery
				 */
				
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();
				browser = new Browser(((Page)exploratory_path.getPath().get(0)).getUrl().toString(), (String)acct_msg.getOptions().get("browser"));
				Page last_page = exploratory_path.findLastPage();
				//log.info("Checking if page is landable");
				//boolean landable_status = last_page.checkIfLandable(acct_msg.getOptions().get("browser").toString());
				//log.info("landable status: " +landable_status);
				//last_page.setLandable(landable_status);
							
				//if(landable_status){
					//clone path starting at last page in path
					//Path shortened_path = path.clone());
				//}

				if(exploratory_path.getPath() != null){
					Page result_page = null;

					// increment total paths being explored for domain
					String domain_url = last_page.getUrl().getHost();
					DomainRepository domain_repo = new DomainRepository();
					IDomain idomain = domain_repo.find(domain_url);
					idomain.setLastDiscoveryPathRanAt(new Date());
					
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					for(Action action : exploratory_path.getPossibleActions()){
						Path path = Path.clone(exploratory_path);
						path.add(action);
						final long pathCrawlStartTime = System.currentTimeMillis();
						result_page = Crawler.crawlPath(path, browser);
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
						
						int last_idx = exploratory_path.getPath().size()-1;
						if(last_idx < 0){
							last_idx = 0;
						}
						last_page.setLandable(last_page.checkIfLandable(acct_msg.getOptions().get("browser").toString()));

						if(last_page.equals(result_page)){//Path.hasCycle(path,result_page)){
							//check if test has 3 or more consecutive click events since last page
					  		path.setIsUseful(false);
					  		System.err.println("EXPLORATORY PATH HAS CYCLE...trying to next action");
					  		continue;
					  	}
					  	else{
					  		OrientConnectionFactory conn = new OrientConnectionFactory();
							Domain domain = domain_repo.find(conn, browser.getPage().getUrl().getHost());
							domain.setLastDiscoveryPathRanAt(new Date());
							int cnt = domain.getDiscoveredTestCount()+1;
							System.out.println("landing page test Count :: "+cnt);
							domain.setDiscoveredTestCount(cnt);
					  		System.out.println("Count :: "+cnt);
					  		
					  		createTest(path, result_page, pathCrawlRunTime, domain, acct_msg);
							System.err.println("TEST CREATED, path is being expanded");

					  		Path new_path = Path.clone(path);
							new_path.add(result_page);
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_path, acct_msg.getOptions());

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf() );
					  		break;
					  	}
					}
				}

			  	browser.close();
				
				/*
				 * tell discovery registry that we are FINISHED running an exploratory path for discovery
				 */
			  	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if (acct_msg.getData() instanceof Path){
				log.info("Path started");

				Path path = (Path)acct_msg.getData();
				assert(path.getPath() != null);
				if(acct_msg.getOptions().isEmpty()){
				}
				
				browser = new Browser(((Page)path.getPath().get(0)).getUrl().toString(), acct_msg.getOptions().get("browser").toString());
				traverse_path_and_create_test(browser, path, acct_msg);
			  	browser.close();
	
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
			}
			else if(acct_msg.getData() instanceof URL){
				log.info("Url provided");

				try{
					browser = new Browser(((URL)acct_msg.getData()).toString(), acct_msg.getOptions().get("browser").toString());
				}
				catch(NullPointerException e){
					log.error("Failed to open connection to browser");
					return;
				}
				log.info("preparting to generate landing page test");
				
				try{
					generate_landing_page_test(browser, acct_msg);
				}catch(Exception e){
					log.info(e.getMessage(), "Error occurred while generating landing page test");
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
	private void createTest(Path path, Page result_page, long crawl_time, Domain domain, Message<?> acct_msg ) {
		path.setIsUseful(true);
		Test test = new Test(path, result_page, domain, "Test #"+domain.getDiscoveredTestCount());							
		TestRepository test_repo = new TestRepository();
		test.setKey(test_repo.generateKey(test));
		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);
		
		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), null, acct_msg.getOptions().get("browser").toString(), test.getResult(), crawl_time);
		test.addRecord(test_record);
		log.info("sending test message out");
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
	 * counts how many clicks have happened in a sequence since last page change
	 * 
	 * @param last_idx
	 * @param path
	 * @param last_page
	 * @return
	 */
	private int getLastClicksSequenceCount(int last_idx, Path path, Page last_page) {
		int clicks = 0;
		
		while(last_idx>=0){
			if(path.getPath().get(last_idx).equals(last_page)){
				break;
			}
			PathObject obj = path.getPath().get(last_idx);
			if(obj.getType().equals("Action")){
				log.info("checking action in exploratory path");
				Action path_action = (Action)obj;
				if(path_action.getName().equals("click") || path_action.getName().equals("doubleclick")){
					log.info("incrementing click count");
					clicks++;
				}
			}
			last_idx--;
		};
		
		return clicks;
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
		
	  	Path path = new Path();
	  	System.out.println("Getting browser page...");
	  	Page page_obj = browser.getPage();
		page_obj.setLandable(true);
	  	path.add(page_obj);
		PathRepository path_repo = new PathRepository();
		path.setKey(path_repo.generateKey(path));
		
		DomainRepository domain_repo = new DomainRepository();
		OrientConnectionFactory conn = new OrientConnectionFactory();
		Domain domain = domain_repo.find(conn, page_obj.getUrl().getHost());
		domain.setLastDiscoveryPathRanAt(new Date());
		int cnt = domain.getDiscoveredTestCount()+1;
		domain.setDiscoveredTestCount(cnt);
		domain_repo.update(conn, domain);
		
		createTest(path, page_obj, 1L, domain, msg);
		
		Message<Path> path_msg = new Message<Path>(msg.getAccountKey(), path, msg.getOptions());

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
	public void traverse_path_and_create_test(Browser browser, Path path, Message<?> acct_msg) throws NoSuchElementException, IOException{

		Page result_page = null;
		long crawl_time_in_ms = -1L;
		final long pathCrawlStartTime = System.currentTimeMillis();
		result_page = Crawler.crawlPath(path, browser);	
		final long pathCrawlEndTime = System.currentTimeMillis();
		
		crawl_time_in_ms = pathCrawlEndTime - pathCrawlStartTime;
		
		Page last_page = path.findLastPage();

		last_page.setLandable(last_page.checkIfLandable(acct_msg.getOptions().get("browser").toString()));
		if(last_page.isLandable()){
			//clone path starting at last page in path
			//Path shortened_path = path.clone());
		}
		
		PathRepository path_repo = new PathRepository();
		path.setKey(path_repo.generateKey(path));
		int last_idx = path.getPath().size()-1;
		if(last_idx < 0){
			last_idx = 0;
		}
		int clicks = getLastClicksSequenceCount(last_idx, path, last_page);
		if(clicks >= 3 && last_page.equals(result_page) && path.getPath().size() > 1){
	  		path.setIsUseful(false);
	  	}
	  	else{				
	  		DomainRepository domain_repo = new DomainRepository();
	  		OrientConnectionFactory conn = new OrientConnectionFactory();
			Domain domain = domain_repo.find(conn, browser.getPage().getUrl().getHost());
			domain.setLastDiscoveryPathRanAt(new Date());
			int cnt = domain.getDiscoveredTestCount()+1;
			System.out.println("landing page test Count :: "+cnt);
			domain.setDiscoveredTestCount(cnt);
			domain_repo.update(conn, domain);
			
	  		createTest(path, result_page, crawl_time_in_ms, domain, acct_msg);
	  	}
	}
}