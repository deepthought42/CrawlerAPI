package com.minion.actors;

import org.openqa.grid.common.exception.GridException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.browsing.Browser;
import com.minion.structs.Message;
import com.qanairy.models.PageState;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.services.BrowserService;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
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

	public static Props props() {
	  return Props.create(LandabilityChecker.class);
	}
	
	@Autowired
	PageStateRepository page_state_repo;
	
	@Autowired
	BrowserService browser_service;
	
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
					boolean landable = false;

					boolean page_visited_successfully = false;
					int cnt  = 0;
					do{
						page_visited_successfully = false;

						try{
							Browser landable_browser = new Browser(bps.browser_name);
							landable_browser.navigateTo(page_state.getUrl());
							System.err.println("screenshots of page state :: "+page_state.getBrowserScreenshots().size());
							if(page_state.equals(browser_service.buildPage(landable_browser))){
								page_state.setLandable(true);
								page_state_repo.save(page_state);
								landable= true;
							}
							page_visited_successfully = true;

							landable_browser.close();

						}catch(GridException e){
							log.error(e.getMessage());
						}
						catch(Exception e){
							log.error("ERROR VISITING PAGE AT ::: "+page_state.getUrl().toString());
							log.error(e.getMessage());
						}

						cnt++;
					}while(!page_visited_successfully && cnt < 3);
					
					System.err.println("is page state landable  ?? :: "+landable);
					//return landable;
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
