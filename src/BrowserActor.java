import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import util.Timing;

/**
 * An this threadable class is implemented to handle the interaction with a browser 
 * @author Brandon Kindred
 *
 */
public class BrowserActor implements Runnable {

	private String url = null;
	private HashMap<WebElement, String> elementActionMap = new HashMap<WebElement, String>();

	private String action = null;
	private WebElement element = null;

	private WebDriver driver = null;
	
	public BrowserActor(String url) {
		this.url = url;
	}

	public BrowserActor(String url, WebElement element, String action) {
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
		Browser browser = new Browser(url);
		this.driver = browser.getDriver();
		
		this.driver = Browser.openWithFirefox(url);
		signIn("test@test.com", "testtest");

		//String pageSrc = driver.getPageSource();
		//Page page = new Page(driver, pageSrc, DateFormat.getDateInstance(), false);
		System.out.println("Built page instance.");
		
		ConcurrentNode<Page> currentPageNode = new ConcurrentNode<Page>(browser.getPage());
		List<PageElement> visibleElements = currentPageNode.data.getElements();
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
			Timing.pauseThread(2000);
			try{
				System.out.println("EXECUTING ACTION :"+ actions[action_idx]+ " Now");
				ActionFactory.execAction(driver, visibleElements.get(element_idx), actions[action_idx]);
				
				//execute the following if it there is no problem executing action
				Page newPage = new Page(driver, driver.getPageSource(), DateFormat.getDateInstance(), false);

				List<PageElement> newVisibleElements = newPage.getElements();
				//DID THE NUMBER OF VISIBLE ELEMENTS CHANGE?
				System.out.println("NEW VISIBLE ELEMENT NUMBER :: " + newVisibleElements.size());
				// then add page to map and set action as an input to the page
				if(!browser.getPage().equals(newPage)){
					System.out.println("PAGE HAS CHANGED. GROWING GRAPH...");
					//add action node to current element node
					//add current element node as input to the action node
					ConcurrentNode<PageElement> elementNode = new ConcurrentNode<PageElement>(visibleElements.get(element_idx));
					currentPageNode.addOutput(elementNode);
					elementNode.addInput(currentPageNode);
					
					ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
					elementNode.addOutput(actionNode);
					actionNode.addInput(elementNode);
					
					driver.navigate().refresh();
					browser.updatePage(DateFormat.getDateInstance(), false);					
				}	
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				System.err.println("ACTOR EXECUTED ACTION :: " +actions[action_idx]);
			}
			catch(UnreachableBrowserException e){
				System.err.println("Browser is unreachable, pausing for 10 seconds");
				this.driver = browser.getDriver();
				Timing.pauseThread(10000);
			}
			catch(WebDriverException e){
				System.err.println("problem accessing webDriver instance");
				driver.close();
				browser = new Browser(url);
				//this.driver = Browser.openWithFirefox(url);		
				visibleElements = currentPageNode.data.getVisibleElements(driver);

			}
			
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
