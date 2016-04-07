package actors;

import java.util.UUID;

import akka.actor.UntypedActor;
import structs.Path;

/**
 * Handles retrieving tests
 * 
 * @author brandon kindred
 *
 */
public class TestCoordinatorActor extends UntypedActor {
	public final UUID uuid;
	public final Path path;
	public final boolean isExpectingPass;
	
	public TestCoordinatorActor(Path path, boolean isExpectingPass){
		uuid = UUID.randomUUID();
		this.path = path;
		this.isExpectingPass = isExpectingPass;
	}
	
	
	public UUID getActorId() {
		return this.uuid;
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
				
	}

}
