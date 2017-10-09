package com.minion.actors;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import akka.actor.ActorRef;

import akka.actor.Props;
import akka.actor.UntypedActor;

import com.qanairy.models.Test;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.ActionOrderOfOperations;
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
	private static Logger log = LogManager.getLogger(PathExpansionActor.class);

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

		String[] actions = ActionFactory.getActions();

		List<PageElement> page_elements = page.getElements();
		System.out.println("Expected number of exploratory paths : " + (page_elements.size()*actions.length) + " : # Elems : "+page.getElements().size()+ " ; # actions :: "+ActionFactory.getActions());
		
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
			//END OF PRECTION CODE
			
			//iterate over all actions
			Path new_path = Path.clone(path);
			new_path.add(page_element);
			
			for(List<Action> action_list : ActionOrderOfOperations.getActionLists()){
				ExploratoryPath action_path = new ExploratoryPath(new_path.getPath(), action_list);
				//Action action_obj = new Action(action);
				pathList.add(action_path);
				path_count++;
			}			
		}
		
		System.out.println("# of Paths added : "+path_count);
		
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
			
			if(acct_msg.getData() instanceof Path){
				Path path = (Path)acct_msg.getData();
				
				System.out.println("EXPANDING PATH WITH LENGTH : "+path.size());
				ArrayList<ExploratoryPath> pathExpansions = new ArrayList<ExploratoryPath>();

				//Message<Path> path_msg = new Message<Path>(acct_msg.getAccountKey(), path);

				//final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
				//memory_registry.tell(path_msg, getSelf());
				
				System.out.println("PATH SPANS MULTIPLE DOMAINS? :: " +path.getSpansMultipleDomains() );
				System.out.println("PATH is useful? :: " +path.isUseful());
				if((path.isUseful() && !path.getSpansMultipleDomains()) || path.size() == 1){
					Page last_page = path.findLastPage();
					Page first_page = (Page)path.getPath().get(0);
					if(first_page == null){
						System.out.println("first page is null");
					}
					if(last_page == null){
						System.out.println("last page is null");
					}
					if(!first_page.getUrl().equals(last_page.getUrl()) && last_page.isLandable()){
						System.out.println("Last page is landable...truncating path to start with last_page");
						path = new Path();
						path.getPath().add(last_page);
					}
					
					pathExpansions = PathExpansionActor.expandPath(path);
					System.out.println("Path expansions found : " +pathExpansions.size());
					
					final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
					for(ExploratoryPath expanded : pathExpansions){
						Message<ExploratoryPath> expanded_path_msg = new Message<ExploratoryPath>(acct_msg.getAccountKey(), expanded);
						
						work_allocator.tell(expanded_path_msg, getSelf() );
					}
				}	
			}
		}
	}
}
