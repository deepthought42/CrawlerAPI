package com.minion.browsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Constructs {@linkplain Actions} provided by Selenium
 *
 */
public class ActionFactory {
	private static Logger log = LoggerFactory.getLogger(ActionFactory.class);

	private static String[] actions = {"click",
								"doubleClick",
								"mouseover",
								"scroll",
								"sendKeys"};
	private static Actions builder;
	
	public ActionFactory(WebDriver driver){
		builder = new Actions(driver);
	}

	/**
	 * 
	 * @param driver
	 * @param elem
	 * @param action
	 */
	public void execAction(WebElement elem, String input, String action) throws WebDriverException{
		if(action.equals("click")){
			builder.click(elem);

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
		}
		else if(action.equals("clickAndHold")){
			//builder.clickAndHold(elem);
		}
		//Context click clicks select/options box
		else if(action.equals("contextClick")){
			builder.contextClick(elem);
		}
		else if(action.equals("doubleClick")){
			builder.doubleClick(elem);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
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
			//builder.sendKeys(elem, Keys.chord(Keys.CONTROL, Keys.ALT, Keys.DELETE));
			builder.sendKeys(elem, input);
		}
		else if(action.equals("mouseover")){
			builder.moveToElement(elem);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
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
