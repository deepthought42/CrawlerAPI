package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ThreadUtils;
import org.openqa.grid.common.exception.GridException;
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
import com.looksee.models.message.BrowserCrawlActionMessage;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;
import com.looksee.utils.JourneyUtils;
import com.looksee.utils.ListUtils;
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
import io.github.resilience4j.retry.annotation.Retry;

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
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	private Account account;

	private ActorRef crawl_actor;
	private int page_audits_completed;
	private List<Step> steps = new ArrayList<>();

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
						List<Step> steps = new ArrayList<>(message.getSteps());
						this.steps = new ArrayList<>(message.getSteps());
						PageState page_state = iterateThroughJourneySteps(steps, 
																		  message.getDomainId(), 
																		  message.getAccountId(), 
																		  message.getAuditRecordId());
						steps.get(steps.size()-1).setEndPage(page_state);
						steps.get(steps.size()-1).setKey(steps.get(steps.size()-1).generateKey());
						
						processIfStepsShouldBeExpanded(steps, message.getDomainId(), message.getAccountId(), message.getAuditRecordId());
					}
					catch(Exception e) {
						e.printStackTrace();
					}				
				})
				.match(PageDataExtractionMessage.class, message -> {
					log.warn("Journey executor received page data extraction message :: "+message.getPageState().getUrl());
					this.steps.get(this.steps.size()-1).setEndPage(message.getPageState());
					this.steps.get(this.steps.size()-1).setKey(this.steps.get(this.steps.size()-1).generateKey());
					
					log.warn("does steps list contain LOGIN? :: "+JourneyUtils.hasLoginStep(this.steps));
					
					processIfStepsShouldBeExpanded(this.steps, message.getDomainId(), message.getAccountId(), message.getAuditRecordId());
				})
				.match(PageDataExtractionError.class, message -> {
					log.warn("(Journey Executor - PageDataExtractionError) executing journey steps :: "+this.steps);

					if( message.getErrorMessage().contains("Received 404 status while building page state")) {
						log.warn("returning because 404 status was encountered for page state");
						
						//tell parent that we processed a journey that is being discarded
						DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	BrowserType.CHROME, 
																								message.getDomainId(), 
																								message.getAccountId(),
																								message.getAuditRecordId());
						crawl_actor.tell(journey_message, getSelf());
					}
					else {
						try {
							PageState page_state = iterateThroughJourneySteps(new ArrayList<>(this.steps), 
																			  message.getDomainId(), 
																			  message.getAccountId(), 
																			  message.getAuditRecordId());
							steps.get(steps.size()-1).setEndPage(page_state);
							steps.get(steps.size()-1).setKey(steps.get(steps.size()-1).generateKey());
							
							processIfStepsShouldBeExpanded(steps, message.getDomainId(), message.getAccountId(), message.getAuditRecordId());
							log.warn("journey executor - PageDataExtractionError) completed execution of steps with Login :: " +JourneyUtils.hasLoginStep(this.steps));
						}
						catch(ElementNotInteractableException e) {
							e.printStackTrace();
						}
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
	 * Checks if the last step in the list of steps has matching start and end page states
	 * 
	 * @param steps
	 * @param domain_id
	 * @param account_id
	 * @param audit_record_id
	 */
	private void processIfStepsShouldBeExpanded(List<Step> steps, long domain_id, long account_id, long audit_record_id) {
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(steps);
		PageState final_page = PathUtils.getLastPageState(steps);
		//is end_page PageState different from second to last PageState
		if(final_page == null) {
			log.warn("Final page in Step is null");
			log.warn("Steps affected :: "+steps);
			//tell parent that we processed a journey that is being discarded
			DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	BrowserType.CHROME, 
																					domain_id, 
																					account_id,
																					audit_record_id);
			crawl_actor.tell(journey_message, getSelf());
		}
		else if(final_page.equals(second_to_last_page)) {
			log.warn("Message page state is equal to second to last page for LOGIN step");
			log.warn("steps :: "+this.steps);
			//tell parent that we processed a journey that is being discarded
			DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	BrowserType.CHROME, 
																					domain_id, 
																					account_id,
																					audit_record_id);
			crawl_actor.tell(journey_message, getSelf());
		}
		else {
			ConfirmedJourneyMessage journey_message = new ConfirmedJourneyMessage(ListUtils.clone(this.steps), 
																				PathStatus.EXAMINED, 
																				BrowserType.CHROME, 
																				domain_id, 
																				account_id,
																				audit_record_id);
			
			crawl_actor.tell(journey_message, getSelf());
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
		log.warn("looking up page with url = "+url_without_protocol + " for audit record id = "+audit_record_id);
		PageState page_record = audit_record_service.findPageWithUrl(audit_record_id, url_without_protocol);
		PageState page_state = null;
		if(page_record == null) {
			page_state = browser_service.performBuildPageProcess(browser);
			page_state = page_state_service.save(page_state);
			audit_record_service.addPageToAuditRecord(audit_record_id, page_state.getId());
		}
		else {
			page_state = page_record;
		}

		log.warn("retrieving element states....");
		List<ElementState> saved_elements = page_state_service.getElementStates(page_state.getId());
		log.warn("retrieved "+saved_elements.size() + " elements for page state : "+page_state.getId());
		//if(elements.isEmpty()) {
		List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
		List<String> unexplored_xpaths = new ArrayList<String>();
		
		for(String xpath: xpaths) {
			boolean match_found = false;
			for(ElementState element: saved_elements) {
				if(element.getXpath().contentEquals(xpath)) {
					match_found = true;
					break;
				}
			}
			if(!match_found) {
				unexplored_xpaths.add(xpath);
			}
		}
		
		//crawl_action.getAuditRecord().setPageState(page_state_record);
		log.warn("building page elements without navigation...");
		List<ElementState> element_states = browser_service.buildPageElementsWithoutNavigation( page_state, 
																								unexplored_xpaths,
																								audit_record_id,
																								page_state.getFullPageHeight(),
																								browser);

		log.warn("enriching "+element_states.size()+" elements....");
		element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
		
		//save elements
		element_states = element_state_service.saveAll(element_states, page_state.getId());
		saved_elements.addAll(element_states);
		page_state.setElements(saved_elements);
		
		List<Long> element_ids = element_states.parallelStream().map(element -> element.getId()).collect(Collectors.toList());
		page_state_service.addAllElements(page_state.getId(), element_ids);
		/*}
		else {
			page_state.setElements(elements);
		}*/
		
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
				log.warn("retriving domain with id :: " + domain_id + "....");
				Domain domain = domain_service.findById(domain_id).get();
				//build page
				//if page url already exists for domain audit record then load that page state instead of performing a build
				//NOTE: This patch is meant to reduce duplication of page builds and will not catch A/B tests
				log.warn("retrieving current page url ...");
				String current_url = BrowserUtils.getPageUrl(browser.getDriver().getCurrentUrl());
				if(BrowserUtils.isExternalLink(domain.getUrl(), current_url)) {
					log.warn("current url is external : "+current_url);
					return null;
				}
				//page = audit_record_service.findPageWithUrl(audit_record_id, current_url);
				
				log.warn("building page state for end of journey...");
				page = buildPage(audit_record_id, browser);
				log.warn("Page elements after page build : "+page.getElements().size() + " :: "+page.getId());
				//}
				/*else {
					//load page elements
					page.setElements(page_state_service.getElementStates(page.getKey()));
					log.warn("Page state with URL "+current_url+" found for DomainAuditRecord.");
				}*/
				complete = true;
			}
			catch(Exception e) {
				log.error("Error occurred while iterating through journey steps ");
				e.printStackTrace();
				if(browser != null) {
					browser.close();
				}
			}
			finally {
				if(browser != null) {
					browser.close();
				}
			}
			count++;
		}while(!complete && count < 1000);
		
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
		log.warn("Done executing steps for journey");
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
			browser.waitForPageToLoad();
			TimingUtils.pauseThread(2000L);
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
