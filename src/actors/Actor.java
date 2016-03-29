package actors;

import java.util.UUID;

public interface Actor {
	public UUID uuid = null;
	
	/**
	 * Get the Id of the actor
	 * 
	 * @return the universal unique ID for this actor
	 */
	public UUID getActorId();
}
