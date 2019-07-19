package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.api.MessageBroadcaster;
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.Test;
import com.qanairy.models.enums.DiscoveryStatus;
import com.qanairy.models.enums.PathStatus;
import com.qanairy.models.message.PageStateMessage;
import com.qanairy.models.message.PathMessage;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.services.BrowserService;
import com.qanairy.services.DiscoveryRecordService;
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
	private ActorSystem actor_system;

	@Autowired
	private DiscoveryRecordRepository discovery_repo;

	@Autowired
	private DiscoveryRecordService discovery_service;

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private BrowserService browser_service;

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
				.match(Message.class, message -> {

			if(message.getData() instanceof Test){
				log.warn("test recieved by path expansion");
				Test test = (Test)message.getData();
				ArrayList<ExploratoryPath> path_expansions = new ArrayList<ExploratoryPath>();
				String discovery_key = message.getOptions().get("discovery_key").toString();
				String browser_name = message.getOptions().get("browser").toString();

				log.warn("looking up discovery record");
				DiscoveryRecord discovery_record = discovery_service.findByKey(discovery_key);

				if(discovery_record.getStatus().equals(DiscoveryStatus.STOPPED)){
					log.info("Discovery is flagged as 'STOPPED' so expansion is being ignored");
					return;
				}

				/*
				 * TODO: uncomment once ready for pricing again.
		    	Account acct = account_service.findByUsername(message.getAccountKey());
		    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
		    		throw new PaymentDueException("Your plan has 0 discovered tests left. Please upgrade to run a discovery");
		    	}
		    	*/

				if(!discovery_record.getExpandedPageStates().contains(test.getResult().getKey())){
					log.warn("Page state has not been expanded yet.");
					//get page states
					List<PageState> page_states = new ArrayList<PageState>();
					for(PathObject path_obj : test.getPathObjects()){
						if(path_obj instanceof PageState){
							PageState page_state = (PageState)path_obj;
							page_state.setElements(page_state_service.getElementStates(page_state.getKey()));
							page_states.add(page_state);
						}
						/*else if(path_obj.getKey().contains("mouseover")){
							log.warn("mouseover action detected in path! Aborting expansion.");
							return;
						}
						*/
					}
					
					log.warn("checking if path has cycle");
					final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
							  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

					if(	(!ExploratoryPath.hasCycle(page_states, test.getResult(), test.getPathObjects().size() == 1)
							&& !test.getSpansMultipleDomains()) || test.getPathKeys().size() == 1){
						log.warn("Cycle not found. Path size to be expanded :: "+test.getPathKeys().size());
						
						if(test.getPathKeys().size() > 1){
							log.warn("test has more than one path key");
							PageState result_page = test.getResult();

							//check if result page has been checked for landability in last 24 hours. If not then check landability of page state
							Duration time_diff = Duration.between(result_page.getLastLandabilityCheck(), LocalDateTime.now());
							Duration minimum_diff = Duration.ofHours(24);

							log.warn("checking if landability needs to be tested");
							if(time_diff.compareTo(minimum_diff) > 0){
								//have page checked for landability
								boolean isLandable = browser_service.checkIfLandable(browser_name, result_page, test.getPathObjects() );
								result_page.setLastLandabilityCheck(LocalDateTime.now());
								result_page.setLandable(isLandable);
								result_page = page_state_service.save(result_page);
							}

							log.warn("is result page landable  ::    "+result_page.isLandable());
							if(result_page.isLandable()){
								try{
									MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
							  	}catch(Exception e){}

								log.warn("sending url to work allocator");
								Message<URL> url_msg = new Message<URL>(message.getAccountKey(), new URL(result_page.getUrl()), message.getOptions());
								work_allocator.tell(url_msg, getSelf() );
							}
							else{
								log.warn("expanding path for test");
								path_expansions = expandPath(test);
								discovery_record = discovery_repo.findByKey(discovery_key);
								discovery_record.addExpandedPageState(test.getResult().getKey());
								discovery_record = discovery_service.save(discovery_record);
								
								log.warn("discovery record :: "+discovery_record);
								for(ExploratoryPath expanded : path_expansions){
									Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());
									work_allocator.tell(expanded_path_msg, getSelf() );
								}
							}
						}
						else{
							path_expansions = expandPath(test);
							discovery_record = discovery_repo.findByKey(discovery_key);
							discovery_record.addExpandedPageState(test.getResult().getKey());
							discovery_record = discovery_service.save(discovery_record);
							log.warn("sending path expansions to work allocator :: "+path_expansions.size());
							for(ExploratoryPath expanded : path_expansions){
								Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());
								work_allocator.tell(expanded_path_msg, getSelf() );
							}
						}

						if(!path_expansions.isEmpty()){
							discovery_record = discovery_repo.findByKey(discovery_key);
							int new_total_path_count = (discovery_record.getTotalPathCount()+path_expansions.size());
							discovery_record.setTotalPathCount(new_total_path_count);
							discovery_record = discovery_service.save(discovery_record);
						}
						log.warn("discovery updated and status being broadcast");
						try{
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
					  	}catch(Exception e){
					  		
						}
					}
				}
				else{
					log.warn("discovery record already has page state expanded  ::    " + test.getResult().getKey());
					log.warn("Discovery record  expanded page states ::   " + discovery_record.getExpandedPageStates());
				}
			}
		})
		.match(PageStateMessage.class, message -> {
			log.warn("page state message encountered : "+message.getPageState());
			List<ExploratoryPath> exploratory_paths = expandPath(message.getPageState());


			DiscoveryRecord discovery_record = discovery_service.increaseTotalPathCount(message.getDiscovery().getKey(), exploratory_paths.size());
			if(discovery_record.getExpandedPageStates().contains(message.getPageState().getKey())){
				return;
			}
			discovery_record.addExpandedPageState(message.getPageState().getKey());
			discovery_record = discovery_service.save(discovery_record);

			log.info("existing total path count :: "+discovery_record.getTotalPathCount());

			try{
				MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
		  	}catch(Exception e){

			}

			final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

			for(ExploratoryPath expanded : exploratory_paths){
				Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());

				work_allocator.tell(expanded_path_msg, getSelf() );
			}

		})
		.match(PathMessage.class, message -> {
			log.warn("expanding path  ::  "+message.getPathObjects().size());
	  		//get last page state
			PageState page_state = null;
			for(int idx=message.getPathObjects().size()-1; idx >= 0; idx--){
				if(message.getPathObjects().get(idx) instanceof PageState){
					page_state = (PageState)message.getPathObjects().get(idx);
					break;
				}
			}

			//get sublist of path from beginning to page state index
			List<ExploratoryPath> exploratory_paths = expandPath(message);

			log.warn("total path expansions found :: "+exploratory_paths.size());
			DiscoveryRecord discovery_record = discovery_service.increaseTotalPathCount(message.getDiscovery().getKey(), exploratory_paths.size());
			if(discovery_record.getExpandedPageStates().contains(page_state.getKey())){
				return;
			}
			discovery_record.addExpandedPageState(page_state.getKey());
			discovery_record = discovery_service.save(discovery_record);

			log.info("existing total path count :: "+discovery_record.getTotalPathCount());

			try{
				MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
		  	}catch(Exception e){

			}

			final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
					  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

			for(ExploratoryPath expanded : exploratory_paths){
				Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());

				work_allocator.tell(expanded_path_msg, getSelf() );
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
	public ArrayList<ExploratoryPath> expandPath(Test test)  {
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		log.warn("expanding path method called....");
		//get last page
		PageState result_page = test.getResult();
		if(result_page == null){
			return null;
		}

		//iterate over all eligible elements
		for(ElementState page_element : getElementStatesForExpansion(test)){
			expanded_elements.put(page_element.getKey(), page_element);
			Set<PageState> element_page_states = page_state_service.getElementPageStatesWithSameUrl(result_page.getUrl(), page_element.getKey());
			boolean higher_order_page_state_found = false;
			//check if there is a page state with a lower x or y scroll offset
			for(PageState page : element_page_states){
				if(result_page.getScrollXOffset() > page.getScrollXOffset()
						|| result_page.getScrollYOffset() > page.getScrollYOffset()){
					higher_order_page_state_found = true;
				}
			}

			if(higher_order_page_state_found){
				continue;
			}

			//check if test should be considered landing page test or not
			boolean is_landing_page_test = (test.getPathObjects().get(0) instanceof Redirect && test.getPathKeys().size() == 2)
													|| test.getPathKeys().size() == 1;

			log.warn("checking if element exists in a previous page state");
			if(!is_landing_page_test && doesElementExistInMultiplePageStatesWithinTest(test, page_element, result_page.getUrl())){
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
				log.warn("expanding path!!!!!!!!!!!!!!!!!");
				//page element is not an input or a form
				Test new_test = Test.clone(test);

				if(!is_landing_page_test){
					new_test.getPathKeys().add(test.getResult().getKey());
					new_test.getPathObjects().add(test.getResult());
				}

				new_test.getPathObjects().add(page_element);
				new_test.getPathKeys().add(page_element.getKey());
			
				//List<Rule> rules = extractor.extractInputRules(page_element);	
				//page_element.getRules().addAll(rules);
			
				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));

				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					for(Action action : action_list){
						ArrayList<String> keys = new ArrayList<String>(new_test.getPathKeys());
						ArrayList<PathObject> path_objects = new ArrayList<PathObject>(new_test.getPathObjects());

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
				log.warn("action list  ::   "+pathList.size());
			}
		}
		return pathList;
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
		PageState last_page = null;
		int last_page_idx = -1;
		for(int idx = path.getPathObjects().size()-1; idx >=0; idx--){
			if(path.getPathObjects().get(idx) instanceof PageState){
				last_page = (PageState)path.getPathObjects().get(idx);
				last_page_idx = idx;
				break;
			}
		}
		
		if(last_page == null){
			log.warn("expansion --  last page is null");
			return null;
		}
		//iterate over all elements
		for(ElementState page_element : last_page.getElements()){
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

			//check if test should be considered landing page test or not
			boolean is_landing_page_test = path.getKeys().size()-1 == last_page_idx;

			log.warn("checking if element exists in a previous page state");
			if(!is_landing_page_test && doesElementExistInMultiplePageStatesWithinPath(path, page_element, last_page.getUrl())){
				log.warn("landing page test ??  "+is_landing_page_test);
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
				/*
				for(PathObject obj : path.getPathObjects()){
					if(obj instanceof Action && ((Action)obj).getName().equals("mouseover")){
						return new ArrayList<>();
					}
				}
				*/
				//List<Rule> rules = extractor.extractInputRules(page_element);	
				//page_element.getRules().addAll(rules);
			
				log.warn("expanding path!!!!!!!!!!!!!!!!!");
				//page element is not an input or a form
				PathMessage new_path = new PathMessage(new ArrayList<>(path.getKeys()), new ArrayList<>(path.getPathObjects()), path.getDiscoveryActor(), PathStatus.EXPANDED, path.getBrowser());

				if(!is_landing_page_test){
					new_path.getKeys().add(last_page.getKey());
					new_path.getPathObjects().add(last_page);
				}

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
	 * Produces all possible element, action combinations that can be produced from the given path
	 *
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 *
	 * @pre page_state != null
	 */
	public ArrayList<ExploratoryPath> expandPath(PageState page_state)  {
		assert page_state != null;
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();

		List<ElementState> elements = page_state.getElements();
		//get List of page states for page

		List<String> path_keys = new ArrayList<>();
		path_keys.add(page_state.getKey());
		List<PathObject> path_objects = new ArrayList<>();
		path_objects.add(page_state);

		//iterate over all elements
		for(ElementState page_element : elements){
			expanded_elements.put(page_element.getKey(), page_element);

			Set<PageState> element_page_states = page_state_service.getElementPageStatesWithSameUrl(page_state.getUrl(), page_element.getKey());
			boolean higher_order_page_state_found = false;
			//check if there is a page state with a lower x or y scroll offset
			for(PageState page : element_page_states){
				if(page_state.getScrollXOffset() > page.getScrollXOffset()
						|| page_state.getScrollYOffset() > page.getScrollYOffset()){
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
			//else if(page_element.getName().equals("input")){
				
				/*
				for(Rule rule : page_element.getRules()){
					List<List<PathObject>> tests = GeneralFormTestDiscoveryActor.generateInputRuleTests(page_element, rule);
					//paths.addAll(generateMouseRulePaths(page_element, rule)
					for(List<PathObject> path_obj_list: tests){
						//iterate over all actions
						for(PathObject path_obj : path_obj_list){
							path_keys.add(path_obj.getKey());
							path_objects.add(path_obj);
						}

						for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
							for(Action action : action_list){
								ExploratoryPath action_path = new ExploratoryPath(new ArrayList<String>(path_keys), new ArrayList<PathObject>(path_objects));
								action_path.getPathKeys().add(action.getKey());
								action_path.getPathObjects().add(action);
								//check for element action sequence.
								//if one exists with one of the actions in the action_list
								// 	 then skip this action path
d
								pathList.add(action_path);
							}
						}
					}
				}
*/
			//}
			else{
				List<String> keys = new ArrayList<>(path_keys);
				List<PathObject> path = new ArrayList<>(path_objects);

				path.add(page_element);
				keys.add(page_element.getKey());

				//List<Rule> rules = extractor.extractInputRules(page_element);	
				//page_element.getRules().addAll(rules);
			
				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					for(Action action : action_list){
						ExploratoryPath action_path = new ExploratoryPath(new ArrayList<String>(keys), new ArrayList<PathObject>(path));
						action_path.getPathKeys().add(action.getKey());
						action_path.getPathObjects().add(action);

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
	private Collection<ElementState> getElementStatesForExpansion(Test test) {
		assert(test != null);
		
		Set<ElementState> elements = new HashSet<>();
		//get last page
		PageState result_page = test.getResult();
		if(result_page == null){
			return elements;
		}
		
		PageState last_page_state = PathUtils.getLastPageState(test.getPathObjects());
		
		if(test.getResult().getUrl().equals(last_page_state.getUrl())){
			Map<String, ElementState> element_xpath_map = new HashMap<>();
			//build hash of element xpaths in last page state
			for(ElementState element : last_page_state.getElements()){
				element_xpath_map.put(element.getXpath(), element);
			}
			
			for(ElementState result : test.getResult().getElements()){
				element_xpath_map.remove(result.getKey());
			}
			return element_xpath_map.values();
		}
		
		return test.getResult().getElements();
	}
}
