package actors;

import java.util.ArrayList;
import java.util.UUID;

import memory.Vocabulary;

public interface Actor {
	UUID uuid = null;
	
	/**
	 * Get the Id of the actor
	 * @return the actor ID
	 */
	public UUID getActorId();
}
