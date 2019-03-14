package com.minion.actors;

import static com.qanairy.config.SpringExtension.SpringExtProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.minion.api.MessageBroadcaster;
import com.minion.api.exception.PaymentDueException;
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.models.Account;
import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.rules.Rule;
import com.qanairy.services.SubscriptionService;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 *
 */
@Component
@Scope("prototype")
public class PathExpansionActor extends AbstractActor {
	private static Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private DiscoveryRecordRepository discovery_repo;
	
	@Autowired
	private AccountRepository account_repo;
	
	@Autowired
	private SubscriptionService subscription_service;
	
	@Autowired
	private ElementRuleExtractor extractor;
	
	/**
     * {@inheritDoc}
     */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.class, message -> {
			
			if(message.getData() instanceof Test){				
				Test test = (Test)message.getData();
				System.err.println("expanding path");
				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();
				DiscoveryRecord discovery_record = discovery_repo.findByKey(message.getOptions().get("discovery_key").toString());

		    	Account acct = account_repo.findByUsername(message.getAccountKey());
		    	if(subscription_service.hasExceededSubscriptionDiscoveredLimit(acct, subscription_service.getSubscriptionPlanName(acct))){
		    		throw new PaymentDueException("Your plan has 0 discovered tests left. Please upgrade to run a discovery");
		    	}
		    	
				if(	(!ExploratoryPath.hasCycle(test.getPathObjects(), test.getResult()) 
						&& !test.getSpansMultipleDomains()) || test.getPathKeys().size() == 1){	
					
					
					// if path is a single page 
					//		then send path to urlBrowserActor
					if(test.getPathKeys().size() > 1 && test.getResult().isLandable()){
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
						discovery_record = discovery_repo.save(discovery_record);
						
						try{
							MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
					  	}catch(Exception e){
						
						}
						
						log.info("SENDING URL TO WORK ALLOCATOR :: "+test.getResult().getUrl());
						final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

						Message<URL> url_msg = new Message<URL>(message.getAccountKey(), new URL(test.getResult().getUrl()), message.getOptions());
						work_allocator.tell(url_msg, getSelf() );

						return;
					}
					else{
						pathExpansions = expandPath(test);

						for(ExploratoryPath expanded : pathExpansions){
							final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());
	
							Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());
							
							work_allocator.tell(expanded_path_msg, getSelf() );
						}
					}
					
					int new_total_path_count = (discovery_record.getTotalPathCount()+pathExpansions.size());
					System.err.println("existing total path count :: "+discovery_record.getTotalPathCount());
					System.err.println("expected total path count :: "+new_total_path_count);
					discovery_record.setTotalPathCount(new_total_path_count);
					discovery_record = discovery_repo.save(discovery_record);

					log.info("existing total path count :: "+discovery_record.getTotalPathCount());
					
					try{
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
				  	}catch(Exception e){
					
					}
				}	
			}
			postStop();
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
			log.info("received unknown message :: "+o.getClass().getName());
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
		
		//get last page
		PageState result_page = test.getResult();
		if(result_page == null){
			return null;
		}

		//iterate over all elements
		log.info("Page elements for expansion :: "+result_page.getElements().size());
		for(PageElement page_element : result_page.getElements()){
			
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
			else if(page_element.getName().equals("input")){
				List<Rule> rules = extractor.extractInputRules(page_element);
				for(Rule rule : rules){
					page_element.addRule(rule);
				}
				for(Rule rule : page_element.getRules()){
					List<List<PathObject>> tests = GeneralFormTestDiscoveryActor.generateInputRuleTests(page_element, rule);
					//paths.addAll(generateMouseRulePaths(page_element, rule)
					for(List<PathObject> path_obj_list: tests){
						//iterate over all actions
						List<PathObject> path_objects = new ArrayList<PathObject>(test.getPathObjects());
						
						List<String> path_keys = new ArrayList<String>(test.getPathKeys());
						for(PathObject path_obj : path_obj_list){
							path_keys.add(path_obj.getKey());
							path_objects.add(path_obj);
						}
						for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
							ExploratoryPath action_path = new ExploratoryPath(path_keys, path_objects, action_list);
							//check for element action sequence. 
							//if one exists with one of the actions in the action_list
							// 	 then skip this action path
							
							
							/*if(ExploratoryPath.hasExistingElementActionSequence(action_path)){
								log.info("EXISTING ELEMENT ACTION SEQUENCE FOUND");
								continue;
							}*/
							pathList.add(action_path);
						}
					}
				}
			}
			else{
				System.err.println("Checking if element exists previously as a path object or within a page state");
				//check if element exists in previous pageStates
				
				if(doesElementExistInMultiplePageStatesWithinTest(test, page_element)){
					continue;
				}
				
				//page element is not an input or a form
				Test new_test = Test.clone(test);

				if(test.getPathKeys().size() > 1){
					new_test.getPathKeys().add(test.getResult().getKey());
					new_test.getPathObjects().add(test.getResult());
				}
				new_test.getPathObjects().add(page_element);
				new_test.getPathKeys().add(page_element.getKey());
				
				System.err.println("adding actions lists to exploratory paths");
				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));
				
				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					System.err.println("exploratory path being created...");
					ExploratoryPath action_path = new ExploratoryPath(new_test.getPathKeys(), new_test.getPathObjects(), action_list);
					
					//check for element action sequence. 
					//if one exists with one of the actions in the action_list
					// 	 then load the existing path and process it for path expansion action path
					/****  NOTE: THE FOLLOWING 3 LINES NEED TO BE FIXED TO WORK CORRECTLY ******/
					/*if(ExploratoryPath.hasExistingElementActionSequence(action_path)){
						continue;
					}*/
					System.err.println("added action path to path list");
					pathList.add(action_path);
				}
			}
		}
		System.err.println("path list size :: " + pathList.size());
		return pathList;
	}

	/**
	 * Checks if a given {@link PageElement} exists in a {@link PageState} within the {@link Test} path
	 *  such that the {@link PageState} preceeds the page state that immediately precedes the element in the test path
	 * 
	 * @param test {@link Test}
	 * @param elem {@link PageElement}
	 * 
	 * @return
	 * 
	 * @pre test != null
	 * @pre elem != null
	 */
	public boolean doesElementExistInMultiplePageStatesWithinTest(Test test, PageElement elem) {
		assert test != null;
		assert elem != null;
		
		if(test.getPathKeys().size() == 1){
			return false;
		}
		for(int path_idx = test.getPathObjects().size()-2; path_idx >= 0; path_idx-- ){
			PathObject obj = test.getPathObjects().get(path_idx);
			if(obj.getType().equals("PageState")){
				PageState page_state = ((PageState) obj);
				log.debug("page state has # of elements  ::  "+page_state.getElements().size());
				for(PageElement page_elem : page_state.getElements()){
					if(elem.equals(page_elem)){
						return true;
					}
				}
			}	
		}

		return false;
	}
}
