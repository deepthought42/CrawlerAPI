package browsing;
import java.text.DateFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;


/**
 * An this threadable class is implemented to handle the interaction with a browser 
 * @author Brandon Kindred
 *
 */
public class BrowserActor implements Runnable {

	private static WebDriver driver;
	private String url = null;
	private ConcurrentNode<Page> pageNode = null;

	private static Browser browser = null;
	
	public BrowserActor(String url) {
		this.url = url;
		driver = Browser.openWithFirefox(url);
		browser = new Browser(driver, url);
	}

	public BrowserActor(String url, ConcurrentNode<Page> pageNode) {
		this.url = url;
		browser = new Browser(driver, url);
		this.pageNode = pageNode;
		driver = Browser.openWithFirefox(url);
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
			pageCrawler(this.pageNode);
			System.out.println("FINISHED EXECUTING ALL ACTIONS FOR THIS PAGE");
			System.out.println("#######################################################");
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
	private void pageCrawler(ConcurrentNode<Page> pageNode){
		System.out.println("Commencing page crawl...");
		int element_idx = 0;
		int action_idx = 0;
		String[] actions = ActionFactory.getActions();
		//System.out.println("Starting iteration over elements");
		while(element_idx < pageNode.getData().getElements().size()){
			boolean err = false;
			Page page = pageNode.getData();
			try{
				//System.out.println("EXECUTING ACTION :"+ actions[action_idx]+ " ON ELEMENT WITH ID " + element_idx + " Now");
				//System.out.println("PAGE NODE :: " + pageNode);
				PageElement pageElement = page.getElements().get(element_idx);
				//System.out.println("WEB ELEMENT ATTRIBUTES :: " + pageElement.getAttributes().size());
				String xpath = pageElement.getXpath();
				
				//if xpath comes back without conditions of tag then find its index within the page by counting how many 
				// tags with the same tag name came before it.
				if(!xpath.contains("[")){
					//start at 1 because xpath elements start counting at 1
					int tag_idx = 1;
					for(int i=0; i<page.getElements().size(); i++){
						if(i == element_idx){
							break;
						}
						if(page.getElements().get(i).getTagName().equals(pageElement.getTagName())){
							tag_idx++;
						}
					}
					xpath+="["+tag_idx+"]";
					pageElement.setXpath(xpath);
				}
				//System.out.println("GENERATED XPATH == " + xpath);
				WebElement elem = driver.findElement(By.xpath(xpath));
				
								
				//execute the following if there is no problem executing action
				Page newPage = new Page(driver, DateFormat.getDateInstance(), false);

				ActionFactory.execAction(driver, elem , actions[action_idx]);
				//add action node to current element node
				//add current element node as input to the action node
				ConcurrentNode<PageElement> elementNode = new ConcurrentNode<PageElement>(pageNode.getData().getElements().get(element_idx));
				pageNode.addOutput(elementNode);
				elementNode.addInput(pageNode);

				ConcurrentNode<String> actionNode = new ConcurrentNode<String>(actions[action_idx]);
				elementNode.addOutput(actionNode);
				actionNode.addInput(elementNode);
				
				//if the page has changed the create a new page node and associate it with the current actionNode
				// otherwise link the action back to the last seen pageNode.
				if(!browser.getPage().equals(newPage)){
					System.out.println("PAGE HAS CHANGED. GROWING GRAPH...");
					//Add new page to action output
					actionNode.addOutput(new ConcurrentNode<Page>(newPage));
					driver.get(url);
					browser = new Browser(url, pageNode.getData());
				}	
				else{
					System.out.println("PAGE DIDN'T CHANGED. DEAD ENDING FOR NOW.");
					//Since there was no change we want to dead end here. This will allow for later passes to 
					//	build out more complex functionality recognition by chaining element action sequences.
				}
				//signIn("test@test.com", "testtest");
			}
			catch(StaleElementReferenceException e){
				System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE ACTOR WAS PERFORMING ACTION : "+
						actions[action_idx] + ". ");
				System.err.println("ACTOR EXECUTED ACTION :: " +actions[action_idx]);
				e.printStackTrace();
				driver.get(url);
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
			catch(WebDriverException e){
				err = true;
				System.err.println("problem accessing WebDriver instance");
				e.printStackTrace();
				driver.get(url);
				browser = new Browser(url, pageNode.getData());
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
		driver.get(url);
		ConcurrentHashMap<ConcurrentNode<?>, Double> map = pageNode.getOutputs();
		System.out.println("Map created. There were " + map.size() + " output links for this node");
		for(ConcurrentNode<?> element : pageNode.getOutputs().keySet()){
			PageElement pageElement = (PageElement)element.getData();
			ConcurrentHashMap<ConcurrentNode<?>, Double> elementMap = element.getOutputs();
			System.out.println("---Element and element map created");
			String xpath = pageElement.generateXpath();

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
