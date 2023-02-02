package com.looksee.actors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;
import com.looksee.utils.ListUtils;
import com.looksee.utils.PathUtils;
import com.looksee.utils.TimingUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
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
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
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
					log.warn("Received journey :: "+message.getId());
					List<Step> steps = new ArrayList<>(message.getSteps());
					try {
						PageState page_state = iterateThroughJourneySteps(steps, 
																		  message.getDomainId(), 
																		  message.getAccountId(), 
																		  message.getAuditRecordId());
						steps.get(steps.size()-1).setEndPage(page_state);
						steps.get(steps.size()-1).setKey(steps.get(steps.size()-1).generateKey());
						
					}
					catch(Exception e) {
						log.error("Exception occurred during journey execution");
						//e.printStackTrace();
					}
					log.warn("done processing journey :: "+message.getId());
					processIfStepsShouldBeExpanded(message.getId(), 
													steps, 
													message.getDomainId(), 
													message.getAccountId(), 
													message.getAuditRecordId());
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
	 * Checks if the last step in the list of steps has matching start and end page states
	 * 
	 * @param steps
	 * @param domain_id
	 * @param account_id
	 * @param audit_record_id
	 */
	private void processIfStepsShouldBeExpanded(int journey_id, 
												List<Step> steps, 
												long domain_id, 
												long account_id, 
												long audit_record_id) 
	{
		try {
			PageState second_to_last_page = PathUtils.getSecondToLastPageState(steps);
			PageState final_page = PathUtils.getLastPageState(steps);
			//is end_page PageState different from second to last PageState
			if(final_page == null || final_page.equals(second_to_last_page)) {
				//tell parent that we processed a journey that is being discarded
				DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	journey_id, 
																						BrowserType.CHROME, 
																						domain_id,
																						account_id, 
																						audit_record_id);
				getSender().tell(journey_message, getSelf());
			}
			else {
				ConfirmedJourneyMessage journey_message = new ConfirmedJourneyMessage(journey_id, 
																					ListUtils.clone(steps), 
																					PathStatus.EXAMINED, 
																					BrowserType.CHROME, 
																					domain_id,
																					account_id, 
																					audit_record_id);
				
				getSender().tell(journey_message, getSelf());
			}
		}catch(Exception e) {
			e.printStackTrace();
			DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	journey_id, 
					BrowserType.CHROME, 
					domain_id,
					account_id, 
					audit_record_id);
			getSender().tell(journey_message, getSelf());
		}
	}

	/**
	 * Constructs a {@link PageState page} including all {@link ElementState elements} on the page as a {@linkplain List}
	 * 
	 * @param audit_record_id
	 * @param browser
	 * @return
	 * @throws Exception
	 * 
	 * @pre browser != null
	 */
	private PageState buildPage(long audit_record_id, Browser browser) throws Exception {
		assert browser != null;
		
		URL current_url = new URL(browser.getDriver().getCurrentUrl());
		String url_without_protocol = BrowserUtils.getPageUrl(current_url.toString());
	
		PageState page_state = audit_record_service.findPageWithUrl(audit_record_id, url_without_protocol);
		if(page_state == null) {
			page_state = browser_service.performBuildPageProcess(browser);
			page_state = page_state_service.save(page_state);
			audit_record_service.addPageToAuditRecord(audit_record_id, page_state.getId());
		}
		List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
		List<ElementState> element_states = browser_service.buildPageElementsWithoutNavigation( page_state, 
																								xpaths,
																								audit_record_id,
																								page_state.getFullPageHeight(),
																								browser);

		element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
		page_state.setElements(element_states);
		
		return page_state;
	}
	
	/**
	 * Executes steps in sequence and builds the resulting page state
	 * 
	 * @param steps
	 * @param domain_id
	 * @param account_id
	 * @param audit_record_id
	 * @return {@link PageState} or null if final page is an external page
	 * @throws Exception
	 * 
	 * @pre steps != null
	 * @pre !steps.isEmpty()
	 */
	private PageState iterateThroughJourneySteps( List<Step> steps, 
												  long domain_id, 
												  long account_id, 
												  long audit_record_id
	) throws Exception {
		assert steps != null;
		assert !steps.isEmpty();
		
		boolean complete = false;
		int count = 0;
		PageState page = null;
		do {
			Browser browser = null;
			try {
				browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
				
				performJourneyStepsInBrowser(steps, browser);
				Domain domain = domain_service.findById(domain_id).get();
				//if page url already exists for domain audit record then load that page state instead of performing a build
				//NOTE: This patch is meant to reduce duplication of page builds and will not catch A/B tests
				String current_url = BrowserUtils.getPageUrl(browser.getDriver().getCurrentUrl());
				if(BrowserUtils.isExternalLink(domain.getUrl(), current_url)) {
					log.warn("current url is external : "+current_url);
					return null;
				}
				
				page = buildPage(audit_record_id, browser);
				complete = true;
			}
			catch(ElementNotInteractableException e ) {
				log.error("Element not interactable exception occurred!");
				//e.printStackTrace();
				complete=true;
			}
			catch(Exception e) {
				log.error("Error occurred while iterating through journey steps.");
				//e.printStackTrace();
			}
			finally {
				if(browser != null) {
					browser.close();
				}
			}
			count++;
		}while(!complete && count < 20);
		
		return page;
	}

	/**
	 * Creates {@link Browser} connection and performs journey steps
	 * 
	 * @param steps
	 * @param browser TODO
	 * 
	 * @return
	 * @throws Exception
	 * 
	 * @pre steps != null
	 * @pre !steps.isEmpty()
	 */
	private void performJourneyStepsInBrowser(List<Step> steps, Browser browser) throws Exception  {
		assert steps != null;
		assert !steps.isEmpty();
				
		PageState initial_page = steps.get(0).getStartPage();
		String sanitized_url = BrowserUtils.sanitizeUrl(initial_page.getUrl(), initial_page.isSecure());

		browser.navigateTo(sanitized_url);
		//execute all steps sequentially in the journey
		executeAllStepsInJourney(steps, browser);
	}

	/**
	 * Executes all {@link Step steps} within a browser
	 * 
	 * @param steps
	 * @param browser
	 */
	private void executeAllStepsInJourney(List<Step> steps, Browser browser) throws Exception{
		ActionFactory action_factory = new ActionFactory(browser.getDriver());
		for(Step step: steps) {
			if(step instanceof SimpleStep) {
				ElementState element = ((SimpleStep)step).getElementState();
				WebElement web_element = browser.getDriver().findElement(By.xpath(element.getXpath()));
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
			browser.waitForPageToLoad();
			//TimingUtils.pauseThread(2000L);
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
