package com.minion.actors;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;

import akka.actor.Props;
import akka.actor.UntypedActor;

import com.minion.api.PastPathExperienceController;
import com.minion.api.models.Test;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.actions.Action;
import com.minion.structs.Message;
import com.minion.structs.Path;
import com.minion.structs.SessionTestTracker;
import com.minion.structs.TestMapper;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;

/**
 * Actor that handles {@link Path}s and {@link Test}s to expand said paths.
 * 
 * @author Brandon Kindred
 *
 */
public class PathExpansionActor extends UntypedActor {
    private static final Logger log = LoggerFactory.getLogger(PathExpansionActor.class);

    /**
	 * Produces all possible element, action combinations that can be produced from the given path
	 * 
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static ArrayList<Path> expandPath(Path path)  {
		log.info( " EXPANDING PATH...");
		ArrayList<Path> pathList = new ArrayList<Path>();
		
		//get last page
		Page page = path.findLastPage();
		if(page == null){
			return null;
		}

		String[] actions = ActionFactory.getActions();

		List<PageElement> page_elements = page.getElements();//  .getVisibleElements(webdriver, "");
		log.info("Expected number of paths : " + (page_elements.size()*actions.length));
		
		//iterate over all elements
		int path_count = 0;
		for(PageElement page_element : page_elements){
			//iterate over all actions
			for(String action : actions){
				Path action_path = Path.clone(path);
				Action action_obj = new Action(action);
				
				log.info("Constructing path object " + path_count + " for expand path");
				action_path.add(page_element);

				action_path.add(action_obj);
				log.info("Setting clone path key to :: " + action_path.generateKey());
				action_path.setKey(action_path.generateKey());
				pathList.add(action_path);
				path_count++;
			}			
		}
		
		log.info("# of Paths added : "+path_count);
		
		return pathList;
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			//send data directly to form test builder 
			final ActorRef form_test_discoverer = this.getContext().actorOf(Props.create(FormTestDiscoveryActor.class), "FormTestDiscoveryActor"+UUID.randomUUID());
			form_test_discoverer.tell(acct_msg, getSelf() );
			
			if(acct_msg.getData() instanceof Test){
				
				Test test = (Test)acct_msg.getData();
				Path path = test.getPath();
				//Message<Test> test_msg = new Message<Test>(acct_msg.getAccountKey(), test);
				
				log.info("EXPANDING TEST PATH WITH LENGTH : "+path.size());
				ArrayList<Path> pathExpansions = new ArrayList<Path>();

				//final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
				//memory_registry.tell(test_msg, getSelf());
				
				if(path != null && path.getIsUseful() && !path.getSpansMultipleDomains()){
					if(!test.getPath().findLastPage().getUrl().equals(test.getResult().getUrl()) && test.getResult().isLandable()){
						log.info("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.getPath().add(test.getResult());
					}
					
					//EXPAND PATH IN TEST
					pathExpansions = PathExpansionActor.expandPath(path);
					log.info("Test Path expansions found : " +pathExpansions.size());
					
					final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					for(Path expanded : pathExpansions){
						Test new_test = new Test(expanded, null,  path.findLastPage().getUrl().getHost());
						// CHECK THAT TEST HAS NOT YET BEEN EXPERIENCED RECENTLY
						SessionTestTracker seqTracker = SessionTestTracker.getInstance();
						TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
						if(!testMap.containsTest(new_test)){
							Message<Test> expanded_test_msg = new Message<Test>(acct_msg.getAccountKey(), new_test);

							work_allocator.tell(expanded_test_msg, getSelf() );
							testMap.addTest(new_test);
						}
						else{
							log.info("TEST WITH KEY : "+new_test.hashCode()+" : HAS ALREADY BEEN EXAMINED!!!! No future examination will happen during this sessions");
							PastPathExperienceController.broadcastTestExperience(testMap.getTestHash().get(test.hashCode()));
						}
					}
				}
			}
			else if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				
				log.info("EXPANDING PATH WITH LENGTH : "+path.size());
				ArrayList<Path> pathExpansions = new ArrayList<Path>();

				Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);

				final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
				memory_registry.tell(path_msg, getSelf());
				
				log.info("PATH SPANS MULTIPLE DOMAINS? :: " +path.getSpansMultipleDomains());
				if(path.getIsUseful() && !path.getSpansMultipleDomains()){
					Page last_page = path.findLastPage();
					Page first_page = (Page)path.getPath().get(0);
					if(first_page == null){
						log.info("first page is null");
					}
					if(last_page == null){
						log.info("last page is null");
					}
					if(!first_page.getUrl().equals(last_page.getUrl()) && last_page.isLandable()){
						log.info("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.getPath().add(last_page);
					}
					// CHECK THAT PAGE ELEMENT ACTION SEQUENCE HAS NOT YET BEEN EXPERIENCED
					Test test = new Test(path, last_page, last_page.getUrl().getHost());
					SessionTestTracker seqTracker = SessionTestTracker.getInstance();
					TestMapper testMap = seqTracker.getSequencesForSession("SESSION_KEY_HERE");
					if(!testMap.containsTest(test)){
						testMap.addTest(test);
					}
					else{
						log.info("TEST WITH KEY : "+test.hashCode()+" : HAS ALREADY BEEN EXAMINED!!!! No future examination will happen during this sessions");
					}

					pathExpansions = PathExpansionActor.expandPath(path);
					log.info("Path expansions found : " +pathExpansions.size());
					
					final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					for(Path expanded : pathExpansions){
						log.info("Sending expanded path : "+expanded.generateKey());
						Message<Path> expanded_path_msg = new Message<Path>(acct_msg.getAccountKey(), expanded);
						
						work_allocator.tell(expanded_path_msg, getSelf() );
					}
				}	
			}
		}
	}
}
