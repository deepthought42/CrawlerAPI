package com.minion.actors;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.services.BrowserService;
import com.qanairy.services.PageStateService;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Handles the collection and extraction of data from pages at a given url
 * 
 */
@Component
@Scope("prototype")
public class ElementDataExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ElementDataExtractor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private BrowserService browser_service;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
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
				.match(PageState.class, page_state-> {
					log.warn("Element data extractor received PageState message..."+page_state.getUrl());
					extractElementDataFromPageStateUsingBrowser(page_state);
					postStop();
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.debug("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.debug("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.debug("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}

	@Deprecated
	private void extractElementDataFromPageStateUsingBrowser(PageState page_state) {
		int error_cnt = 0;
		boolean page_state_build_success = false;
		Browser browser = null;
		Map<String, ElementState> reviewed_elements = new HashMap<>();
		error_cnt = 0;
		page_state_build_success = false;
		do {
			try {
				log.debug("Element Data Extractor retrieving browser connection ... "+page_state.getUrl());
				browser = BrowserConnectionHelper.getConnection(BrowserType.create("chrome"), BrowserEnvironment.DISCOVERY);
				browser.navigateTo(page_state.getUrl());
				List<ElementState> elements = browser_service.extractElementStates(page_state.getSrc(), browser, reviewed_elements);
			
				page_state.addElements(elements);
				page_state = page_state_service.save(page_state);
				//send page state message to auditor
				getSender().tell(page_state, getSelf());
				break;
			}catch(Exception e) {
				if(e instanceof GridException || e instanceof WebDriverException) {
					log.warn("Selenium Grid exception thrown during page data extractions");
					e.printStackTrace();
				}
				e.printStackTrace();
			}
			finally {
				if( browser != null ) {
					browser.close();
				}
			}
		}while(!page_state_build_success && error_cnt < 10000);
	}
	
	
	private void extractElementDataFromPageState(PageState page_state) {
		int error_cnt = 0;
		boolean page_state_build_success = false;
		Map<String, ElementState> reviewed_elements = new HashMap<>();
		error_cnt = 0;
		page_state_build_success = false;
		do {
			try {
				log.debug("Element Data Extractor retrieving browser connection ... "+page_state.getUrl());
				
				List<ElementState> elements = browser_service.extractElementStates(page_state.getSrc(), new URL(page_state.getUrl()));
			
				page_state.addElements(elements);
				page_state = page_state_service.save(page_state);
				//send page state message to auditor
				getSender().tell(page_state, getSelf());
				break;
			}catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				
			}
		}while(!page_state_build_success && error_cnt < 10000);
	}	
}
