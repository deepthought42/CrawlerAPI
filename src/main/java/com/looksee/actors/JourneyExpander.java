package com.looksee.actors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.browsing.ActionFactory;
import com.looksee.browsing.Browser;
import com.looksee.helpers.BrowserConnectionHelper;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserEnvironment;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.journeys.ElementInteractionStep;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.Step;
import com.looksee.models.journeys.StepExecutor;
import com.looksee.models.message.JourneyMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.TimingUtils;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import us.codecraft.xsoup.Xsoup;

/**
 * 
 * 
 */
@Component
@Scope("prototype")
@Deprecated
public class JourneyExpander extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(JourneyExpander.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private StepExecutor step_executor;
	
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
				.match(Journey.class, journey-> {
					log.warn("JOURNEY EXPANSION MANAGER received new JOURNEY for mapping");

					List<Journey> hover_interactions = new ArrayList<>();
					List<Journey> click_interactions = new ArrayList<>();
					List<String> interactive_elements = new ArrayList<>();
					
					boolean executed_successfully = false;
					int cnt = 0;
					Browser browser = null;
					do {
						try {
							//start a new browser session
							browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
							ActionFactory action_factory = new ActionFactory(browser.getDriver());
		
							log.warn("journey :: "+journey);
							log.warn("browser :: "+browser);
							executeJourney(journey, browser);
							String current_url = browser.getDriver().getCurrentUrl();
							log.warn("CURRENT URL   ::    "+current_url);
							//construct page and add page to list of page states
							URL page_url = new URL(current_url);							
		
							//build page state for baseline
							PageState journey_result_page = browser_service.buildPageState(page_url);
							journey_result_page = page_state_service.save(journey_result_page);
							//domain_service.addPage(domain.getId(), journey_result_page.getKey());

							Document doc = Jsoup.parse(journey_result_page.getSrc());
							
							//get all leaf elements 
							List<ElementState> leaf_elements = page_state_service.getVisibleLeafElements(journey_result_page.getKey());
							
							for(ElementState leaf_element : leaf_elements) {
								
								//check each leaf element for mouseover interaction
								WebElement web_element = browser.getDriver().findElement(By.xpath(leaf_element.getXpath()));
								action_factory.execAction(web_element, "", Action.MOUSE_OVER);
		
								Element element = Xsoup.compile(leaf_element.getXpath()).evaluate(doc).getElements().get(0);
								String css_selector = "";//generateXpathUsingJsoup(element, doc, attributes, xpath_cnt);

								ElementState new_element_state = BrowserService.buildElementState(
																					leaf_element.getXpath(), 
																					browser.extractAttributes(web_element), 
																					element,
																					web_element, 
																					leaf_element.getClassification(), 
																					Browser.loadCssProperties(web_element, 
																					browser.getDriver()),
																					"",
																					css_selector);
								
								new_element_state = element_state_service.save(new_element_state);
								//if page url is not the same as journey result page url then load new page for this
								//construct page and add page to list of page states
								URL new_page_url = new URL(current_url);
								PageState exploration_result_page = browser_service.buildPageState(new_page_url, browser, null);
								log.warn("Page state built in journey explorer");

								log.warn("journey result page key :: "+journey_result_page.getKey());
								log.warn("exploration result page ::  "+exploration_result_page.getKey());
								log.warn("journey result matches exploration result?   " + journey_result_page.equals(exploration_result_page));
								//check if page state is same as original page state. If not then add new ElementInteractionStep 
								if(!journey_result_page.equals(exploration_result_page)) {
									exploration_result_page = page_state_service.save(exploration_result_page);
									
									log.warn("creating new element interaction step .... "+new_element_state);
									Step step = new ElementInteractionStep(journey_result_page, new_element_state, Action.MOUSE_OVER, exploration_result_page);
									if(existsInJourney(journey, step)) {
										continue;
									}
									step = step_service.save(step);
									//add element back to service step
									//clone journey and add this step at the end
									List<Step> steps = new ArrayList<>(journey.getSteps());
									steps.add(step);
									List<Long> ordered_ids = new ArrayList<>(journey.getOrderedIds());
									ordered_ids.add(step.getId());
									Journey new_journey = new Journey(steps, ordered_ids);
									
									//add journey to list of elements to explore for click or typing interactions
									getSender().tell(new_journey, getSelf());
									//hover_interactions.add(new_journey);
									interactive_elements.add(leaf_element.getKey());
								}
							}
	
							log.warn("sending "+hover_interactions.size()+ " hover interactions to Journey Manager +++");
							executed_successfully = true;
							break;
						}
						catch(Exception e) {
							log.warn("Exception occurred while executing journey ::   "+e.getMessage());
							e.printStackTrace();
							if(browser != null) {
								browser.close();
							}
						}
						TimingUtils.pauseThread(15000L);
					}while(!executed_successfully && cnt < 50);
					
					

					/*
					
					
					for(ElementState leaf_element : leaf_elements) {
						if(interactive_elements.contains(leaf_element.getKey())) {
							continue;
						}
						
						//check each leaf element for click interaction
						WebElement web_element = browser.getDriver().findElement(By.xpath(leaf_element.getXpath()));
						action_factory.execAction(web_element, "", "click");

						Element element = Xsoup.compile(leaf_element.getXpath()).evaluate(doc).getElements().get(0);
						ElementState new_element_state = BrowserService.buildElementState(
								leaf_element.getXpath(), 
								browser.extractAttributes(web_element), 
								element,
								web_element, 
								leaf_element.getClassification(), 
								Browser.loadCssProperties(web_element, browser.getDriver()));
						new_element_state = element_state_service.save(new_element_state);
						
						//if page url is not the same as journey result page url then load new page for this
						String url_after_interaction = browser.getDriver().getCurrentUrl();
						PageState exploration_result_page = browser_service.buildPageState(page, browser);
						for(ElementState element_state : exploration_result_page.getElements()) {
							WebElement new_web_element = browser.getDriver().findElement(By.xpath(element_state.getXpath()));

							element_state.setRenderedCssValues(Browser.loadCssProperties(new_web_element, browser.getDriver()));
							element_state.setVisible(new_web_element.isDisplayed());
						}
						
						//check if page state is same as original page state. If not then add new ElementInteractionStep 
						if(!journey_result_page.equals(exploration_result_page)) {
							Step step = new ElementInteractionStep(journey_result_page, new_element_state, new Action("mouseover"), exploration_result_page);
							//clone journey and add this step at the end
							Journey new_journey = journey.clone();
							new_journey.addStep(step);
							//add journey to list of elements to explore for click or typing interactions
							click_interactions.add(new_journey);
							
							//reset state by executing the journey again
							executeJourney(journey, browser);
						}
					}
					
					log.warn("sending  "+click_interactions.size()+ "  click interactions to Journey Manager +++");
					for(Journey click_journey : click_interactions) {
						getSender().tell(click_journey, getSelf());
					}
					
					
					for(Journey hover_journey : hover_interactions) {
						executeJourney(hover_journey, browser);
						ElementState last_element = JourneyUtils.extractLastElement(hover_journey);
						//check each leaf element for click interaction
						WebElement web_element = browser.getDriver().findElement(By.xpath(last_element.getXpath()));
						action_factory.execAction(web_element, "", "click");

						Element element = Xsoup.compile(last_element.getXpath()).evaluate(doc).getElements().get(0);
						ElementState new_element_state = BrowserService.buildElementState(
								last_element.getXpath(), 
								browser.extractAttributes(web_element), 
								element,
								web_element, 
								last_element.getClassification(), 
								Browser.loadCssProperties(web_element, browser.getDriver()));
						new_element_state = element_state_service.save(new_element_state);
						
						//if page url is not the same as journey result page url then load new page for this
						String url_after_interaction = browser.getDriver().getCurrentUrl();
						PageState exploration_result_page = browser_service.buildPageState(page, browser);
						
						for(ElementState element_state : exploration_result_page.getElements()) {
							WebElement new_web_element = browser.getDriver().findElement(By.xpath(element_state.getXpath()));

							element_state.setRenderedCssValues(Browser.loadCssProperties(new_web_element, browser.getDriver()));
							element_state.setVisible(new_web_element.isDisplayed());
						}
						
						
						//check if page state is same as original page state. If not then add new ElementInteractionStep 
						if(!journey_result_page.equals(exploration_result_page)) {
							Step step = new ElementInteractionStep(journey_result_page, new_element_state, new Action("mouseover"), exploration_result_page);
							//clone journey and add this step at the end
							Journey new_journey = journey.clone();
							new_journey.addStep(step);
							//add journey to list of elements to explore for click or typing interactions
							click_interactions.add(new_journey);
							
							//TODO : reset state by executing the journey again
							executeJourney(journey, browser);
									
						}
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
	
	private boolean existsInJourney(Journey journey, Step step) {
		for(Step journey_step : journey.getSteps()) {
			if(step.getKey().contentEquals(step.getKey())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param journey
	 * @param browser
	 */
	private void executeJourney(Journey journey, Browser browser) {
		assert journey != null;
		assert browser != null;
		
		List<Step> ordered_steps = new ArrayList<>();
		//execute journey steps
		for(long step_id : journey.getOrderedIds()) {
			
			for(Step step: journey.getSteps()) {
				if(step.getId() == step_id) {
					ordered_steps.add(step);
					break;
				}
			}
		}

		for(Step step : ordered_steps) {
			
			log.warn("step :: "+step);
			//execute step
			step_executor.execute(browser, step);
		}
	}	
}
