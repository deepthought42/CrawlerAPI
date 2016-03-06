package actors;

import java.util.UUID;

public interface Actor {
	UUID uuid = null;
	
	/**
	 * Get the Id of the actor
	 * @return the actor ID
	 */
	public UUID getActorId();
}
