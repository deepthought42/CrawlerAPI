import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class NodeTester {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Action action = new Action(org.openqa.selenium.interactions.Actions.class);
		ConcurrentNode<Action> actionNode = new ConcurrentNode<Action>(action);
		Method[] methods = actionNode.data.getKnownActions(); 
		for(int i =0; i < methods.length; i+=2){
			System.err.println("METHOD NAME :: " + methods[i].getName());
			actionNode.data.executeAction(i);
			try {
				try {
					methods[i].invoke(action.className.newInstance());
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
