package com.minion.actors;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.openqa.grid.common.exception.GridException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.qanairy.models.PageState;
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
					do{
						page_visited_successfully = false;
						
						try{
							PageState page_state_record = page_state_repo.findByKey(page_state.getKey());
							if(page_state_record != null){
								log.info("Landability checker found page state with key :: "+page_state.getKey());
								page_state = page_state_record;
							}
							Browser landable_browser = new Browser(bps.browser_name);
							landable_browser.navigateTo(page_state.getUrl());
							log.info("screenshots of page state :: "+page_state.getBrowserScreenshots().size());
							if(page_state.equals(browser_service.buildPage(landable_browser))){
								page_state.setLandable(true);
							}
							page_state.setLastLandabilityCheck(LocalDateTime.now());
							page_state.setLandable(true);
							page_visited_successfully = true;
							page_state_repo.save(page_state);
							landable_browser.close();
							break;
						}catch(GridException e){
							log.warning(e.getLocalizedMessage());
						}
						catch(NoSuchAlgorithmException e){
							log.warning("ERROR VISITING PAGE AT ::: "+page_state.getUrl().toString());
							log.warning(e.getLocalizedMessage());
						}
						catch(ClientException e){
							log.warning(e.getLocalizedMessage());
						}

						cnt++;
					}while(!page_visited_successfully && cnt < 500000);
					
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
