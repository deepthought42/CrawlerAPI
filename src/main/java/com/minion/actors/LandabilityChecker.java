package com.minion.actors;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.minion.browsing.BrowserFactory;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.services.BrowserService;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Able to receive a page state and check if it is landable(user can reach page state as first state at a given url)
 *  in a desired browser
 */
@Component
@Scope("prototype")
public class LandabilityChecker extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	PageStateRepository page_state_repo;
	
	@Autowired
	BrowserService browser_service;
	
	public static Props props() {
		return Props.create(LandabilityChecker.class);
	}
	
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

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(BrowserPageState.class, bps -> {
					PageState page_state = bps.page;

					boolean page_visited_successfully = false;
					int cnt  = 0;
					
					PageState page_state_record = page_state_repo.findByKey(page_state.getKey());
					if(page_state_record != null){
						page_state = page_state_record;
					}
					page_state.setLastLandabilityCheck(LocalDateTime.now());
					page_state = page_state_repo.save(page_state);

					do{
						page_visited_successfully = false;
						Browser landable_browser = null;
						try{							
							landable_browser = BrowserFactory.buildBrowser(bps.browser_name, BrowserEnvironment.DISCOVERY);
							landable_browser.navigateTo(page_state.getUrl());
							log.info("screenshots of page state :: "+page_state.getBrowserScreenshots().size());
							if(page_state.equals(browser_service.buildPage(landable_browser))){
								page_state.setLandable(true);
							}
							else{
								page_state.setLandable(false);
							}
							page_visited_successfully = true;
							page_state = page_state_repo.save(page_state);
						}
						catch(Exception e){
							landable_browser.close();
							log.warning(e.getLocalizedMessage());
						}
						finally{
							landable_browser.close();
						}

						cnt++;
					}while(!page_visited_successfully && cnt < Integer.MAX_VALUE);
					
					log.info("is page state landable  ?? :: "+page_state.isLandable());
					postStop();
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
				.matchAny(o -> log.info("received unknown message"))
				.build();
	}
	
	public static final class BrowserPageState {
		PageState page;
		String browser_name;
		
		public BrowserPageState(PageState page, String browser_name){
			this.page = page;
			this.browser_name = browser_name;
		}
	}
}
