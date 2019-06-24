package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.minion.structs.Message;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ElementState;
import com.qanairy.models.Group;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.message.TestCandidateMessage;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DiscoveryRecordService;
import com.qanairy.services.TestService;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class ParentPathExplorer extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(ParentPathExplorer.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;

	@Autowired
	private DiscoveryRecordService discovery_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private TestService test_service;

	@Autowired
	private Crawler crawler;

	private ActorRef path_expansion;

	@Override
	public void preStart() {
		//subscribe to cluster changes
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
	      MemberEvent.class, UnreachableMember.class);

		path_expansion = actor_system.actorOf(SpringExtProvider.get(actor_system)
			  .props("pathExpansionActor"), "path_expansion_actor"+UUID.randomUUID());
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	/**
	 * {@inheritDoc}
	 *
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(TestCandidateMessage.class, message-> {
					//get index of last page element in path
			  		int last_elem_idx = getIndexOfLastElementState(message.getKeys());
			  		List<String> final_path_keys = message.getKeys();
			  		List<PathObject> final_path_objects = message.getPathObjects();
			  		List<String> path_keys = message.getKeys();//.subList(0, last_elem_idx+1);
					List<PathObject> path_objects = message.getPathObjects();//.subList(0, last_elem_idx+1);
					Browser browser = null;

					log.warn("generating parent xpaths");
			  		long start = System.currentTimeMillis();
			  		//get parent web element
					log.warn("keys vs objects sizes     :::::::::::::    "  + path_keys.size() + "   :   "+path_objects.size());
					List<PathObject> ordered_path_objects = new ArrayList<PathObject>();
					//Ensure Order path objects
					for(String path_obj_key : path_keys){
						for(PathObject obj : path_objects){
							if(obj.getKey().equals(path_obj_key)){
								ordered_path_objects.add(obj);
							}
						}
					}

					PathObject last_path_obj = null;
					List<PathObject> reduced_path_obj = new ArrayList<PathObject>();
					//scrub path objects for duplicates
					for(PathObject obj : ordered_path_objects){
						if(last_path_obj == null || !obj.getKey().equals(last_path_obj.getKey())){
							last_path_obj = obj;
							reduced_path_obj.add(obj);
						}
					}
					ordered_path_objects = reduced_path_obj;

					path_objects = ordered_path_objects;

					//get array of all elements preceding last page element
					List<String> beginning_path_keys = path_keys.subList(0, last_elem_idx);
					List<PathObject> beginning_path_objects = path_objects.subList(0, last_elem_idx);

					//get array of all elements after last page element
					List<String> end_path_keys = new ArrayList<String>();
					List<PathObject> end_path_objects = new ArrayList<>();

					if((last_elem_idx+1) < path_keys.size()){
						end_path_keys = path_keys.subList(last_elem_idx+1, path_keys.size());
						end_path_objects = path_objects.subList(last_elem_idx+1, path_keys.size());
					}

					//get last page element
					ElementState last_element = (ElementState)path_objects.get(last_elem_idx);

					boolean results_match = false;
					boolean error_occurred = false;
					//do while result matches expected result
					do{
						try{
							error_occurred = false;

							browser = BrowserConnectionFactory.getConnection(message.getDiscovery().getBrowserName(), BrowserEnvironment.DISCOVERY);
							//crawl path using array of preceding elements
							log.warn("crawling beginning of path :: "+beginning_path_keys);
							crawler.crawlPathWithoutBuildingResult(beginning_path_keys, beginning_path_objects, browser, message.getDiscovery().getDomainUrl());

							//extract parent element
							String element_xpath = last_element.getXpath();
							WebElement current_element = browser.getDriver().findElement(By.xpath(element_xpath));
							WebElement parent_web_element = browser_service.getParentElement(current_element);

							log.warn("Builing element state");
							ElementState parent_element = null;
							try{
								parent_element = browser_service.buildElementState(browser, parent_web_element, ImageIO.read(new URL(message.getResultPage().getScreenshotUrl())));
								if(parent_element == null){
									break;
								}
							}
							catch(RasterFormatException e){
								break;
							}

							log.warn("crawling partial path");
							//finish crawling using array of elements following last page element
							crawler.crawlPartialPath(end_path_keys, end_path_objects, browser, message.getDiscovery().getDomainUrl(), parent_element);

							log.warn("building parent result page state");
							//build result page
							PageState parent_result = browser_service.buildPage(browser);

							log.warn("checking if parent result matches expected result");
							//if result matches expected page then build new path using parent element state and break from loop
							if(parent_result.equals(message.getResultPage())){
								log.warn("parent result matches expected result page");
								final_path_keys = new ArrayList<>(beginning_path_keys);
								final_path_keys.add(parent_element.getKey());
								final_path_keys.addAll(end_path_keys);

								final_path_objects = new ArrayList<>(beginning_path_objects);
								final_path_objects.add(parent_element);
								final_path_objects.addAll(end_path_objects);
								results_match = true;
							}
							else{
								results_match = false;
								break;
							}
							log.warn("Setting last element to parent element");
							last_element = parent_element;
						}catch(Exception e){
							error_occurred = true;
							e.printStackTrace();
						}
						finally{
							if(browser != null){
								browser.close();
							}
						}
					}while((results_match || error_occurred) && !last_element.getName().equals("body"));

					log.warn("final path objects ::    " + final_path_objects);
			  		for(PathObject obj : final_path_objects){
						log.warn("PATH OBJECT AFTER PARENT ::  "+obj.getType());
			  		}

			  		long end = System.currentTimeMillis();
			  		log.warn("time(ms) spent generating ALL parent xpaths :: " + (end-start));

			  		Test test = createTest(final_path_keys, final_path_objects, message.getResultPage(), (end-start), message.getDiscovery().getDomainUrl(), message.getDiscovery().getBrowserName());

			  		Message<Test> test_msg = new Message<Test>(message.getAccountKey(), test, message.getOptions());

			  		if(!test.getSpansMultipleDomains()){
			  			path_expansion.tell(test_msg, getSelf());
			  		}

			  		DiscoveryRecord discovery_record = discovery_service.incrementTestCount(message.getDiscovery().getKey());

					//DiscoveryRecord discovery_record = discovery_service.increaseExaminedPathCount(message.getDiscovery().getKey(), 1);

					try{
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
				  	}catch(Exception e){
				  		log.error("Error sending discovery status from Exploratory Actor :: "+e.getMessage());
					}
					
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.info("received unknown message");
				})
				.build();
	}

	/**
	 * Generates {@link Test Tests} for test
	 * @param test
	 * @param result_page
	 * @throws JsonProcessingException
	 * @throws MalformedURLException
	 */
	private Test createTest(List<String> path_keys, List<PathObject> path_objects, PageState result_page, long crawl_time, String host, String browser_name) throws JsonProcessingException, MalformedURLException {
		log.warn("Creating test........");
		Test test = new Test(path_keys, path_objects, result_page, null);

		Test test_db = test_service.findByKey(test.getKey());
		if(test_db != null){
			test = test_db;
		}

		test.setRunTime(crawl_time);
		test.setLastRunTimestamp(new Date());
		addFormGroupsToPath(test);

		TestRecord test_record = new TestRecord(test.getLastRunTimestamp(), TestStatus.UNVERIFIED, browser_name, result_page, crawl_time);
		test.addRecord(test_record);

		boolean leaves_domain = !test.firstPage().getUrl().contains(new URL(test.getResult().getUrl()).getHost());
		test.setSpansMultipleDomains(leaves_domain);
		return test_service.save(test, host);
	}


	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 *
	 * @param test {@linkplain Test} that you want to label
	 */
	private void addFormGroupsToPath(Test test) {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(ElementState.class)){
				ElementState elem = (ElementState)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_service.save(test, test.firstPage().getUrl());
					break;
				}
			}
		}
	}

	private int getIndexOfLastElementState(List<String> path_keys){
		for(int element_idx=path_keys.size()-1; element_idx > 0; element_idx--){
			if(path_keys.get(element_idx).contains("elementstate")){
				return element_idx;
			}
		}

		return -1;
	}
}
