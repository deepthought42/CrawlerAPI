import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import util.Timing;

public class Actor implements Runnable {

	private String url = null;
	private HashMap<WebElement, String> elementActionMap = new HashMap<WebElement, String>();
	private WebElement element = null;
	private String action = null;
	private WebDriver driver = null;
	
	public Actor(String url) {
		this.url = url;
	}

	public Actor(String url, WebElement element, String action) {
		this.url = url;
		this.element = element;
		this.action = action;
	}
	
	public void signIn(String username, String pass){
		driver.findElement(By.xpath("//a[@id='signInLink']")).click();
	    new WebDriverWait(driver, 5).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='userFormEmailField']")));
		WebElement user = driver.findElement(By.xpath("//input[@id='userFormEmailField']"));
		user.sendKeys(username);
		WebElement password = driver.findElement(By.xpath("//input[@id='userFormPasswordField']"));
		password.sendKeys(pass);
		driver.findElement(By.xpath("//button[@id='userFormSignInButton']")).click();
	}

	/**
	 * This method will open a firefox browser and load the url that was given at instantiation.
	 *  The actor will load the page into memory, access the element it needs, and then perform an action on it.
	 */
	public void run() {
		//get a web browser driver and open the browser to the desired url
		//get the page
		this.driver = DiffHandler.openWithFirefox(url);
		signIn("test@test.com", "testtest");

		String pageSrc = driver.getPageSource();
		Page page = new Page(driver, pageSrc, DateFormat.getDateInstance(), false);
		System.out.println("Built page instance.");
		
		ConcurrentNode<Page> currentPageNode = new ConcurrentNode<Page>(page);
		List<PageElement> visibleElements = currentPageNode.data.getVisibleElements(driver);
		System.out.println("Wrapped page instance in a graph node");
		
		for(PageElement elem : visibleElements){
			ConcurrentNode<PageElement> element = new ConcurrentNode<PageElement>(elem);
			currentPageNode.addOutput(element);
		}
		
		System.out.println("----------------------------------------------------");
		System.err.println("loaded up elements. there were " + currentPageNode.getOutputs().size());
		System.out.println("----------------------------------------------------");
		
		int element_idx = 0;
		int action_idx = 0;
		String[] actions = ActionFactory.getActions();
		System.out.println("Starting iteration over elements");
		while(element_idx < visibleElements.size()){
			
			try{
				System.out.println("EXECUTING ACTION :"+ actions[action_idx]+ " Now");
				ActionFactory.execAction(driver, visibleElements.get(element_idx), actions[action_idx]);
				
				//execute the following if it there is no problem executing action
				List<PageElement> newVisibleElements = page.getVisibleElements(driver);
				//DID THE NUMBER OF VISIBLE ELEMENTS CHANGE?
				System.out.println("NEW VISIBLE ELEMENT NUMBER :: " + newVisibleElements.size());
				// then add page to map and set action as an input to the page
				if(visibleElements.size() != newVisibleElements.size()){
					System.err.println("Sizes not equal");
				}
				if(!visibleElements.get(element_idx).cssMatches(newVisibleElements.get(element_idx))){
					System.out.println("CSS ATTRIBUTES DO NOT MATCH");
				}

				ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
				
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				System.err.println("ACTOR EXECUTED ACTION :: " +actions[action_idx]);
			}
			catch(UnreachableBrowserException e){
				System.err.println("Browser is unreachable, pausing for 5 seconds");
				Timing.pauseThread(5000);
			}
			//catch()
			if(action_idx >= actions.length-1){
				action_idx = 0;
				element_idx++;
			}
			else{
				action_idx++;
			}
		}
	}
}
