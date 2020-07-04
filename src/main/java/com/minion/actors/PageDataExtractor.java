package com.minion.actors;


import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.WebDriverException;
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
import com.qanairy.utils.TimingUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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
	private ActorSystem actor_system;
	
	@Autowired
	private PageService page_service;
	
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
				.match(Page.class, page-> {
					log.warn("Page data extractor received Page Message..."+page.getUrl());
					int error_cnt = 0;
					boolean page_state_build_success = false;
					Browser browser = null;
					do {
						try {
							log.debug("retrieving browser connection ... ");
							browser = BrowserConnectionHelper.getConnection(BrowserType.create("chrome"), BrowserEnvironment.DISCOVERY);
							log.debug("navigating to url :: "+page.getUrl());
							browser.navigateTo(page.getUrl());
		
							//build page state with element states at the same time
							log.debug("building page state...");
							PageState page_state = browser_service.buildPageState( page, browser );
							page.addPageState(page_state);
							page = page_service.save(page);
							page_state_build_success = true;
							
							log.warn("sending page state to element data extractor..."+page_state.getUrl());
							ActorRef element_data_extractor = actor_system.actorOf(SpringExtProvider.get(actor_system)
									.props("elementDataExtractor"), "elementDataExtractor"+UUID.randomUUID());
							element_data_extractor.tell(page_state, getSender());
							break;
						}catch(Exception e) {
							if(e instanceof GridException || e instanceof WebDriverException) {
								log.debug("Exception thrown during page data extractions");
							}
							else {
								e.printStackTrace();
							}
							error_cnt++;
						}
						finally {
							if( browser != null ) {
								browser.close();
								browser = null;
							}							
						}
					}while(!page_state_build_success && error_cnt < 10000);

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
}
