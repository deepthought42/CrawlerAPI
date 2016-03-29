package actors;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.UntypedActor;
import structs.Path;

public class PathExpansionActor extends UntypedActor {
	
	public final ActorSystem actor_system;
	
	public PathExpansionActor(ActorSystem system){
		this.actor_system = system;
	}
	@Override
	public void onReceive(Object message) throws Exception {
		
	}
}
