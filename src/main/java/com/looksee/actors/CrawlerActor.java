package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.Form;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.FormType;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.BrowserCrawlActionMessage;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.PageUtils;
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

@Component
@Scope("prototype")
public class CrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(CrawlerActor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DomainService domain_service;
	
	private ActorRef audit_manager;
	
	List<PageState> visited_pages = new ArrayList<>();
	private Map<String, List<ElementState>> explored_elements = new HashMap<>();
	
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
				.match(CrawlActionMessage.class, message -> {
					audit_manager = getContext().getParent();
					if(CrawlAction.START.equals(message.getAction())) {
						log.warn("STARTING crawl actor. Sending message to page state builder");
						
						
						ActorRef page_state_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
								  .props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
						page_state_builder.tell(message, getSelf());
					}
					else if(CrawlAction.STOP.equals(message.getAction())) {
						
					}
				})
				.match(PageDataExtractionMessage.class, msg -> {			
					log.warn("path expansion actor received PageDataExtraction Message");
					List<ElementState> elements = msg.getPageState().getElements();

					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					List<ElementState> filtered_elements = elements.parallelStream()
																	.filter(element -> element.isVisible())
																	.filter(element -> {
																			Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																			return BrowserService.hasWidthAndHeight(dimension);
																	})
																	.filter(element -> element.getXLocation() >= 0 && element.getYLocation() >= 0)
																	.filter(element -> isInteractiveElement(element))
																	.filter(element -> !isElementExplored(msg.getPageState().getKey(), element))
																	.filter(element -> !isFormElement(element))
																	.collect(Collectors.toList());
					
					Set<Form> forms = PageUtils.extractAllForms(msg.getPageState());
					Set<Form> unexplored_forms = new HashSet<>();
					
					for(Form form: forms) {
						if(!isElementExplored(msg.getPageState().getKey(), form.getFormTag())) {
							unexplored_forms.add(form);
						}
						addElementsToExploredMap(msg.getPageState(), form.getFormTag());
					}
					
					//generate form journeys
					List<Step> form_steps = generateFormSteps(msg.getDomainId(), msg.getPageState(), unexplored_forms);
					for(Step step: form_steps) {
						List<Step> step_list = new ArrayList<>();
						step_list.add(step);
						JourneyMessage journey_msg = new JourneyMessage(step_list, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		msg.getDomainId(), 
																		msg.getAccountId(),
																		msg.getAuditRecordId());
						
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
								.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
					
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(msg.getPageState(), filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(msg.getPageState(), filtered_elements);
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> steps = new ArrayList<>();
						steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		msg.getDomainId(), 
																		msg.getAccountId(),
																		msg.getAuditRecordId());
						
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
				})
				.match(BrowserCrawlActionMessage.class, message -> {
					ActorRef page_builder = getContext().actorOf(SpringExtProvider.get(actor_system)
														.props("pageStateBuilder"), "pageStateBuilder"+UUID.randomUUID());
					page_builder.tell(message, getSelf());
				})
				.match(ConfirmedJourneyMessage.class, message -> {
					log.warn("crawler received confirmed journey message :: "+message.getSteps().size() + " steps");
					PageState final_page = PathUtils.getLastPageState(message.getSteps());
					Domain domain = domain_service.findById(message.getDomainId()).get();
					if( BrowserUtils.isExternalLink(domain.getUrl(), final_page.getUrl()) 
							|| visited_pages.contains(final_page)/*|| pageState is in visited list*/) {
						log.warn("final page has already been visited --------------------------");
						audit_manager.tell(message, getSelf());
						return;
					}

					//Add page state to visited list
					visited_pages.add(final_page);
					
					//retrieve all ElementStates from journey
					List<ElementState> elements = final_page.getElements();
					final String last_page_key = final_page.getKey();
					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					List<ElementState> filtered_elements = elements.parallelStream()
																	.filter(element -> element.isVisible())
																	.filter(element -> {
																			Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																			return BrowserService.hasWidthAndHeight(dimension);
																	})
																	.filter(element -> element.getXLocation() > 0 && element.getYLocation() > 0)
																	.filter(element -> isInteractiveElement(element))
																	.filter(element -> !isElementExplored(last_page_key, element))
																	.filter(element -> !isFormElement(element))
																	.collect(Collectors.toList());
					
					Set<Form> forms = PageUtils.extractAllForms(final_page);
					Set<Form> unexplored_forms = new HashSet<>();
					
					for(Form form: forms) {
						if(!isElementExplored(final_page.getKey(), form.getFormTag())) {
							unexplored_forms.add(form);
						}
						addElementsToExploredMap(final_page, form.getFormTag());
					}
					
					//generate form journeys
					List<Step> steps = generateFormSteps(message.getDomainId(), final_page, unexplored_forms);
					for(Step step: steps) {
						List<Step> steps_list = new ArrayList<>(message.getSteps());
						
						if(!steps.contains(step)) {
							steps_list.add(step);
						}
						else {
							log.warn("FORM STEP EXISTS IN STEP LIST ALREADY");
						}
						
						JourneyMessage journey_msg = new JourneyMessage(steps_list, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId(),
																		message.getAuditRecordId());
						
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
													.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
					
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(final_page, filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(final_page, filtered_elements);
					
					if(new_steps.isEmpty()) {
						audit_manager.tell(message, getSelf());
						return;
					}
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> cloned_steps = new ArrayList<>(message.getSteps());
						cloned_steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(cloned_steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId(),
																		message.getAuditRecordId());
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
				})
				.match(PageDataExtractionMessage.class, message -> {
					log.warn("path expansion actor received PageDataExtraction Message");
					List<ElementState> elements = message.getPageState().getElements();

					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					List<ElementState> filtered_elements = elements.parallelStream()
																	.filter(element -> element.isVisible())
																	.filter(element -> {
																			Dimension dimension = new Dimension(element.getWidth(), element.getHeight()); 
																			return BrowserService.hasWidthAndHeight(dimension);
																	})
																	.filter(element -> element.getXLocation() >= 0 && element.getYLocation() >= 0)
																	.filter(element -> isInteractiveElement(element))
																	.filter(element -> !isElementExplored(message.getPageState().getKey(), element))
																	.filter(element -> !isFormElement(element))
																	.collect(Collectors.toList());
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(message.getPageState(), filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(message.getPageState(), filtered_elements);
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> steps = new ArrayList<>();
						steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId(),
																		message.getAuditRecordId());
						
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
					}
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
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
	
	private List<Step> generateFormSteps(long domain_id, PageState pageState, Set<Form> forms) {
		List<Step> steps = new ArrayList<>();
		for(Form form: forms) {
			
			if(FormType.LOGIN.equals(form.getType())){
				//retrieve user credentials
				TestUser user = domain_service.findTestUser(domain_id).get(0);
				//get username field
				ElementState username_element = getFormField(form.getFormFields(), "username");
				//create step to enter username into username field
				
				//get password field
				ElementState password_element = getFormField(form.getFormFields(), "password");

				//create step to enter password into password field
				//get submit button
				ElementState submit_btn = form.getSubmitField();
				//create step to click on submit button
				steps.add( new LoginStep(pageState, null, username_element, password_element, submit_btn, user) );
			}
			else if(FormType.REGISTRATION.equals(form.getType())) {
				//generate username value
				//generate password value
				//save user credentials
				
				//get username field
				//create step to enter username into username field
				
				//get password field
				//create step to enter password into password field
				
				//get password confirmation field if it exists
				//create step to enter password into password confirmation field
				
				//get submit button
				//create step to click on submit button
			}
		}
		return steps;
	}

	private ElementState getFormField(List<ElementState> formFields, String form_string) {
		for(ElementState element: formFields) {
			for(String attr_value : element.getAttributes().values()) {
				if(attr_value.toLowerCase().contains(form_string)) {
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * Generate {@link List} of {@link SimpleStep steps}
	 * 
	 * @param last_page
	 * @param elements
	 * @return
	 */
	private List<Step> generateSteps(PageState last_page, List<ElementState> elements) {
		List<Step> steps = new ArrayList<>();
		for(ElementState element: elements) {
			SimpleStep step = new SimpleStep(last_page, 
								 element, 
								 Action.CLICK, 
								 "", 
								 null);
			steps.add(step);
		}
		return steps;
	}

	private void addElementsToExploredMap(PageState last_page, List<ElementState> filtered_elements) {
		if(explored_elements.containsKey(last_page.getKey())) {
			explored_elements.get(last_page.getKey()).addAll(filtered_elements);
		}
		else {
			explored_elements.put(last_page.getKey(), filtered_elements);
		}
	}
	

	private void addElementsToExploredMap(PageState last_page, ElementState element) {
		if(explored_elements.containsKey(last_page.getKey())) {
			explored_elements.get(last_page.getKey()).add(element);
		}
		else {
			List<ElementState> elements = new ArrayList<>();
			elements.add(element);
			explored_elements.put(last_page.getKey(), elements);
		}
	}
	
	/**
	 * Checks if element is a link, a button element or class contains "btn or button"
	 * @param element {@link ElementState}
	 * 
	 * @return true if element is a link or button element or if element class contains "btn or button"
	 */
	private boolean isInteractiveElement(ElementState element) {
		return element.getName().contentEquals("a") 
				|| element.getName().contentEquals("button")
				|| (element.getAttributes().containsKey("class")
					&& (element.getAttribute("class").contains("btn")
						|| element.getAttribute("class").contains("button")))
				|| (element.getAttributes().containsKey("type") 
					&& (element.getAttribute("type").contains("button")
						|| element.getAttribute("type").contains("checkbox")
						|| element.getAttribute("type").contains("email")
						|| element.getAttribute("type").contains("file")
						|| element.getAttribute("type").contains("radio")
						|| element.getAttribute("type").contains("tel")))
				|| element.getAttributes().containsKey("onclick");
	}
	
	private boolean isElementExplored(String page_state_key, ElementState element) {
		return explored_elements.containsKey(page_state_key) 
				&& explored_elements.get(page_state_key).contains(element);
	}
	
	private boolean isFormElement(ElementState element) {
		return element.getXpath().contains("form");
	}
}
