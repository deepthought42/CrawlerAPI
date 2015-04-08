package browsing;
import java.text.DateFormat;
import java.util.concurrent.ConcurrentHashMap;

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
	private ConcurrentNode<Page> pageNode = null;

	private WebDriver driver = null;
	
	public BrowserActor(String url) {
		this.url = url;
	}

	public BrowserActor(String url, ConcurrentNode<Page> pageNode) {
		this.url = url;
		this.pageNode = pageNode;
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
		signIn("test@test.com", "testtest");

		System.out.println("Built page instance.");
		//TODO :: LOAD PAGE NODE FROM MEMORY FOR GIVEN URL.
		// IF IT EXISTS CHECK IF IT HAS BEEN MAPPED. IF IT HAS NOT THEN MAP IT
		// ELSE CRAWL MAP TO FIND NEW PAGES TO MAP
		this.pageNode = new ConcurrentNode<Page>(browser.getPage());
		System.out.println("Wrapped page instance in a graph node");
		
		System.out.println("----------------------------------------------------");
		System.err.println("loaded up elements. there were " + this.pageNode.data.getElements().size());
		System.out.println("----------------------------------------------------");
		if(this.pageNode.getOutputs() != null & this.pageNode.getOutputs().isEmpty()){
			pageCrawler(browser, this.pageNode);
			System.out.println("FINISHED EXECUTING ALL ACTIONS FOR THIS PAGE");
		}
		System.out.println("CRAWLING PAGE COMPLETE...");
		System.out.println("Number of elements in page output :: " + pageNode.getOutputs().size());
		
		//else{
			System.out.println("Page node already has outputs. lets crawl them!");
			//browser = new Browser(url);
			mapCrawler(browser, url, this.pageNode);
		//}


		System.exit(1);
	}
	
	/**
	 * Retrieves all elements on a page, and performs all known actions on each element.
	 * 	Generates a map consisting of the page nodes outputs being Elements and elements 
	 *  outputs being actions.
	 *  
	 *  TODO :: Remove sign in from crawler. It should be abstracted away.
	 *  
	 * @param browser A Browser instance
	 * @param pageNode The network node of type Page that is to be crawled
	 */
	private void pageCrawler(Browser browser, ConcurrentNode<Page> pageNode){
		int element_idx = 0;
		int action_idx = 0;
		String[] actions = ActionFactory.getActions();
		System.out.println("Starting iteration over elements");
		while(element_idx < pageNode.getData().getElements().size()){
			
			Timing.pauseThread(2000);
			try{
				System.out.println("EXECUTING ACTION :"+ actions[action_idx]+ " Now");
				ActionFactory.execAction(driver, pageNode.getData().getElements().get(element_idx), actions[action_idx]);
				
				//execute the following if it there is no problem executing action
				Page newPage = new Page(driver, DateFormat.getDateInstance(), false);

				if(!browser.getPage().equals(newPage)){
					System.out.println("PAGE HAS CHANGED. GROWING GRAPH...");
					//add action node to current element node
					//add current element node as input to the action node
					ConcurrentNode<PageElement> elementNode = new ConcurrentNode<PageElement>(pageNode.getData().getElements().get(element_idx));
					pageNode.addOutput(elementNode);
					elementNode.addInput(pageNode);
					
					ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
					elementNode.addOutput(actionNode);
					actionNode.addInput(elementNode);
					
					//Add new page to action output
					Page page = new Page(browser.getDriver(), DateFormat.getDateInstance(), true);
					actionNode.addOutput(new ConcurrentNode<Page>(page));
					//driver.navigate().refresh();
					//browser.updatePage(DateFormat.getDateInstance(), false);
				}	
				browser.close();
				browser = new Browser(url);
				this.driver = browser.getDriver();
				signIn("test@test.com", "testtest");
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				System.err.println("ACTOR EXECUTED ACTION :: " +actions[action_idx]);
			}
			catch(UnreachableBrowserException e){
				System.err.println("Browser is unreachable, pausing for 10 seconds");
				//Timing.pauseThread(5000);
			}
			catch(WebDriverException e){
				System.err.println("problem accessing webDriver instance");
				driver.close();
				browser = new Browser(url);
				
				this.driver = browser.getDriver();	
				signIn("test@test.com", "testtest");

				//visibleElements = pageNode.data.getVisibleElements(driver);
			}
			
			if(action_idx >= actions.length-1){
				action_idx = 0;
				element_idx++;
			}
			else{
				action_idx++;
			}
			System.out.println("ACTION IDS :: "+ action_idx + "; ELEMENT IDX :: "+element_idx);
		}
	}
	
	private void mapCrawler(Browser browser, String url, ConcurrentNode<Page> pageNode){
		browser = new Browser(url);
		ConcurrentHashMap<ConcurrentNode<?>, Double> map = pageNode.getOutputs();
		System.out.println("Map created");
		for(ConcurrentNode<?> element : pageNode.getOutputs().keySet()){
			PageElement pageElement = (PageElement)element.getData();
			ConcurrentHashMap<ConcurrentNode<?>, Double> elementMap = element.getOutputs();
			System.out.println("---Element and element map created");
			for(ConcurrentNode<?> action : elementMap.keySet()){
				//perform action on element. 
				System.out.println("EXECUTING ACTION :: " + action.getData().toString());
				ActionFactory.execAction(driver, pageElement, action.getData().toString());
				
				//check that response matches expected response based on action output
				ConcurrentHashMap<ConcurrentNode<?>, Double> actionOutputs = action.getOutputs();
				
				if(actionOutputs.keys().hasMoreElements()){
					System.out.println("ACTION HAS PAGES ASSOCIEATED THAT ARE EQUAL");
					Page page = (Page)actionOutputs.keys().nextElement().getData();

					//retrieve current browser page
					Page browserPage = browser.getPage();
					if(page.equals(browserPage)){
						System.out.println("PAGES MATCH AFTER CRAWL...LOOKIN GOOD!");
						//Everything is looking good. 
					}
					else{
						System.out.println("PAGE IS NOT THE SAME!!!! A CHANGE HAS BEEN ENCOUNTERED");
					}
					browser.close();
					browser = new Browser(url);
				}
				else{
					Page page = new Page(browser.getDriver(), DateFormat.getDateInstance(), true);
					action.addOutput(new ConcurrentNode<Page>(page));
					System.out.println("Added page to action since it didn't yet exist, yet here we are.");
				}
			}
		}
	}
}
