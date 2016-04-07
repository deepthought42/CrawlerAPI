package actors;

import java.util.ArrayList;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.UntypedActor;
import browsing.Page;
import browsing.PathObject;
import shortTerm.ShortTermMemoryRegistry;
import structs.Path;

public class PathExpansionActor extends UntypedActor {
		
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			//get last page
			final ActorRef memory_registry = this.getContext().actorOf(Props.create(ShortTermMemoryRegistry.class), "memoryRegistry");

			//get current page
			Path path = (Path)message;
			Page last_page = path.getLastPageVertex();
			Page current_page = path.getLastPageVertex();
			ArrayList<Path> pathExpansions = new ArrayList<Path>();
			if(last_page.equals(current_page) && path.getPath().size() > 1){
				//it is not valuable because the state did not change
				//register with memory as not valuable. 
				//!!! It should not be expanded !!!
				path.setIsUseful(false);
			}
			else if(path.getPath().size() == 1){
				//It is valuable because there is only 1 page in the path
				//register with memory as valuable.
				memory_registry.tell(path, getSelf());

				//Expand path
				path.setIsUseful(true);
				pathExpansions = Path.expandPath(path);
			}
			else if(!last_page.equals(current_page)){
				//State has changed
				//register with memory
				memory_registry.tell(path, getSelf());

				//expand path
				path.setIsUseful(true);
				pathExpansions = Path.expandPath(path);
			}
			
			final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator");
			for(Path expanded : pathExpansions){
				work_allocator.tell(expanded, getSelf() );
			}
		}
	}
}
