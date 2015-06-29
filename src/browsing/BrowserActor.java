package browsing;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * An this threadable class is implemented to handle the interaction with a browser 
 * @author Brandon Kindred
 *
 */
public class BrowserActor implements Runnable {

	private static WebDriver driver;
	private String url = null;
	private ConcurrentNode<Page> pageNode = null;
	private List<Page> pagesSeen = new ArrayList<Page>();
	private static Browser browser = null;
	
	public BrowserActor(String url) {
		this.url = url;
		browser = new Browser(driver, url);
	}

	public BrowserActor(String url, ConcurrentNode<Page> pageNode) {
		this.url = url;
		browser = new Browser(driver, url);
		this.pageNode = pageNode;
	}
	
	public void signIn(String username, String pass){
		driver.findElement(By.xpath("//a[@id='signInLink']")).click();
	    //new WebDriverWait(driver, 5).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='userFormEmailField']")));
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

		System.out.println("RETRIEVING DRIVER...");
		//signIn("test@test.com", "testtest");

		System.out.println("Built page instance.");
		//TODO :: LOAD PAGE NODE FROM MEMORY FOR GIVEN URL.
		// IF IT EXISTS CHECK IF IT HAS BEEN MAPPED. IF IT HAS NOT THEN MAP IT
		// ELSE CRAWL MAP TO FIND NEW PAGES TO MAP
		this.pageNode = new ConcurrentNode<Page>(browser.getPage());
		System.out.println("Wrapped page instance in a graph node");
		
		System.out.println("----------------------------------------------------");
		System.out.println("loaded up elements. there were " + this.pageNode.data.getElements().size());
		System.out.println("----------------------------------------------------");
		if(this.pageNode.getOutputs() != null & this.pageNode.getOutputs().isEmpty()){
			long tStart = System.currentTimeMillis();
			pageCrawler(this.pageNode);
			long tEnd = System.currentTimeMillis();
			long tDelta = tEnd - tStart;
			double elapsedSeconds = tDelta / 1000.0;
			
			System.out.println("-----ELAPSED TIME FOR CRAWL :: "+elapsedSeconds + "-----");
			System.out.println("FINISHED EXECUTING ALL ACTIONS FOR THIS PAGE");
			System.out.println("#######################################################");
		}
		System.out.println("CRAWLING PAGE COMPLETE...");
		System.out.println("TOTAL NUMBER OF NEW PAGES :: "+pagesSeen.size());
		System.out.println("Number of elements in page output :: " + pageNode.getOutputs().size());
		
		//else{
			System.out.println("Page node already has outputs. lets crawl them!");
			//browser = new Browser(url);
			//mapCrawler(browser, url, this.pageNode);
			
			ConcurrentHashMap<ConcurrentNode<?>, Double> map = pageNode.getOutputs();
			System.out.println("Map created. There were " + map.size() + " output links for this node");
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
	private void pageCrawler(ConcurrentNode<Page> pageNode){
		System.out.println("Commencing page crawl...");
		int element_idx = 0;
		int last_element_idx = -1;
		ConcurrentNode<PageElement> elementNode = null;
		int action_idx = 0;
		String[] actions = ActionFactory.getActions();
		//System.out.println("Starting iteration over elements");
		Page page = pageNode.getData();
		page.getVisibleElements(browser.getDriver(), page.getElements(), "//body");
		pagesSeen.add(page);
		while(element_idx < pageNode.getData().getElements().size()){
			boolean err = false;
			try{
				//System.out.println("EXECUTING ACTION :"+ actions[action_idx]+ " ON ELEMENT WITH ID " + element_idx + " Now");
				//System.out.println("PAGE NODE :: " + pageNode);
				PageElement pageElement = page.getElements().get(element_idx);
				//System.out.println("WEB ELEMENT ATTRIBUTES :: " + pageElement.getAttributes().size());
				//System.out.println("THERE WERE " +driver.findElements(By.xpath(pageElement.getXpath())).size()+" ELEMENTS FOR XPATH :: "+ pageElement.getXpath());

				//System.out.println("GENERATED XPATH == " + pageElement.getXpath());
				WebElement elem = browser.getDriver().findElement(By.xpath(pageElement.getXpath()));
				//JavascriptExecutor javascriptDriver = (JavascriptExecutor)driver;

		        //javascriptDriver.executeScript("arguments[0].style.border='3px solid red'", elem);

								
				//execute the following if there is no problem executing action

				ActionFactory.execAction(browser.getDriver(), elem , actions[action_idx]);
				
				Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), false);

				//check if element already has been added. If it has then skip
				// adding element to page and just add action to element
				
				//add action node to current element node
				//add current element node as input to the action node
				
				if(element_idx != last_element_idx){
					elementNode = new ConcurrentNode<PageElement>(pageElement);
					pageNode.addOutput(elementNode);
					elementNode.addInput(pageNode);
					last_element_idx = element_idx;
				}
				
				ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
				elementNode.addOutput(actionNode);
				actionNode.addInput(elementNode);
				
				//if the page has changed the create a new page node and associate it with the current actionNode
				// otherwise link the action back to the last seen pageNode.
				if(!browser.getPage().equals(newPage)){
					boolean previouslySeen = false;
					for(Page pageSeen : pagesSeen){
						if(pageSeen.equals(newPage)){
							previouslySeen = true;
						}
					}
					if(!previouslySeen){
						pagesSeen.add(newPage);
					}
					System.out.println("PAGE HAS CHANGED. GROWING GRAPH...");
					//Add new page to action output
					actionNode.addOutput(new ConcurrentNode<Page>(newPage));
				}	
				else{
					System.out.println("PAGE DIDN'T CHANGED. DEAD ENDING FOR NOW.");
					//Since there was no change we want to dead end here. This will allow for later passes to 
					//	build out more complex functionality recognition by chaining element action sequences.
				}
				browser.getUrl(url);
				//browser = new Browser(url, pageNode.getData());

				//signIn("test@test.com", "testtest");
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				//e.printStackTrace();
				browser.getUrl(url);
			}
			catch(UnreachableBrowserException e){
				System.err.println("Browser is unreachable.");
				System.out.println("DRIVER :: " + driver);
				err = true;
				break;
				//driver.close();
				//driver = Browser.openWithFirefox(url);
				//browser = new Browser(url, pageNode.getData());
				
				//e.printStackTrace();
			}
			catch(ElementNotVisibleException e){
				System.out.println("ELEMENT IS NOT CURRENTLY VISIBLE.");
			}
			catch(InvalidSelectorException e){
				System.out.println("INVALID SELECTOR");
				//e.printStackTrace();
			}
			catch(WebDriverException e){
				err = true;
				System.err.println("problem accessing WebDriver instance");
				e.printStackTrace();
				browser.getUrl(url);
				//browser = new Browser(url, pageNode.getData());
				//signIn("test@test.com", "testtest");
			}
			
			if(!err){
				if(action_idx >= actions.length-1){
					action_idx = 0;
					element_idx++;
				}
				else{
					action_idx++;
				}
			}
			System.out.println("PERFORMING ACTION :: "+ actions[action_idx] + " on element at index :: "+element_idx);
		}
	}
	
	private void mapCrawler(Browser browser, String url, ConcurrentNode<Page> pageNode){
		browser = new Browser(driver, url);
		browser.getUrl(url);
		ConcurrentHashMap<ConcurrentNode<?>, Double> map = pageNode.getOutputs();
		System.out.println("Map created. There were " + map.size() + " output links for this node");
		for(ConcurrentNode<?> element : pageNode.getOutputs().keySet()){
			PageElement pageElement = (PageElement)element.getData();
			ConcurrentHashMap<ConcurrentNode<?>, Double> elementMap = element.getOutputs();
			System.out.println("---Element and element map created");
			String xpath = pageElement.generateXpath(driver);

			WebElement elem = driver.findElement(By.xpath(xpath));
			for(ConcurrentNode<?> action : elementMap.keySet()){
				//perform action on element. 
				System.out.println("EXECUTING ACTION :: " + action.getData().toString());
				ActionFactory.execAction(driver, elem, action.getData().toString());
				
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
					//browser.close();
					browser = new Browser(driver, url);
					driver.get(url);
				}
				else{
					Page page = new Page(driver, DateFormat.getDateInstance(), true);
					action.addOutput(new ConcurrentNode<Page>(page));
					System.out.println("Added page to action since it didn't yet exist, yet here we are.");
				}
			}
		}
	}
	
	
}
