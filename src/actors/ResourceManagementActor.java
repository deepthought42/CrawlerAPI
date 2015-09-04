package actors;

import java.util.ArrayList;
import java.util.UUID;

public class ResourceManagementActor {
	private int allowedActors = 0;
	private ArrayList<UUID> presentActors = new ArrayList<UUID>();
	
	/**
	 * Create ResourceManagementActor with a given number of allowed actor
	 *   threads to allow
	 * @param allowedActors number of threads allowed
	 */
	public ResourceManagementActor(int allowedActors){
		this.allowedActors = allowedActors;
	}
	
	/**
	 * Accounts for presence of actor thread running
	 * @param actor
	 * @return
	 */
	public boolean punchIn(Actor actor){
		System.out.print(Thread.currentThread().getName() + "Punching In...");
		return presentActors.add(actor.getActorId());
	}
	
	/**
	 * Accounts for presence of actor thread shutting down
	 * @param actor
	 * @return
	 */
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
	public synchronized boolean areResourcesAvailable(){
		if(this.presentActors.size() >= allowedActors){
			return false;
		}
		return true;
	}
}
