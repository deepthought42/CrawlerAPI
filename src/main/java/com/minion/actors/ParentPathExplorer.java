package com.minion.actors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.minion.browsing.Crawler;
import com.qanairy.models.ElementState;
import com.qanairy.models.Group;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.message.TestCandidateMessage;
import com.qanairy.services.BrowserService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.TestService;
import com.qanairy.utils.BrowserUtils;
import com.qanairy.utils.PathUtils;

import akka.actor.AbstractActor;
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
	private PageStateService page_state_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private TestService test_service;

	@Autowired
	private Crawler crawler;

	@Override
	public void preStart() {
		//subscribe to cluster changes
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
	      MemberEvent.class, UnreachableMember.class);
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
			  		long start = System.currentTimeMillis();

			  		List<String> final_path_keys = new ArrayList<String>(message.getKeys());
			  		List<PathObject> final_path_objects = new ArrayList<PathObject>(message.getPathObjects());
			  		Browser browser = null;

			  		List<String> path_keys = new ArrayList<>(message.getKeys());
					List<PathObject> path_objects = PathUtils.orderPathObjects(path_keys, message.getPathObjects());
					//get index of last page element in path
			  		int last_elem_idx = PathUtils.getIndexOfLastElementState(path_keys);
			  		
					log.warn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
					log.warn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			  		log.warn("path keys :: " + path_keys.size());
			  		for(String key : path_keys){
			  			log.warn("key :: " + key);
			  		}
			  		log.warn("path objects  :: " + path_objects.size());
			  		for(PathObject obj : message.getPathObjects()){
			  			log.warn(obj.getType() + "  :   "+obj);
			  		}
			  		log.warn("last element index  :: " + last_elem_idx);
			  		log.warn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
					log.warn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

					
					//get array of all elements preceding last page element
					List<String> beginning_path_keys = path_keys.subList(0, last_elem_idx);
					List<PathObject> beginning_path_objects = path_objects.subList(0, last_elem_idx);

					//get array of all elements after last page element
					List<String> end_path_keys = new ArrayList<String>();
					List<PathObject> end_path_objects = new ArrayList<>();

					if((last_elem_idx+1) < path_keys.size()){
						end_path_keys = path_keys.subList(last_elem_idx+1, path_keys.size());
						end_path_objects.addAll(path_objects.subList(last_elem_idx+1, path_objects.size()));
					}

					//get last page element
					ElementState last_element = (ElementState)path_objects.get(last_elem_idx);
			  		PageState last_page = PathUtils.getLastPageState(path_objects);
			  		PageState first_page = PathUtils.getFirstPage(path_objects);
			  		String host = new URL(last_page.getUrl()).getHost();
			  		
					boolean results_match = false;
					boolean error_occurred = false;
					//do while result matches expected result
					do{
						try{
							error_occurred = false;

							browser = BrowserConnectionFactory.getConnection(message.getBrowser(), BrowserEnvironment.DISCOVERY);
							//crawl path using array of preceding elements\
							log.warn("Crawling path :: " +path_objects);
							log.warn("crawling beginning of path :: "+beginning_path_keys);
							log.warn("navigating to url :: " +first_page.getUrl());
							browser.navigateTo(first_page.getUrl());
							crawler.crawlPathWithoutBuildingResult(beginning_path_keys, beginning_path_objects, browser, host);

							//extract parent element
							String element_xpath = last_element.getXpath();
							WebElement current_element = browser.getDriver().findElement(By.xpath(element_xpath));
							WebElement parent_web_element = browser_service.getParentElement(current_element);

							//if parent element does not have width then continue
							Dimension element_size = parent_web_element.getSize();
							if(!BrowserService.hasWidthAndHeight(element_size)
										|| !BrowserService.isElementVisibleInPane(browser, parent_web_element.getLocation(), element_size)){
								break;
							}
							//if parent element is not visible in pane then break
							log.warn("Builing element state");
							ElementState parent_element = null;
							parent_element = browser_service.buildElementState(browser, parent_web_element, ImageIO.read(new URL(last_page.getScreenshotUrl())));
							if(parent_element == null){
								break;
							}

							List<String> parent_end_path_keys = new ArrayList<>();
							parent_end_path_keys.add(parent_element.getKey());
							parent_end_path_keys.addAll(end_path_keys);
							
							List<PathObject> parent_end_path_objects = new ArrayList<>();
							parent_end_path_objects.add(parent_element);
							parent_end_path_objects.addAll(end_path_objects);
							//finish crawling using array of elements following last page element
							crawler.crawlPathWithoutBuildingResult(parent_end_path_keys, parent_end_path_objects, browser, host);

							PageLoadAnimation loading_animation = BrowserUtils.getLoadingAnimation(browser, host);
							if(loading_animation != null){
								parent_end_path_keys.add(loading_animation.getKey());
								parent_end_path_objects.add(loading_animation);
							}
							
							log.warn("building parent result page state");
							String screenshot_checksum = PageState.getFileChecksum(browser.getViewportScreenshot());
							
							PageState result = page_state_service.findByScreenshotChecksum(screenshot_checksum);
							if(result == null){
								result = page_state_service.findByAnimationImageChecksum(screenshot_checksum);
							}

							log.warn("checking if parent result matches expected result");
							//if result matches expected page then build new path using parent element state and break from loop
							if(result != null && result.equals(message.getResultPage())){
								log.warn("parent result matches expected result page");
								final_path_keys = new ArrayList<>(beginning_path_keys);
								final_path_keys.addAll(parent_end_path_keys);

								final_path_objects = new ArrayList<>(beginning_path_objects);
								final_path_objects.addAll(parent_end_path_objects);
								results_match = true;
							}
							else{
								results_match = false;
								break;
							}
							log.warn("Setting last element to parent element");
							last_element = parent_element;
						}catch(NullPointerException e){
							log.warn("NullPointerException occurred in ParentPathExplorer :: "+e.getMessage());
							error_occurred = true;
							e.printStackTrace();
						}catch(Exception e){
							log.warn("Exception occurred in ParentPathExplorer :: "+e.getMessage());
							error_occurred = true;
						}
						finally{
							if(browser != null){
								browser.close();
							}
						}
					}while((results_match || error_occurred) && !last_element.getName().equals("body"));

					log.warn("final path objects ::    " + final_path_objects);
			  		for(PathObject obj : final_path_objects){
						log.warn("PATH OBJECT AFTER PARENT ::  "+obj);
			  		}

			  		long end = System.currentTimeMillis();
			  		log.warn("time(ms) spent generating ALL parent xpaths :: " + (end-start));
			  		log.warn("test host :: " + host);
			  		Test test = createTest(final_path_keys, final_path_objects, message.getResultPage(), (end-start), host, message.getBrowser().toString());
		  			message.getDiscoveryActor().tell(test, getSelf());
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
		
		log.warn("creating test with host :: " + host);
		return test_service.save(test, host);
	}


	/**
	 * Adds Group labeled "form" to test if the test has any elements in it that have form in the xpath
	 *
	 * @param test {@linkplain Test} that you want to label
	 * @throws MalformedURLException 
	 */
	private void addFormGroupsToPath(Test test) throws MalformedURLException {
		//check if test has any form elements
		for(PathObject path_obj: test.getPathObjects()){
			if(path_obj.getClass().equals(ElementState.class)){
				ElementState elem = (ElementState)path_obj;
				if(elem.getXpath().contains("form")){
					test.addGroup(new Group("form"));
					test_service.save(test, new URL(test.firstPage().getUrl()).getHost());
					break;
				}
			}
		}
	}
}
