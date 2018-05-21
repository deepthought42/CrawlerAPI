package com.minion.actors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.persistence.Action;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.Rule;
import com.qanairy.persistence.Test;
import com.minion.api.MessageBroadcaster;
import com.minion.browsing.ActionOrderOfOperations;
import com.minion.browsing.form.ElementRuleExtractor;
import com.minion.structs.Message;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.TestPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;

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

			if(acct_msg.getData() instanceof Test){				
				Test test = (Test)acct_msg.getData();
				
				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();
				if((!ExploratoryPath.hasCycle(test.getPathObjects(), test.getResult()) 
						&& !test.getSpansMultipleDomains()) || test.getPathKeys().size() == 1){
					PageState last_page = test.findLastPage();
					PageState first_page = test.firstPage();
					
					if(!last_page.equals(first_page) && last_page.isLandable()){
						OrientConnectionFactory conn = new OrientConnectionFactory();
						DiscoveryRecordDao discovery_dao = new DiscoveryRecordDaoImpl();
						DiscoveryRecord discovery_record = discovery_dao.find(acct_msg.getOptions().get("discovery_key").toString());
						discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+1);
						discovery_dao.save(discovery_record);
						conn.close();

						final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
						Message<URL> url_msg = new Message<URL>(acct_msg.getAccountKey(), last_page.getUrl(), acct_msg.getOptions());
						work_allocator.tell(url_msg, getSelf() );
						
						MessageBroadcaster.broadcastDiscoveryStatus(first_page.getUrl().getHost(), discovery_record);
						return;
					}
					
					pathExpansions = PathExpansionActor.expandPath(test);
					
			  		OrientConnectionFactory conn = new OrientConnectionFactory();
					DiscoveryRecordDao discovery_dao = new DiscoveryRecordDaoImpl();
					DiscoveryRecord discovery_record = discovery_dao.find(acct_msg.getOptions().get("discovery_key").toString());
					discovery_record.setTotalPathCount(discovery_record.getTotalPathCount()+pathExpansions.size());
					discovery_dao.save(discovery_record);
					MessageBroadcaster.broadcastDiscoveryStatus(first_page.getUrl().getHost(), discovery_record);
					conn.close();

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
	public static ArrayList<ExploratoryPath> expandPath(Test test)  {
		ArrayList<ExploratoryPath> pathList = new ArrayList<ExploratoryPath>();
		
		//get last page
		PageState page = test.findLastPage();
		if(page == null){
			return null;
		}

		List<PageElement> page_elements = page.getElements();
		
		//iterate over all elements
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
				List<Rule> rules = ElementRuleExtractor.extractInputRules(page_element);
				for(Rule rule : rules){
					page_element.addRule(rule);
				}
				for(Rule rule : page_element.getRules()){
					List<Test> tests = FormTestDiscoveryActor.generateInputRuleTests(page_element, rule);
					//paths.addAll(generateMouseRulePaths(page_element, rule)
					for(Test form_test: tests){
						//iterate over all actions
						Test new_test = TestPOJO.clone(test);
						new_test.getPathObjects().addAll(form_test.getPathObjects());
						for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
							ExploratoryPath action_path = new ExploratoryPath(new_test.getPathObjects(), action_list);
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
				Test new_test = TestPOJO.clone(test);
				new_test.addPathObject(page_element);
				//page_element.addRules(ElementRuleExtractor.extractMouseRules(page_element));

				for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
					
					ExploratoryPath action_path = new ExploratoryPath(new_test.getPathObjects(), action_list);
					
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
