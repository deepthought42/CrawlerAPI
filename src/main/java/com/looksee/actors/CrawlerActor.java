package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.PathUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class CrawlerActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(AestheticAuditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
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
					log.warn("Total elements after filtering ... "+filtered_elements.size());
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(msg.getPageState(), filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(msg.getPageState(), filtered_elements);
					
					log.warn("new steps found ... "+new_steps.size());
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> steps = new ArrayList<>();
						steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		msg.getDomainId(), 
																		msg.getAccountId());
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
						log.warn("sending journey message to Journey Executor actor");
					}
				})
				.match(ConfirmedJourneyMessage.class, message -> {
					PageState final_page = PathUtils.getLastPageState(message.getSteps());
					Domain domain = domain_service.findById(message.getDomainId()).get();
					if( BrowserUtils.isExternalLink(domain.getUrl(), final_page.getUrl()) 
							|| visited_pages.contains(final_page)/*|| pageState is in visited list*/) {
						audit_manager.tell(message, getSelf());
					}
					
					//Add page state to visited list
					visited_pages.add(final_page);
					
					/*
					JourneyMessage journey_message = new JourneyMessage(message.getSteps(), 
																		PathStatus.EXAMINED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId());
					ActorRef path_expansion_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
							  .props("pathExpansionActor"), "pathExpansionActor"+UUID.randomUUID());
					path_expansion_actor.tell(message, getSelf());
					getContext().getSender().tell(PoisonPill.class, getSelf());
					
				})
				.match(JourneyMessage.class, message -> {
					log.warn("Path extraction actor received Journey message");
					//Retrieve last PageState in path
					PageState last_page = PathUtils.getLastPageState(message.getSteps());
					if(last_page == null) {
						last_page = PathUtils.getSecondToLastPageState(message.getSteps());
					}
					 */
					//retrieve all ElementStates from journey
					List<ElementState> elements = final_page.getElements();
					final String last_page_key = final_page.getKey();
					//Filter out non interactive elements
					//Filter out elements that are in explored map for PageState with key
					//Filter out form elements
					log.warn("total page elements before filtering ... "+elements.size());
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
					
					log.warn("Total journey elements after filtering ... "+filtered_elements.size());

					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(final_page, filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(final_page, filtered_elements);
					
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> cloned_steps = new ArrayList<>(message.getSteps());
						cloned_steps.add(step);
						log.warn("Cloned steps size :: "+cloned_steps.size());
						JourneyMessage journey_msg = new JourneyMessage(cloned_steps, 
																		PathStatus.EXPANDED, 
																		BrowserType.CHROME, 
																		message.getDomainId(), 
																		message.getAccountId());
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
					log.warn("Total elements after filtering ... "+filtered_elements.size());
					//send form elements to form journey actor
					//Add interactive elements to explored map for PageState key
					addElementsToExploredMap(message.getPageState(), filtered_elements);
					//Build Steps for all elements in list
					List<Step> new_steps = generateSteps(message.getPageState(), filtered_elements);
					
					log.warn("new steps found ... "+new_steps.size());
					//generate new journeys with new steps and send to journey executor to be evaluated
					for(Step step: new_steps) {
						List<Step> steps = new ArrayList<>();
						steps.add(step);
						JourneyMessage journey_msg = new JourneyMessage(steps, PathStatus.EXPANDED, BrowserType.CHROME, message.getDomainId(), message.getAccountId());
						ActorRef journey_executor = getContext().actorOf(SpringExtProvider.get(actor_system)
																.props("journeyExecutor"), "journeyExecutor"+UUID.randomUUID());
						journey_executor.tell(journey_msg, getSelf());
						log.warn("sending journey message to Journey Executor actor");
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
	
	/**
	 * Generate {@link List} of {@link Step steps}
	 * 
	 * @param last_page
	 * @param elements
	 * @return
	 */
	private List<Step> generateSteps(PageState last_page, List<ElementState> elements) {
		List<Step> steps = new ArrayList<>();
		for(ElementState element: elements) {
			Step step = new Step(last_page, 
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
