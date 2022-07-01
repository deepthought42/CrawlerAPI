package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
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
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.BrowserCrawlActionMessage;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.PathUtils;
import com.looksee.utils.TimingUtils;

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
	private BrowserService browser_service;
	
	private Account account;

	private ActorRef crawl_actor;
	private int page_audits_completed;
	private List<Step> steps;

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
					this.crawl_actor = getContext().getParent();
					try {
						log.warn("JOURNEY MAPPING MANAGER received new URL for mapping");
						this.steps = message.getSteps();
						PageState initial_page = message.getSteps().get(0).getStartPage();
						//navigate to url of first page state in first journey step
						Browser browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
						String sanitized_url = BrowserUtils.sanitizeUrl(initial_page.getUrl(), initial_page.isSecure());
						log.warn("nagivating to url :: "+sanitized_url);
						browser.navigateTo(sanitized_url);
						log.warn("(Journey Executor) executing "+message.getSteps().size()+" steps in journey....");
						//execute all steps sequentially in the journey
						executeAllStepsInJourney(message.getSteps(), browser);
						//send build page message to PageStateBuilder
						BrowserCrawlActionMessage browser_page_builder_message = new BrowserCrawlActionMessage(message.getDomainId(), 
																												message.getAccountId(), 
																												message.getAuditRecordId(), 
																												new URL(browser.getDriver().getCurrentUrl()), 
																												browser);
						
						ActorRef page_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
															.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_builder.tell(browser_page_builder_message, getSelf());

					}
					catch(ElementNotInteractableException e) {
						e.printStackTrace();
					}				
				})
				.match(PageDataExtractionMessage.class, message -> {
					log.warn("Journey executor received page data extraction message :: "+message.getPageState().getKey());
					this.steps.get(this.steps.size()-1).setEndPage(message.getPageState());
					this.steps.get(this.steps.size()-1).setKey(this.steps.get(this.steps.size()-1).generateKey());
					PageState second_to_last_page = PathUtils.getSecondToLastPageState(this.steps);
					//is end_page PageState different from second to last PageState
					if(message.getPageState().equals(second_to_last_page)) {
						log.warn("returning because message page state is equal to second to last page");
						
						//tell parent that we processed a journey that is being discarded
						DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	BrowserType.CHROME, 
																								message.getDomainId(), 
																								message.getAccountId(),
																								message.getAuditRecordId());
						return;
					}
					
					ConfirmedJourneyMessage journey_message = new ConfirmedJourneyMessage(this.steps, 
																						PathStatus.EXAMINED, 
																						BrowserType.CHROME, 
																						message.getDomainId(), 
																						message.getAccountId(),
																						message.getAuditRecordId());
					
					crawl_actor.tell(journey_message, getSelf());
				})
				.match(PageDataExtractionError.class, message -> {
					try {
						log.warn("JOURNEY MAPPING MANAGER received page data extraction error");
						PageState initial_page = this.steps.get(0).getStartPage();
						//navigate to url of first page state in first journey step
						Browser browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
						String sanitized_url = BrowserUtils.sanitizeUrl(initial_page.getUrl(), true);
						browser.navigateTo(sanitized_url);
						log.warn("(Journey Executor - PageDataExtractionError) executing "+this.steps.size()+" steps in journey....");

						//execute all steps sequentially in the journey
						executeAllStepsInJourney(this.steps, browser);
						log.warn("current browser url :: "+browser.getDriver().getCurrentUrl());
						//send build page message to PageStateBuilder
						BrowserCrawlActionMessage browser_page_builder_message = new BrowserCrawlActionMessage(message.getDomainId(), 
																												message.getAccountId(), 
																												message.getAuditRecordId(), 
																												new URL(browser.getDriver().getCurrentUrl()), 
																												browser);
						
						ActorRef page_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
															.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_builder.tell(browser_page_builder_message, getSelf());
					}
					catch(ElementNotInteractableException e) {
						e.printStackTrace();
					}
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
	
	/**
	 * Executes all {@link Step steps} within a browser
	 * 
	 * @param steps
	 * @param browser
	 */
	private void executeAllStepsInJourney(List<Step> steps, Browser browser) {
		ActionFactory action_factory = new ActionFactory(browser.getDriver());
		for(Step step: steps) {
			if(step instanceof SimpleStep) {
				log.warn("step id :: "+step.getId());
				log.warn("Step starting url :: "+step.getStartPage().getUrl());
				log.warn("element xpath :: "+((SimpleStep)step).getElementState().getXpath());

				WebElement web_element = browser.getDriver().findElement(By.xpath(((SimpleStep)step).getElementState().getXpath()));
				
				action_factory.execAction(web_element, "", ((SimpleStep)step).getAction());
			}
			else if(step instanceof LoginStep) {
				LoginStep login_step = (LoginStep)step;
				WebElement username_element = browser.getDriver().findElement(By.xpath(login_step.getUsernameElement().getXpath()));
				action_factory.execAction(username_element, login_step.getTestUser().getUsername(), Action.SEND_KEYS);
				
				WebElement password_element = browser.getDriver().findElement(By.xpath(login_step.getPasswordElement().getXpath()));
				action_factory.execAction(password_element, login_step.getTestUser().getPassword(), Action.SEND_KEYS);
				WebElement submit_element = browser.getDriver().findElement(By.xpath(login_step.getSubmitElement().getXpath()));
				
				action_factory.execAction(submit_element, "", Action.CLICK);
			}
			if(step.getEndPage() == null || step.getEndPage().getKey() != step.getStartPage().getKey()) {
				TimingUtils.pauseThread(5000L);
			}
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
