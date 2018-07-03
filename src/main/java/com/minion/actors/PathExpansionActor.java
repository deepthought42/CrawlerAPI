package com.minion.actors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.config.SpringExtension;
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
public class PathExpansionActor extends UntypedActor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	DiscoveryRecordRepository discovery_repo;
	
	/**
     * {@inheritDoc}
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			
			if(acct_msg.getData() instanceof Test){				
				Test test = (Test)acct_msg.getData();

				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();
				DiscoveryRecord discovery_record = discovery_repo.findByKey(acct_msg.getOptions().get("discovery_key").toString());

				if((!ExploratoryPath.hasCycle(test.getPathObjects(), test.getResult()) 
						&& !test.getSpansMultipleDomains()) || test.getPathKeys().size() == 1){
					PageState last_page = test.findLastPage();
					
					if(!last_page.equals(test.getResult()) && test.getResult().isLandable()){
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
						discovery_record = discovery_repo.save(discovery_record);

						final ActorRef work_allocator = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
								  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());

						Message<URL> url_msg = new Message<URL>(acct_msg.getAccountKey(), new URL(test.getResult().getUrl()), acct_msg.getOptions());
						work_allocator.tell(url_msg, getSelf() );
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
						return;
					}
					else{
						pathExpansions = PathExpansionActor.expandPath(test);
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+pathExpansions.size());
						discovery_record = discovery_repo.save(discovery_record);
						MessageBroadcaster.broadcastDiscoveryStatus(discovery_record);
	
						for(ExploratoryPath expanded : pathExpansions){
							final ActorRef work_allocator = actor_system.actorOf(SpringExtension.SPRING_EXTENSION_PROVIDER.get(actor_system)
									  .props("workAllocationActor"), "work_allocation_actor"+UUID.randomUUID());
	
							Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(acct_msg.getAccountKey(), expanded, acct_msg.getOptions());
							
							work_allocator.tell(expanded_path_msg, getSelf() );
						}
					}
				}	
			}
		}
	}
	
    /**
	 * Produces all possible element, action combinations that can be produced from the given path
	 * 
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static ArrayList<ExploratoryPath> expandPath(Test test)  {
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
				List<Rule> rules = ElementRuleExtractor.extractInputRules(page_element);
				for(Rule rule : rules){
					page_element.addRule(rule);
				}
				for(Rule rule : page_element.getRules()){
					List<List<PathObject>> tests = FormTestDiscoveryActor.generateInputRuleTests(page_element, rule);
					//paths.addAll(generateMouseRulePaths(page_element, rule)
					for(List<PathObject> path_obj_list: tests){
						//iterate over all actions
						List<PathObject> path_objects = new ArrayList<PathObject>(test.getPathObjects());
						path_objects.addAll(path_obj_list);
						
						List<String> path_keys = new ArrayList<String>(test.getPathKeys());
						for(PathObject path_obj : path_obj_list){
							path_keys.add(path_obj.getKey());
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
					new_test.addPathKey(test.getResult().getKey());
					new_test.addPathObject(test.getResult());
				}
				new_test.addPathObject(page_element);
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
