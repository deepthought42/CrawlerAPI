package actors;

import browsing.Page;

/**
 * Action Pruning Actor is meant to compliment the browser actor in web mapping graphs.
 * The Action Pruning Actor iterates over the graph and find all actions for each {@link Page page} node
 * and prunes off actions that exist through multiple paths by finding an element that is the parent of all
 * other elements with the same action leading to the same page and prunes all other actions that are not the parent
 * 
 * @author Brandon Kindred
 *
 */
public class ActionPruningActor implements Runnable{

	public void run() {
		// TODO Auto-generated method stub
		
	}

}
