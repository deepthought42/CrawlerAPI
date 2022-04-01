package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.browsing.ActionFactory;
import com.looksee.browsing.Browser;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.NavigationStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.JourneyMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.JourneyService;
import com.looksee.utils.PathUtils;

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
 * 
 * 
 */
@Component
@Scope("prototype")
public class JourneyExecutor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(JourneyExecutor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private BrowserService browser_service;
	
	private Account account;

	private int page_audits_completed;

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
		page_audits_completed = 0;
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
				.match(JourneyMessage.class, message-> {
					log.warn("JOURNEY MAPPING MANAGER received new URL for mapping");
					PageState initial_page = message.getSteps().get(0).getStartPage();
					//navigate to url of first page state in first journey step
					Browser browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
					browser.navigateTo(initial_page.getUrl());
					
					//execute all steps sequentially in the journey
					executeAllStepsInJourney(message.getSteps(), browser);
					
					//build final page state
					PageState end_page = browser_service.buildPageState(new URL(initial_page.getUrl()), browser, new URL(browser.getDriver().getCurrentUrl()));
					message.getSteps().get(message.getSteps().size()-1).setEndPage(end_page);
					
					PageState second_to_last_page = PathUtils.getSecondToLastPageState(message.getSteps());
					//is end_page PageState different from second to last PageState
					if(end_page.equals(second_to_last_page)) {
						return;
					}
					
					getContext().getParent().tell(message, getSelf());
					
					//create navigation step
					/*
					NavigationStep step = new NavigationStep(url);
					
					Set<Step> steps = new HashSet<>();
					steps.add(step);
					List<String> ordered_keys = new ArrayList<>();
					ordered_keys.add(step.getKey());
					
					//Create new Journey with navigation step
					Journey journey = new Journey(steps, ordered_keys);
					journey_service.save(journey);
					//send Journey to JourneyExplorer actor
					ActorRef journeyExpander = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("journeyExpander"), "journeyExpander"+UUID.randomUUID());
					journeyExpander.tell(journey, getSelf());	
					*/
					
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
	
	private void executeAllStepsInJourney(List<Step> steps, Browser browser) {
		ActionFactory action_factory = new ActionFactory(browser.getDriver());

		for(Step step: steps) {
			WebElement web_element = browser.getDriver().findElement(By.xpath(step.getElementState().getXpath()));
			
			action_factory.execAction(web_element, "", step.getAction());
		}
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public int getPageAuditsCompleted() {
		return page_audits_completed;
	}

	public void setPageAuditsCompleted(int page_audits_completed) {
		this.page_audits_completed = page_audits_completed;
	}
	
	
}
