package browsing;
import java.lang.reflect.Method;



public class Action{
	public Class<?> className;
	public Method[] knownActions;
	/**
	 * instantiate a new class
	 * @param className
	 */
	public Action(Class<?> className){
		this.className = className;
		knownActions = className.getMethods();
	}

	public Method[] getKnownActions(){
		return knownActions;
	}
	
	public void executeAction(int method_idx){
		this.knownActions[method_idx].getParameterTypes();
		System.err.println("GETTING PARAMATER TYPES");
		//check if there are known required values.
		//If there are no known required values then generate values for each
		//
	}
	//CLICK, DOUBLE_CLICK, SEND_KEYS;
}
