package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openqa.selenium.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.looksee.helpers.ActionHelper;
import com.looksee.models.ActionOLD;
import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.ExploratoryPath;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.enums.ElementClassification;
import com.looksee.models.enums.PathStatus;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.ConfirmedJourneyMessage;
import com.looksee.models.message.JourneyMessage;
import com.looksee.models.message.PageDataExtractionMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;
import com.looksee.utils.PathUtils;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 *
 */
@Component
@Scope("prototype")
public class PathExpansionActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(PathExpansionActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private PageStateService page_state_service;
	
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
     */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(JourneyMessage.class, message -> {
				log.warn("Path extraction actor received Journey message");
				//Retrieve last PageState in path
				PageState last_page = PathUtils.getLastPageState(message.getSteps());
				if(last_page == null) {
					last_page = PathUtils.getSecondToLastPageState(message.getSteps());
				}
				//retrieve all ElementStates from journey
				List<ElementState> elements = last_page.getElements();
				final String last_page_key = last_page.getKey();
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
				addElementsToExploredMap(last_page, filtered_elements);
				//Build Steps for all elements in list
				List<Step> new_steps = generateSteps(last_page, filtered_elements);
				
				//generate new journeys with new steps and send to journey executor to be evaluated
				for(Step step: new_steps) {
					List<Step> cloned_steps = new ArrayList<>(message.getSteps());
					cloned_steps.add(step);
					log.warn("Cloned steps size :: "+cloned_steps.size());
					JourneyMessage journey_msg = new JourneyMessage(cloned_steps, PathStatus.EXPANDED, BrowserType.CHROME, message.getDomainId(), message.getAccountId());
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
			.match(ConfirmedJourneyMessage.class, message -> {
				getContext().getParent().tell(message, getSelf());
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
			.matchAny(o -> {
				log.info("received unknown message :: "+o);
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
			Step step = new Step(last_page, element, Action.CLICK, "", null);
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
	 * Checks if url contains internal link format at end of url
	 * 
	 * @param url
	 */
	public static boolean isInternalLink(String url) {
		return url.matches(".*/#[a-zA-Z0-9]+$");
	}
	
	/**
	 * Produces all possible element, action combinations that can be produced from the given path
	 *
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * 
	 * @pre path != null
	 */
	@Deprecated
	public ArrayList<ExploratoryPath> expandPath(JourneyMessage path)  {
		assert path != null;
		
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		//get last page states for page
	
		Collection<ElementState> elements = getElementStatesForExpansion(path.getSteps());
		log.warn("element states to be expanded :: "+elements.size());

		//iterate over all elements
		for(ElementState element : elements){
			if(element.getClassification().equals(ElementClassification.SLIDER) || 
				element.getClassification().equals(ElementClassification.TEMPLATE));
				log.warn("skipping element :: "+element.getXpath());
				continue;
			}
			//Set<PageState> element_page_states = page_state_service.getElementPageStatesWithSameUrl(last_page.getUrl(), page_element.getKey());
			
			//PLACE ACTION PREDICTION HERE INSTEAD OF DOING THE FOLLOWING LOOP
			/*DataDecomposer data_decomp = new DataDecomposer();
			try {
				Brain.predict(DataDecomposer.decompose(page_element), actions);
			} catch (IllegalArgumentException | IllegalAccessException | NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			//END OF PREDICTION CODE


			//skip all elements that are within a form because form paths are already expanded by {@link FormTestDiscoveryActor}
			//page element is not an input or a form
			JourneyMessage new_path = new JourneyMessage(new ArrayList<>(path.getSteps()), 
														   PathStatus.EXPANDED, 
														   path.getBrowser(), 
														   path.getDomainId(), 
														   path.getAccountId());

			//new_path.getPathObjects().add(element);
			//new_path.getKeys().add(element.getKey());

			for(List<ActionOLD> action_list : ActionHelper.getActionLists()){
				for(ActionOLD action : action_list){
					//ArrayList<String> keys = new ArrayList<String>(new_path.);
					ArrayList<Step> path_objects = new ArrayList<Step>(new_path.getSteps());

					//keys.add(action.getKey());
					//path_objects.add(action);

					//ExploratoryPath action_path = new ExploratoryPath(keys, path_objects);

					//pathList.add(action_path);
				}
			}
		
		return null;
	}

	/**
	 * Checks if result has same url as last page in path of {@link Test}. If the urls match,
	 * then a difference between the lists is acquired and only the complementary set is returned.
	 * If the urls don't match then the entire set of {@link Element} for the result page is returned.
	 *
	 * @param test {@link Test} to be expanded
	 *
	 * @return {@link Collection} of element states
	 *
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	private Collection<ElementState> getElementStatesForExpansion(List<Step> steps) {
		assert(steps != null);
		assert(!steps.isEmpty());

		log.warn("getting element states for expansion ....."+steps.size());
		//get last page
		PageState last_page_state = PathUtils.getLastPageState(steps);
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(steps);

		if(last_page_state == null){
			log.warn("last page state is null. returning emtpy hash");
			return new HashSet<>();
		}

		if( second_to_last_page == null){
			log.warn("second to last page state is null. checking elements for expandability :: "+last_page_state.getElements().size());
			Collection<ElementState> expandable_elements =  page_state_service.getExpandableElements(last_page_state.getElements());
			log.warn("returning last page state elements with # of expandable elements :: "+expandable_elements.size());

			return expandable_elements;
		}

		if(last_page_state.getUrl().equals(second_to_last_page.getUrl())){
			Map<String, ElementState> element_xpath_map = new HashMap<>();
			//build hash of element xpaths in last page state
			for(ElementState element : last_page_state.getElements()){
				//continue if element is not displayed, or element is not child
				element_xpath_map.put(element.getXpath(), element);
			}

			for(ElementState element : second_to_last_page.getElements()){
				element_xpath_map.remove(element.getXpath());
			}
			
			log.warn("returning elements :: "+element_xpath_map.values().size());
			return element_xpath_map.values();
		}
		
		log.warn("####################################################################################################");

		//filter list elements from last page elements
		log.warn("elements before filtering :: " + last_page_state.getElements().size());
		List<ElementState> filtered_list = filterListElements(last_page_state.getElements());
		log.warn("returning elements :: "+filtered_list.size());
		return filtered_list;
	}

	@Deprecated
	private List<ElementState> filterListElements(
		List<ElementState> elements
	) {
		List<ElementState> filtered_elements = new ArrayList<>();
		for(ElementState element : elements) {
			if(!element.getClassification().equals(ElementClassification.TEMPLATE) 
				&& !element.getClassification().equals(ElementClassification.SLIDER)
			){
				filtered_elements.add(element);
			}
		}
		return filtered_elements;
	}
	
	private boolean isListElement(ElementState element) {
		return element.getClassification().equals(ElementClassification.TEMPLATE) 
				|| element.getClassification().equals(ElementClassification.SLIDER);
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
