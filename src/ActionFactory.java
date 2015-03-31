import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;


public class ActionFactory {
	private static String[] actions = {"click",
								"doubleClick",
								"sendKeys"};
	private static Actions builder;
	
	public static void execAction(WebDriver driver, PageElement pageElem, String action){
		builder = new Actions(driver);
		WebElement elem = pageElem.getElement();
		if(action.equals("click")){
			builder.click(elem);
		}
		else if(action.equals("clickAndHold")){
			builder.clickAndHold(elem);
		}
		else if(action.equals("contextClick")){
			//builder.contextClick(elem);
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
		builder.perform();
	}
	
	/**
	 * The list of actions possible
	 * @return
	 */
	public static String[] getActions(){
		return actions;
	}
}
