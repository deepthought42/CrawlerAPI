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
					PathMessage path = new PathMessage(expanded.getPathKeys(), expanded.getPathObjects(), message.getDiscoveryActor(), PathStatus.EXPANDED, message.getBrowser());
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
			//check if page element is an input
			else{
				
				//List<Rule> rules = extractor.extractInputRules(page_element);	
				//page_element.getRules().addAll(rules);
			
				log.warn("expanding path!!!!!!!!!!!!!!!!!");
				//page element is not an input or a form
				PathMessage new_path = new PathMessage(new ArrayList<>(path.getKeys()), new ArrayList<>(path.getPathObjects()), path.getDiscoveryActor(), PathStatus.EXPANDED, path.getBrowser());

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
						log.warn("adding action path:: " );
						pathList.add(action_path);
					}
				}
				log.warn("action list  ::   "+pathList.size());
			}
		}
		return pathList;
	}

	/**
	 * Checks if a given {@link ElementState} exists in a {@link PageState} within the {@link Test} path
	 *  such that the {@link PageState} preceeds the page state that immediately precedes the element in the test path
	 *
	 * @param test {@link Test}
	 * @param elem {@link ElementState}
	 *
	 * @return
	 *
	 * @pre test != null
	 * @pre elem != null
	 */
	public boolean doesElementExistInMultiplePageStatesWithinPath(PathMessage path, ElementState elem, String page_url) {
		assert path != null;
		assert elem != null;

		if(path.getKeys().size() == 1){
			return false;
		}
		
		log.warn("checking if element exists in multiple page states via path");
		Map<String, Integer> elem_cnt = new HashMap<>();
		for(int path_idx = path.getPathObjects().size()-1; path_idx >= 0; path_idx-- ){
			PathObject obj = path.getPathObjects().get(path_idx);
			if(obj instanceof PageState){
				PageState page_state = ((PageState) obj);
				for(ElementState page_elem : page_state.getElements()){
					if(!elem_cnt.containsKey(page_elem.getXpath())){
						elem_cnt.put(page_elem.getXpath(), 1);
					}
					else{
						elem_cnt.put(page_elem.getXpath(), elem_cnt.get(page_elem.getXpath())+1);
					}
					//log.warn("checking element xpath :: " + page_elem.getXpath() + "   :    "+elem_cnt.get(page_elem.getXpath()));
					
				}
			}
		}

		log.warn("element count :: " + elem_cnt.get(elem.getXpath()));
		return elem_cnt.get(elem.getXpath()) > 1;
	}
	
	/**
	 * Checks if a given {@link ElementState} exists in a {@link PageState} within the {@link Test} path
	 *  such that the {@link PageState} preceeds the page state that immediately precedes the element in the test path
	 *
	 * @param test {@link Test}
	 * @param elem {@link ElementState}
	 *
	 * @return
	 *
	 * @pre test != null
	 * @pre elem != null
	 */
	public boolean doesElementExistInMultiplePageStatesWithinTest(Test test, ElementState elem, String page_url) {
		assert test != null;
		assert elem != null;

		if(test.getPathKeys().size() == 1){
			return false;
		}
		
		log.warn("checking if element exists in multiple page states via test");
		for(int path_idx = test.getPathObjects().size()-1; path_idx >= 0; path_idx-- ){
			PathObject obj = test.getPathObjects().get(path_idx);
			if(obj instanceof PageState){
				PageState page_state = ((PageState) obj);
				log.debug("page state has # of elements  ::  "+page_state.getElements().size());
				for(ElementState page_elem : page_state.getElements()){
					if(elem.getXpath().equals(page_elem.getXpath()) && page_url.equals(page_state.getUrl())){
						return true;
					}
					else if(elem.equals(page_elem)){
						return true;
					}
				}
			}
		}

		return false;
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
	 * @pre test != null
	 */
	private Collection<ElementState> getElementStatesForExpansion(List<PathObject> path_objects) {
		assert(path_objects != null);
		assert(!path_objects.isEmpty());
		
		Set<ElementState> elements = new HashSet<>();
		//get last page
		PageState last_page_state = PathUtils.getLastPageState(path_objects);
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(path_objects);
		if(last_page_state == null){
			return elements;
		}
		
		
		if(last_page_state.getUrl().equals(second_to_last_page.getUrl())){
			Map<String, ElementState> element_xpath_map = new HashMap<>();
			//build hash of element xpaths in last page state
			for(ElementState element : second_to_last_page.getElements()){
				element_xpath_map.put(element.getXpath(), element);
			}
			
			for(ElementState element : last_page_state.getElements()){
				element_xpath_map.remove(element.getKey());
			}
			return element_xpath_map.values();
		}
		
		return last_page_state.getElements();
	}
}
