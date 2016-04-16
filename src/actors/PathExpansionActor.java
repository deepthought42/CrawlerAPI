package actors;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.log4j.Logger;

import akka.actor.ActorRef;

import akka.actor.Props;
import akka.actor.UntypedActor;
import structs.Path;

public class PathExpansionActor extends UntypedActor {
    private static final Logger log = Logger.getLogger(PathExpansionActor.class);

    /**
     * {@inheritDoc}
     */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Path){
			//get last page
			
			//get current page
			Path path = (Path)message;
			log.info("EXPANDING PATH WITH LENGTH : "+path.getPath().size());
			ArrayList<Path> pathExpansions = new ArrayList<Path>();
			
			final ActorRef memory_registry = this.getContext().actorOf(Props.create(MemoryRegistryActor.class), "memoryRegistry"+UUID.randomUUID());
			memory_registry.tell(path, getSelf());
			
			if(path.isUseful()){
				pathExpansions = Path.expandPath(path);
				final ActorRef work_allocator = this.getContext().actorOf(Props.create(WorkAllocationActor.class), "workAllocator"+UUID.randomUUID());
				for(Path expanded : pathExpansions){
					work_allocator.tell(expanded, getSelf() );
				}
			}			
		}
	}
}
