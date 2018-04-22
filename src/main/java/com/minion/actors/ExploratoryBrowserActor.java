package com.minion.actors;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.models.dto.TestRepository;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class ExploratoryBrowserActor extends UntypedActor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ExploratoryBrowserActor.class.getName());

	/**
	 * {@inheritDoc}
	 * 
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Path and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException 
	 * @throws IOException 
	 * @throws NoSuchElementException 
	 */
	@Override
	public void onReceive(Object message) throws NullPointerException, NoSuchElementException, IOException {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;

			Browser browser = null;
			if (acct_msg.getData() instanceof ExploratoryPath){
				ExploratoryPath exploratory_path = (ExploratoryPath)acct_msg.getData();

				/*
				 * tell discovery registry that we are running an exploratory path for discovery
				 */
		  		OrientConnectionFactory conn = new OrientConnectionFactory();
				DomainRepository domain_repo = new DomainRepository();	
				Domain domain = domain_repo.find(conn, ((Page)exploratory_path.getPath().get(0)).getUrl().getHost());
				domain.setDiscoveryPathCount(domain.getDiscoveryPathCount()+1);
				domain_repo.save(conn, domain);

				browser = new Browser(((Page)exploratory_path.getPath().get(0)).getUrl().toString(), (String)acct_msg.getOptions().get("browser"));
				
				Page last_page = exploratory_path.findLastPage();
				/*
				last_page.setLandable(last_page.checkIfLandable(acct_msg.getOptions().get("browser").toString()));
				if(last_page.isLandable()){
					//clone path starting at last page in path
					System.err.println("Last page in traversal in landable");
					path = new Path();
					path.add(last_page);
				}
				*/
				
				if(exploratory_path.getPath() != null){
					Page result_page = null;

					// increment total paths being explored for domain
					String domain_url = last_page.getUrl().getHost();
					IDomain idomain = domain_repo.find(domain_url);
					idomain.setLastDiscoveryPathRanAt(new Date());
					//iterate over all possible actions and send them for expansion if crawler returns a page that differs from the last page
					//It is assumed that a change in state, regardless of how miniscule is of interest and therefore valuable. 
					
					for(Action action : exploratory_path.getPossibleActions()){
						Path path = Path.clone(exploratory_path);
						path.add(action);
						final long pathCrawlStartTime = System.currentTimeMillis();
						int tries = 0;
						do{
							result_page = Crawler.crawlPath(path, browser);
							result_page.setLandable(last_page.checkIfLandable(acct_msg.getOptions().get("browser").toString()));
							tries++;
							System.err.println("Attempting to get result_page :: "+tries+";   is NULL?   ::  "+(result_page==null));
						}while(result_page == null && tries < 5);
						
						final long pathCrawlEndTime = System.currentTimeMillis();

						long pathCrawlRunTime = pathCrawlEndTime - pathCrawlStartTime;
						
						int last_idx = path.getPath().size()-1;
						if(last_idx < 0){
							last_idx = 0;
						}
						
						if(ExploratoryPath.hasCycle(path, result_page)){
					  		path.setIsUseful(false);
					  		continue;
					  	}
					  	else{
					  		path.setIsUseful(true);
							
					  		domain = domain_repo.find(conn, domain_url);
					  		domain.setLastDiscoveryPathRanAt(new Date());
							domain.setDiscoveredTestCount(domain.getDiscoveredTestCount()+1);
							domain_repo.save(conn, domain);
							
					  		createTest(path, result_page, pathCrawlRunTime, domain, acct_msg);
							
					  		Path new_path = Path.clone(path);
							new_path.add(result_page);
							Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), new_path, acct_msg.getOptions());

							final ActorRef path_expansion_actor = this.getContext().actorOf(Props.create(PathExpansionActor.class), "PathExpansionActor"+UUID.randomUUID());
							path_expansion_actor.tell(path_msg, getSelf());
					  	}
						
						if(path.isUseful()){
							break;
						}
					}
					
					/*
					 * tell discovery registry that we are FINISHED running an exploratory pa	th for discovery
					 */
					domain = domain_repo.find(conn, domain_url);
					domain.setDiscoveryPathCount(domain.getDiscoveryPathCount()-1);
					domain_repo.save(conn, domain);
				  	browser.close();
					
				  	conn.close();
				}

				
				//PLACE CALL TO LEARNING SYSTEM HERE
				//Brain.learn(path, path.getIsUseful());
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
		Test test = new Test(path, result_page, domain, "Test #" + domain.getDiscoveredTestCount());							
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
}
