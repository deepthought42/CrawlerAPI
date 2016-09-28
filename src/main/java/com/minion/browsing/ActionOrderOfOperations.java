package com.minion.browsing;

import java.util.HashMap;

/**
 * Contains the Order of operations for actions. A lower value indicates that
 *   an action has less precedence.
 *   
 * @author Brandon Kindred
 *
 */
public class ActionOrderOfOperations {
	public static HashMap<String, Integer> actionOrderOfOperationsMap 
		= new HashMap<String, Integer>();
	
	public static Integer getOrderOfOperationForAction(String actionName){
		return actionOrderOfOperationsMap.get(actionName);
	}
}
