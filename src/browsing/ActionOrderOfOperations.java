package browsing;

import java.util.HashMap;

public class ActionOrderOfOperations {
	public static HashMap<String, Integer> actionOrderOfOperationsMap 
		= new HashMap<String, Integer>();
	
	public static Integer getOrderOfOperationForAction(String actionName){
		return actionOrderOfOperationsMap.get(actionName);
	}
}
