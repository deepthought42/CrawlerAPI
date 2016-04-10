package actors;

import java.util.ArrayList;
import java.util.UUID;

import akka.actor.ActorRef;

import akka.actor.Props;
import akka.actor.UntypedActor;
import structs.Path;

public class PathExpansionActor extends UntypedActor {
		
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			//get last page
			
			//get current page
			Path path = (Path)message;
			System.err.println("EXPANDING PATH WITH LENGTH : "+path.getPath().size());
			ArrayList<Path> pathExpansions = new ArrayList<Path>();
			
			final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
			memory_registry.tell(path, getSelf());
			
			if(path.isUseful()){
				pathExpansions = Path.expandPath(path);
				final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
				for(Path expanded : pathExpansions){
					System.err.println("PASSING EXPANSION TO WORK ALLOCATOR");
					work_allocator.tell(expanded, getSelf() );
				}
			}			
		}
	}
}
