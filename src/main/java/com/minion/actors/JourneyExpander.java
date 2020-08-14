package com.minion.actors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.minion.browsing.ActionFactory;
import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.models.journeys.ElementInteractionStep;
import com.qanairy.models.journeys.Journey;
import com.qanairy.models.journeys.Step;
import com.qanairy.services.BrowserService;
import com.qanairy.services.ElementStateService;
import com.qanairy.services.PageService;
import com.qanairy.services.PageStateService;
import com.qanairy.services.StepService;
import com.qanairy.utils.JourneyUtils;

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
public class JourneyExpander extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(JourneyExpander.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageService page_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private StepService step_service;
	
	
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
					log.warn("JOURNEY EXPANSION MANAGER received new URL for mapping");

					List<Journey> hover_interactions = new ArrayList<>();
					List<Journey> click_interactions = new ArrayList<>();

					List<String> interactive_elements = new ArrayList<>();

					//start a new browser session
					Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
					ActionFactory action_factory = new ActionFactory(browser.getDriver());

					log.warn("journey :: "+journey);
					log.warn("browser :: "+browser);
					executeJourney(journey, browser);
					String current_url = browser.getDriver().getCurrentUrl();
					//construct page and add page to list of page states
					URL page_url = new URL(current_url);
					String path = page_url.getPath();
					Page page = new Page(new ArrayList<>(), Browser.cleanSrc(browser.getDriver().getPageSource()), browser.getDriver().getTitle(), page_url.toString(), path);
					page = page_service.save( page );

					//build page state for baseline
					PageState journey_result_page = browser_service.buildPageState(page, browser);
					Document doc = Jsoup.parse(journey_result_page.getSrc());
					
					//get all leaf elements 
					List<ElementState> leaf_elements = page_state_service.getVisibleLeafElements(journey_result_page.getKey());
					
					for(ElementState leaf_element : leaf_elements) {
						
						//check each leaf element for mouseover interaction
						WebElement web_element = browser.getDriver().findElement(By.xpath(leaf_element.getXpath()));
						action_factory.execAction(web_element, "", "mouseover");

						Element element = Xsoup.compile(leaf_element.getXpath()).evaluate(doc).getElements().get(0);
						ElementState new_element_state = BrowserService.buildElementState(
								leaf_element.getXpath(), 
								browser.extractAttributes(web_element), 
								element, leaf_element.getClassification(), 
								Browser.loadCssProperties(web_element, browser.getDriver()));
						new_element_state.setVisible(web_element.isDisplayed());
						
						//if page url is not the same as journey result page url then load new page for this
						//construct page and add page to list of page states
						URL new_page_url = new URL(current_url);
						String new_path = page_url.getPath();
						Page new_page = new Page(new ArrayList<>(), browser.cleanSrc(browser.getDriver().getPageSource()), browser.getDriver().getTitle(), (new_page_url.getHost()+new_path), new_path);						
						PageState exploration_result_page = browser_service.buildPageState(new_page, browser);
						for(ElementState element_state : exploration_result_page.getElements()) {
							WebElement new_web_element = browser.getDriver().findElement(By.xpath(element_state.getXpath()));

							element_state.setRenderedCssValues(Browser.loadCssProperties(new_web_element, browser.getDriver()));
							element_state.setVisible(new_web_element.isDisplayed());
						}
						
						//check if page state is same as original page state. If not then add new ElementInteractionStep 
						if(!journey_result_page.equals(exploration_result_page)) {
							Step step = new ElementInteractionStep(journey_result_page, new_element_state, new Action("mouseover"), exploration_result_page);
							step = step_service.save(step);
							//clone journey and add this step at the end
							Set<Step> steps = new HashSet<>(journey.getSteps());
							steps.add(step);
							List<String> ordered_keys = new ArrayList<>(journey.getOrderedKeys());
							ordered_keys.add(step.getKey());
							Journey new_journey = new Journey(steps, ordered_keys);
							
							//add journey to list of elements to explore for click or typing interactions
							hover_interactions.add(new_journey);
							interactive_elements.add(leaf_element.getKey());
						}
					}
					
					log.warn("sending "+hover_interactions.size()+ "  to Journey Manager +++");
					for(Journey hover_journey : hover_interactions) {
						getSender().tell(hover_journey, getSelf());
					}
					
					
					
					
					
					

					
					
					
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
								element, leaf_element.getClassification(), 
								Browser.loadCssProperties(web_element, browser.getDriver()));
						new_element_state.setVisible(web_element.isDisplayed());
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
					}
					
					log.warn("sending "+hover_interactions.size()+ "  to Journey Manager +++");
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
								element, last_element.getClassification(), 
								Browser.loadCssProperties(web_element, browser.getDriver()));
						new_element_state.setVisible(web_element.isDisplayed());
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
					}
				})
				.match(Journey.class, journey -> {

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
	
	private void executeJourney(Journey journey, Browser browser) {
		assert journey != null;
		assert browser != null;
		
		List<Step> ordered_steps = new ArrayList<>();
		
		//execute journey steps
		for(String step_key : journey.getOrderedKeys()) {
			for(Step step: journey.getSteps()) {
				if(step.getKey().contentEquals(step_key)) {
					ordered_steps.add(step);
					break;
				}
			}
		}
		
		for(Step step : ordered_steps) {
			
			log.warn("step :: "+step);
			//execute step
			step.execute(browser);
		}
	}	
}
