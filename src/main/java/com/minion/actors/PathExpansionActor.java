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
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.repository.DiscoveryRecordRepository;
import com.qanairy.models.rules.Rule;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 *
 */
@Component
@Scope("prototype")
public class PathExpansionActor extends AbstractActor {
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
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

				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();
				DiscoveryRecord discovery_record = discovery_repo.findByKey(message.getOptions().get("discovery_key").toString());

				if(test.firstPage().getUrl().contains((new URL(test.getResult().getUrl()).getHost())) && 
						(!ExploratoryPath.hasCycle(test.getPathKeys(), test.getResult()) 
						&& !test.getSpansMultipleDomains()) || test.getPathKeys().size() == 1){	
					
					//Send test to simplifier
					//when simplifier returns simplified test
					// if path is a single page 
					//		then send path to urlBrowserActor
					
					//	expand path
					// 	send expanded path to work allocator
					
					if(test.getPathKeys().size() > 1 && test.getResult().isLandable()){
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
						discovery_record = discovery_repo.save(discovery_record);

						final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
								  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

						Message<URL> url_msg = new Message<URL>(message.getAccountKey(), new URL(test.getResult().getUrl()), message.getOptions());
						work_allocator.tell(url_msg, getSelf() );
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
						return;
					}
					else if(!discovery_record.getExpandedPageState().contains(test.getResult().getKey())){					
						pathExpansions = expandPath(test);
						System.err.println(pathExpansions.size()+"   path expansions found.");
						
						DiscoveryRecord discovery_record2 = discovery_repo.findByKey(message.getOptions().get("discovery_key").toString());
						if(discovery_record2 != null){
							discovery_record = discovery_record2;
						}
						int new_total_path_count = (discovery_record.getTotalPathCount()+pathExpansions.size());
						discovery_record.setTotalPathCount(new_total_path_count);
						discovery_record.getExpandedPageState().add(test.getResult().getKey());
						discovery_record = discovery_repo.save(discovery_record);
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
	
						for(ExploratoryPath expanded : pathExpansions){
							final ActorRef work_allocator = actor_system.actorOf(SpringExtProvider.get(actor_system)
									  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());
	
							Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(message.getAccountKey(), expanded, message.getOptions());
							
							work_allocator.tell(expanded_path_msg, getSelf() );
						}
					}
				}	
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
			log.info("received unknown message");
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
		PageState page = test.getResult();
		if(page == null){
			return null;
		}

		//iterate over all elements
		System.err.println("Page elements for expansion :: "+page.getElements().size());
		for(PageElement page_element : page.getElements()){
			
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
								System.err.println("EXISTING ELEMENT ACTION SEQUENCE FOUND");
								continue;
							}*/
							pathList.add(action_path);
						}
					}
				}
			}
			else{
				Test new_test = Test.clone(test);
				if(test.getPathKeys().size() > 1){
					new_test.getPathKeys().add(test.getResult().getKey());
					new_test.getPathObjects().add(test.getResult());
				}
				new_test.getPathObjects().add(page_element);
				new_test.getPathKeys().add(page_element.getKey());

				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));

				
				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					ExploratoryPath action_path = new ExploratoryPath(new_test.getPathKeys(), new_test.getPathObjects(), action_list);
					
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
				
		return pathList;
	}
}
