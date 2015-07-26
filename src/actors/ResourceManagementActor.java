package actors;

import java.util.ArrayList;
import java.util.UUID;

public class ResourceManagementActor {
	private int allowedActors = 0;
	private ArrayList<UUID> presentActors = new ArrayList<UUID>();
	
	public ResourceManagementActor(int allowedActors){
		this.allowedActors = allowedActors;
	}
	
	public boolean punchIn(Actor actor){
		return presentActors.add(actor.getActorId());
	}
	
	public UUID punchOut(Actor actor){
		int index = 0;
		for(UUID actorId : presentActors){
			if(actorId.equals(actor.getActorId())){
				break;
			}
			index++;
		}
		return presentActors.remove(index);
	}
	
	/**
	 * Determine if resources are available to new actors
	 * 
	 * @return whether or not there is room for another actor
	 */
	public boolean areResourcesAvailable(){
		if(this.presentActors.size() >= allowedActors){
			return false;
		}
		return true;
	}
}
