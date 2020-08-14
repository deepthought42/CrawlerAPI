package com.minion.actors;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.services.BrowserService;
import com.qanairy.services.PageService;
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
public class PageDataExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(PageDataExtractor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
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
				.match(Page.class, page-> {
					log.warn("Page data extractor received Page Message..."+page.getUrl());
					int cnt = 0;
					do {
						try {
							//build page state with element states at the same time
							log.warn("building page state...");
							Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
							browser.navigateTo("http://"+page.getUrl());
							String page_src = browser.getDriver().getPageSource();
							String current_url = browser.getDriver().getCurrentUrl();
							String title = browser.getDriver().getTitle();
							
							PageState page_state = browser_service.buildPageState( page, page_src, current_url, title, browser);
							browser.close();

							log.warn("saving page state to database");
							page_state = page_state_service.save(page_state);
							page_service.addPageState(page.getKey(), page_state.getKey());
							
							log.warn("sending page state to audit manager..."+page_state.getUrl());
							
							getSender().tell(page_state, getSelf());
							cnt = 100;
							break;
						}catch(Exception e) {
							log.warn("problem saving page state during data extraction...."+e.getMessage());
							e.printStackTrace();
						}
						cnt++;
					}while(cnt < 5);
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
}
