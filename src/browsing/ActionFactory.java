package browsing;
import java.util.HashMap;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class ActionFactory {
	private static String[] actions = {"click",
								"doubleClick",
								"mouseover"};/*,
								"sendKeys"};*/
	private static HashMap<String, Integer> actionWeights = new HashMap<String, Integer>();
	private static Actions builder;

	static {
		actionWeights.put("click", new Integer(2));
		actionWeights.put("doubleClick", new Integer(3));
		actionWeights.put("mouseover", new Integer(1));
		actionWeights.put("sendKeys", new Integer(4));
	}
	
	public ActionFactory(WebDriver driver){
		builder = new Actions(driver);
		loadWeights();
	}
	
	public static void loadWeights(){
		
	}
	/**
	 * 
	 * @param driver
	 * @param elem
	 * @param action
	 */
	public void execAction(WebElement elem, String action) throws WebDriverException{
		if(action.equals("click")){
			builder.click(elem);
		}
		else if(action.equals("clickAndHold")){
			builder.clickAndHold(elem);
		}
		else if(action.equals("contextClick")){
			builder.contextClick(elem);
		}
		else if(action.equals("doubleClick")){
			builder.doubleClick(elem);
		}
		else if(action.equals("dragAndDrop")){
			//builder.dragAndDrop(source, target);
		}
		else if(action.equals("keyDown")){
			//builder.keyDown();
		}
		else if(action.equals("keyUp")){
			//builder.keyUp(theKey);
		}
		else if(action.equals("release")){
			builder.release(elem);
		}
		else if(action.equals("sendKeys")){
			
			builder.sendKeys(elem, Keys.chord(Keys.CONTROL, "a", Keys.DELETE));
			builder.sendKeys(elem, "Some src Val");
		}
		else if(action.equals("mouseover")){
			builder.moveToElement(elem);
		}
		builder.perform();
	}
	
	/**
	 * The list of actions possible
	 * @return
	 */
	public static String[] getActions(){
		return actions;
	}

	/**
	 * returns the cost of the action based on static cost value for a given key
	 * s
	 * @param action
	 * @return
	 */
	public static Integer getCost(String action) {
		return actionWeights.get(action);
	}
}
