package com.minion.actors;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.qanairy.helpers.ActionHelper;
import com.qanairy.models.Action;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.enums.ElementClassification;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.PathMessage;
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
				log.warn("STARTING PATH EXPANSION....  "+message.getPathObjects().size());
				//get sublist of path from beginning to page state index
				List<ExploratoryPath> exploratory_paths = expandPath(message);
				log.warn("total path expansions found :: "+exploratory_paths.size());

				for(ExploratoryPath expanded : exploratory_paths){
					PathMessage path = new PathMessage(expanded.getPathKeys(), expanded.getPathObjects(), message.getDiscoveryActor(), PathStatus.EXPANDED, message.getBrowser(), message.getDomainActor(), message.getDomain(), message.getAccountId());
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
	 * 
	 * @pre path != null
	 */
	public ArrayList<ExploratoryPath> expandPath(PathMessage path)  {
		assert path != null;
		
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		//get last page states for page
	
		Collection<ElementState> elements = getElementStatesForExpansion(path.getPathObjects());
		log.warn("element states to be expanded :: "+elements.size());

		//iterate over all elements
		for(ElementState element : elements){
			if(element.getClassification().equals(ElementClassification.SLIDER.getShortName()) || 
				element.getClassification().equals(ElementClassification.TEMPLATE.getShortName()) || 
				element.isPartOfForm()){
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
			PathMessage new_path = new PathMessage(new ArrayList<>(path.getKeys()), new ArrayList<>(path.getPathObjects()), path.getDiscoveryActor(), PathStatus.EXPANDED, path.getBrowser(), path.getDomainActor(), path.getDomain(), path.getAccountId());

			new_path.getPathObjects().add(element);
			new_path.getKeys().add(element.getKey());

			for(List<Action> action_list : ActionHelper.getActionLists()){
				for(Action action : action_list){
					ArrayList<String> keys = new ArrayList<String>(new_path.getKeys());
					ArrayList<PathObject> path_objects = new ArrayList<PathObject>(new_path.getPathObjects());

					keys.add(action.getKey());
					path_objects.add(action);

					ExploratoryPath action_path = new ExploratoryPath(keys, path_objects);

					pathList.add(action_path);
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

		log.warn("getting element states for expansion ....."+path_objects.size());
		//get last page
		PageState last_page_state = PathUtils.getLastPageState(path_objects);
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(path_objects);

		if(last_page_state == null){
			log.warn("last page state is null. returning emtpy hash");
			return new HashSet<>();
		}

		if( second_to_last_page == null){
			log.warn("second to last page state is null. returning last page state elements with size :: "+last_page_state.getElements().size());
			return last_page_state.getElements();
		}

		if(last_page_state.getUrl().equals(second_to_last_page.getUrl())){
			Map<String, ElementState> element_xpath_map = new HashMap<>();
			//build hash of element xpaths in last page state
			for(ElementState element : last_page_state.getElements()){
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

	private List<ElementState> filterListElements(
		List<ElementState> elements
	) {
		List<ElementState> filtered_elements = new ArrayList<>();
		for(ElementState element : elements) {
			if(!element.getClassification().equals(ElementClassification.TEMPLATE.getShortName()) 
				&& !element.getClassification().equals(ElementClassification.SLIDER.getShortName())
			){
				filtered_elements.add(element);
			}
		}
		return filtered_elements;
	}
}
