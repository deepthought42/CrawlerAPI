package com.minion.actors;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.qanairy.rules.Rule;
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.models.Action;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 *
 */
public class PathExpansionActor extends UntypedActor {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

	/**
     * {@inheritDoc}
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;

			if(acct_msg.getData() instanceof Path){
				//send data directly to form test builder
				final ActorRef form_test_discoverer = this.getContext().actorOf(Props.create(FormTestDiscoveryActor.class), "FormTestDiscoveryActor"+UUID.randomUUID());
				form_test_discoverer.tell(acct_msg, getSelf() );
				
				Path path = (Path)acct_msg.getData();
				
				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();
				if((path.isUseful() && !path.getSpansMultipleDomains()) || path.size() == 1){
					Page last_page = path.findLastPage();
					Page first_page = (Page)path.getPath().get(0);
					
					if(!first_page.getUrl().equals(last_page.getUrl()) && last_page.isLandable()){
						System.err.println("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.getPath().add(last_page);
					}
					
					pathExpansions = PathExpansionActor.expandPath(path);
					System.err.println("Path expansions found : " +pathExpansions.size());
					
					for(ExploratoryPath expanded : pathExpansions){
						final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
						Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(acct_msg.getAccountKey(), expanded, acct_msg.getOptions());
						
						work_allocator.tell(expanded_path_msg, getSelf() );
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
	public static ArrayList<ExploratoryPath> expandPath(Path path)  {
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		
		//get last page
		Page page = path.findLastPage();
		if(page == null){
			return null;
		}

		List<PageElement> page_elements = page.getElements();
		
		//iterate over all elements
		int path_count = 0;
		for(PageElement page_element : page_elements){
			
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
			if(page_element.getName().equals("input")){
				page_element.addRules(ElementRuleExtractor.extractInputRules(page_element));
				for(Rule rule : page_element.getRules()){
					List<Path> paths = FormTestDiscoveryActor.generateInputRuleTests(page_element, rule);
					//paths.addAll(generateMouseRulePaths(page_element, rule)
					for(Path form_path: paths){
						System.err.println("constructing new path");
						//iterate over all actions
						Path new_path = Path.clone(path);
						new_path.getPath().addAll(form_path.getPath());
						for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
							ExploratoryPath action_path = new ExploratoryPath(new_path.getPath(), action_list);
							//check for element action sequence. 
							//if one exists with one of the actions in the action_list
							// 	 then skip this action path
							
							
							/*if(ExploratoryPath.hasExistingElementActionSequence(action_path)){
								System.err.println("EXISTING ELEMENT ACTION SEQUENCE FOUND");
								continue;
							}*/
							pathList.add(action_path);
							path_count++;
						}
					}
				}
			}
			else{
				Path new_path = Path.clone(path);
				new_path.add(page_element);
				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));

				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					
					ExploratoryPath action_path = new ExploratoryPath(new_path.getPath(), action_list);
					
					//check for element action sequence. 
					//if one exists with one of the actions in the action_list
					// 	 then skip this action path
					/****  NOTE: THE FOLLOWING 3 LINES NEED TO BE FIXED TO WORK CORRECTLY ******/
					//if(ExploratoryPath.hasExistingElementActionSequence(action_path)){
					//	continue;
					//}
					pathList.add(action_path);
					path_count++;
				}
			}
		}
				
		return pathList;
	}
}
