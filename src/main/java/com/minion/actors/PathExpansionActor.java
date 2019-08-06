package com.minion.actors;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.browsing.ActionOrderOfOperations;
import com.qanairy.models.Action;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.PathMessage;
import com.qanairy.services.PageStateService;
import com.qanairy.utils.PathUtils;

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
	private PageStateService page_state_service;

	private Map<String, ElementState> expanded_elements;

	public PathExpansionActor() {
		this.expanded_elements = new HashMap<String, ElementState>();
	}

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
			.match(PathMessage.class, message -> {
				log.warn("expanding path  ::  "+message.getPathObjects().size());
		
				//get sublist of path from beginning to page state index
				List<ExploratoryPath> exploratory_paths = expandPath(message);
				log.warn("total path expansions found :: "+exploratory_paths.size());
		
				for(ExploratoryPath expanded : exploratory_paths){
					PathMessage path = new PathMessage(expanded.getPathKeys(), expanded.getPathObjects(), message.getDiscoveryActor(), PathStatus.EXPANDED, message.getBrowser(), message.getDomainActor());
					message.getDiscoveryActor().tell(path, getSelf());
				}
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
	 * Produces all possible element, action combinations that can be produced from the given path
	 *
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public ArrayList<ExploratoryPath> expandPath(PathMessage path)  {
		log.warn("path size for expansion :: " + path.getPathObjects().size());
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		log.warn("expanding path method called....");
		//get last page states for page
		PageState last_page = PathUtils.getLastPageState(path.getPathObjects());
		
		
		if(last_page == null){
			log.warn("expansion --  last page is null");
			return null;
		}
		//iterate over all elements
		for(ElementState page_element : getElementStatesForExpansion(path.getPathObjects())){
			expanded_elements.put(page_element.getKey(), page_element);
			Set<PageState> element_page_states = page_state_service.getElementPageStatesWithSameUrl(last_page.getUrl(), page_element.getKey());
			boolean higher_order_page_state_found = false;
			log.warn("Element page states count :: "+element_page_states.size());
			//check if there is a page state with a lower x or y scroll offset
			for(PageState page : element_page_states){
				if(last_page.getScrollXOffset() > page.getScrollXOffset()
						|| last_page.getScrollYOffset() > page.getScrollYOffset()){
					higher_order_page_state_found = true;
				}
			}

			if(higher_order_page_state_found){
				continue;
			}

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
			if(page_element.getXpath().contains("form")){
				continue;
			}
			else{
				//List<Rule> rules = extractor.extractInputRules(page_element);	
				//page_element.getRules().addAll(rules);
			
				log.warn("expanding path!!!!!!!!!!!!!!!!!");
				//page element is not an input or a form
				PathMessage new_path = new PathMessage(new ArrayList<>(path.getKeys()), new ArrayList<>(path.getPathObjects()), path.getDiscoveryActor(), PathStatus.EXPANDED, path.getBrowser(), path.getDomainActor());

				new_path.getPathObjects().add(page_element);
				new_path.getKeys().add(page_element.getKey());

				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));

				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					for(Action action : action_list){
						ArrayList<String> keys = new ArrayList<String>(new_path.getKeys());
						ArrayList<PathObject> path_objects = new ArrayList<PathObject>(new_path.getPathObjects());

						keys.add(action.getKey());
						path_objects.add(action);

						ExploratoryPath action_path = new ExploratoryPath(keys, path_objects);
						//check for element action sequence.
						//if one exists with one of the actions in the action_list
						// 	 then load the existing path and process it for path expansion action path
						/****  NOTE: THE FOLLOWING 3 LINES NEED TO BE FIXED TO WORK CORRECTLY ******/
						/*if(ExploratoryPath.hasExistingElementActionSequence(action_path)){
							continue;
						}*/
						pathList.add(action_path);
					}
				}
			}
		}
		return pathList;
	}
	
	/**
	 * Checks if result has same url as last page in path of {@link Test}. If the urls match, 
	 * then a difference between the lists is acquired and only the complementary set is returned. 
	 * If the urls don't match then the entire set of {@link ElementState} for the result page is returned.
	 * 
	 * @param test {@link Test} to be expanded
	 * 
	 * @return {@link Collection} of element states
	 * 
	 * @pre path_objects != null
	 * @pre !path_objects.isEmpty()
	 */
	private Collection<ElementState> getElementStatesForExpansion(List<PathObject> path_objects) {
		assert(path_objects != null);
		assert(!path_objects.isEmpty());
		
		//get last page
		PageState last_page_state = PathUtils.getLastPageState(path_objects);
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(path_objects);
		
		log.warn("####################################################################################################");
		log.warn("####################################################################################################");

		if(last_page_state == null){
			log.warn("LAST PAGE STATE IS NULL DURING EXPANSION!!!!!!!!!!!!!!");
			return new HashSet<>();
		}
		
		if( second_to_last_page == null){
			log.warn("second to last page state is null. Returning all elements for last page state");
			return last_page_state.getElements();
		}
		
		log.warn("last page url      ::  " + last_page_state.getUrl());
		log.warn("second to last url ::  " + second_to_last_page.getUrl());
		log.warn("Do urls match????    :: " + last_page_state.getUrl().equals(second_to_last_page.getUrl()));
		if(last_page_state.getUrl().equals(second_to_last_page.getUrl())){
			Map<String, ElementState> element_xpath_map = new HashMap<>();
			//build hash of element xpaths in last page state
			for(ElementState element : last_page_state.getElements()){
				element_xpath_map.put(element.getXpath(), element);
			}
			
			log.warn("element xpath map size :: " + element_xpath_map.size());
			log.warn("# elements for last page :: " + last_page_state.getElements().size());
			log.warn("# elements for second to last page :: " + second_to_last_page.getElements().size());
			for(ElementState element : second_to_last_page.getElements()){
				element_xpath_map.remove(element.getXpath());
			}
			
			log.warn("# of elements left in map after filtering  ::   " + element_xpath_map.size());
			for(String xpath : element_xpath_map.keySet()){
				log.warn("xpath :: "+xpath);
			}
			log.warn("####################################################################################################");
			log.warn("####################################################################################################");

			return element_xpath_map.values();
		}
		log.warn("####################################################################################################");
		log.warn("####################################################################################################");

		return last_page_state.getElements();
	}
}
