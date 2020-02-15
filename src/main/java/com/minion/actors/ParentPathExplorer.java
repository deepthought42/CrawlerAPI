package com.minion.actors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.minion.browsing.Crawler;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.Group;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.enums.DiscoveryAction;
import com.qanairy.models.message.DiscoveryActionRequest;
import com.qanairy.models.message.TestCandidateMessage;
import com.qanairy.models.message.TestMessage;
import com.qanairy.services.BrowserService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.TestCreatorService;
import com.qanairy.services.TestService;
import com.qanairy.utils.PathUtils;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

@Component
@Scope("prototype")
public class ParentPathExplorer extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(ParentPathExplorer.class.getName());
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private TestCreatorService test_creator_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private TestService test_service;

	@Autowired
	private ElementStateService element_state_service;
	
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
			  		List<String> path_keys = new ArrayList<>(message.getKeys());
					List<PathObject> path_objects = PathUtils.orderPathObjects(path_keys, message.getPathObjects());

					//get index of last page element in path
			  		int last_elem_idx = PathUtils.getIndexOfLastElementState(path_keys);

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
			  		Browser browser = null;
			  		
			  		//check if url changes between last page state and result
			  		boolean is_page_change = !message.getResultPage().getUrl().equals(last_page.getUrl());
			  		
					//do while result matches expected result
					do{
						/*
						Timeout timeout = Timeout.create(Duration.ofSeconds(120));
						Future<Object> future = Patterns.ask(message.getDomainActor(), new DiscoveryActionRequest(message.getDomain(), message.getAccountId()), timeout);
						DiscoveryAction discovery_action = (DiscoveryAction) Await.result(future, timeout.duration());

						if(discovery_action == DiscoveryAction.STOP) {
							log.warn("path message discovery actor returning");
							return;
						}
						*/
						try{
							error_occurred = false;
							browser = BrowserConnectionHelper.getConnection(message.getBrowser(), BrowserEnvironment.DISCOVERY);
							//crawl path using array of preceding elements\
							browser.navigateTo(first_page.getUrl());
							
							crawler.crawlPathWithoutBuildingResult(beginning_path_keys, beginning_path_objects, browser, host, message.getAccountId());
								
							log.warn("Parent path explorer is looking up parent element :: "+last_element.getXpath());
							ElementState parent_element = element_state_service.getParentElement(message.getAccountId(), message.getDomain(), last_page.getKey(), last_element.getKey());
							//if parent element does not have width then continue
							if(parent_element == null){
								log.warn("PARENT ELEMENT IS NULL!!! ABORTING PARENT PATH EXPANSION!!!!!!");
								break;
							}

							Dimension element_size = new Dimension(parent_element.getWidth(), parent_element.getHeight());
							Point location = new Point(parent_element.getXLocation(), parent_element.getYLocation());
							if(!BrowserService.hasWidthAndHeight(element_size) && BrowserService.doesElementHaveNegativePosition(location) && BrowserService.isElementLargerThanViewport(browser, element_size)){
								log.warn("parent element doesn't have width or height");
								break;
							}

							if((parent_element.getWidth() <= last_element.getWidth() || parent_element.getHeight() <= last_element.getHeight())
									&& (parent_element.getXLocation() >= last_element.getXLocation() || parent_element.getYLocation() >= last_element.getYLocation())){
								//parent as same location and size as child, stop exploring parents
								log.warn("Parent element isn't larger than child element?!?!  WTF??");
								break;
							}

							List<String> parent_end_path_keys = new ArrayList<>();
							parent_end_path_keys.add(parent_element.getKey());
							parent_end_path_keys.addAll(end_path_keys);

							List<PathObject> parent_end_path_objects = new ArrayList<>();
							parent_end_path_objects.add(parent_element);
							parent_end_path_objects.addAll(end_path_objects);

							log.warn("finishing path crawl in parent path explorer.....");
							//finish crawling using array of elements following last page element
							crawler.crawlParentPathWithoutBuildingResult(parent_end_path_keys, parent_end_path_objects, browser, host, last_element, message.getAccountId());
							
							PageState result = null;
							if(is_page_change) {
								if(browser.getDriver().getCurrentUrl().equals(message.getResultPage().getUrl())){
									result = message.getResultPage();
								}
							}
							else{
								result = browser_service.buildPageState(message.getAccountId(), message.getDomain(), browser);
							}


							//if result matches expected page then build new path using parent element state and break from loop
							if(result != null && result.equals(message.getResultPage())){
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
							last_element = parent_element;
						}
						catch(NullPointerException e){
							log.warn("NullPointerException occurred in ParentPathExplorer :: "+e.getMessage());
							error_occurred = true;
						}
						catch(WebDriverException e){
							log.warn("Exception occurred in ParentPathExplorer :: "+e.getMessage());
							error_occurred = true;
						}
						catch(GridException e){
							log.warn("Exception occurred in ParentPathExplorer :: "+e.getMessage());
							error_occurred = true;
						}
						finally{
							if(browser != null){
								browser.close();
							}
						}
					}while((results_match || error_occurred) && !last_element.getName().equals("body"));
					long end = System.currentTimeMillis();
			  		log.warn("time(ms) spent generating ALL parent xpaths :: " + (end-start));

					boolean is_result_matches_other_page_in_path = test_service.checkIfEndOfPathAlreadyExistsInPath(message.getResultPage(), path_keys);
					if(is_result_matches_other_page_in_path) {
						return;
					}

					Domain domain = message.getDomain();
					log.warn("domain url :: "+domain.getUrl());
				  	URL domain_url = new URL(domain.getProtocol()+"://"+domain.getUrl());

			  		Test test = test_creator_service.createTest( final_path_keys, final_path_objects, message.getResultPage(), (end-start), message.getBrowser().toString(), domain_url.getHost(), message.getAccountId());
					TestMessage test_message = new TestMessage(test, message.getDiscoveryActor(), message.getBrowser(), message.getDomainActor(), domain, message.getAccountId());

		  			message.getDiscoveryActor().tell(test_message, getSelf());
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
					break;
				}
			}
		}
	}
}
