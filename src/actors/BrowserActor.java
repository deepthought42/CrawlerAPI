package actors;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.swing.text.DateFormatter;

import observableStructs.ObservableQueue;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;

import browsing.ActionFactory;
import browsing.Browser;
import browsing.ConcurrentNode;
import browsing.ElementAction;
import browsing.Page;
import browsing.PageElement;
import structs.Path;

/*
 * NEEDED:
 * 		NAVIGATOR/CRAWLER -- crawl path to end
 * 		MAPCOLORER -- mark completely evaluated nodes as seen{1}, mapped{2}
 * 		NODE_BUILDER -- find all possible nodes that can be added and add them to the current node. 
 * 						for each new node added, create a new path with the new node as the last node on path
 * 
 * 
 * 
 * Retrieve path fom current path variable. 
 * Crawl path to end of chain.
 * If node at end of chain is a page, then add element to path
 * If node at end of chain is an element, then perform action and add action to path following element
 * 		If performing action results in change of page then add page to action. add page to path
 * If node at end of chain is an action, then add an element to the action node and to end of path.
 * 
 * if all elements for a page have been evaluated and added to the page node then mark page node as mapped {1}
 * If last element in path is an action node and all elements not including the elements preceding the action in the path 
 *    have been added to the action node then color node as mapped
 * 
 *    
 */
/**
 * An this threadable class is implemented to handle the interaction with a browser 
 * @author Brandon Kindred
 *
 */
public class BrowserActor extends Thread implements Actor{

	private UUID uuid = null;
	private static WebDriver driver;
	private String url = null;
	private ObservableQueue<Path> pathQueue = null;
	private ConcurrentNode<Page> pageNode = null;
	private Path path = null;
	private Path clonePath = null;
	private List<Page> pagesSeen = new ArrayList<Page>();
	private Browser browser = null;
	private ResourceManagementActor resourceManager = null;
	private WorkAllocationActor workAllocator = null;
	private ArrayList<PageElement> currentElements = null;
	
	public BrowserActor(String url) {
		this.url = url;
		browser = new Browser(url);
		this.path = new Path();
	}

	/**
	 * Creates instance of BrowserActor with given url for entry into website
	 * 
	 * @param url	url of page to be accessed
	 * @param queue observable path queue
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(String url, ObservableQueue<Path> queue, ResourceManagementActor resourceManager, WorkAllocationActor workAllocator) {
		assert(queue != null);
		assert(queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		browser = new Browser(url);
		this.pathQueue = queue;
		this.path = new Path();
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
		
		if(this.path.getPath().isEmpty()){
			this.path.add( new ConcurrentNode<Page>(browser.getPage()));
			System.out.println(this.getName() + " PATH LENGTH :: "+this.path.getPath().size());
		}
	}
	
	/**
	 * Creates instance of browserActor with existing path to crawl.
	 * 
	 * @param queue ovservable path queue
	 * @param path	path to use to navigate to desired page
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(ObservableQueue<Path> queue, Path path, ResourceManagementActor resourceManager, WorkAllocationActor workAllocator) {
		assert(queue != null);
		assert(queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.path = path;
		ConcurrentNode<?> node = (ConcurrentNode<?>) path.getPath().getFirst(); 
		assert(((Page)node.getData()).getUrl() != null);

		this.url = ((Page)node.getData()).getUrl().getPath();
		
		System.out.println(this.getName() + " BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES IN PATH");
		browser = new Browser(url);

		this.pathQueue = queue;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
	}
	
	/**
	 * 
	 * @return PageNode
	 */
	public ConcurrentNode<Page> getPageNode(){
		return this.pageNode;
	}
	
	public void signIn(String username, String pass){
		driver.findElement(By.xpath("//a[@id='signInLink']")).click();
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
		resourceManager.punchIn(this);
		try{
			do{
				long tStart = System.currentTimeMillis();
	
				if(this.path.getPath().isEmpty()){
					this.path.add(new ConcurrentNode<Page>(browser.getPage()));
					System.out.println(this.getName() + " PATH LENGTH :: "+this.path.getPath().size());
				}
				else{
					this.url = ((Page)((ConcurrentNode<?>)path.getPath().getFirst()).getData()).getUrl().getPath();
					System.out.println(Thread.currentThread().getName() + " -> NEW URL :: " + this.url);
					browser.getDriver().get(this.url);
				}
				currentElements = browser.getPage().getElements();
				try{
					crawlPath();
				}catch(NoSuchElementException e){
					System.err.println(this.getName() + " NO SUCH ELEMENT FOUND IN PATH. PATH IS EMPTY");
					e.printStackTrace();
				}
				catch(UnhandledAlertException e){
					System.err.println(this.getName() + " -> UNHANDLED ALERT EXCEPTION OCCURRED");
					try{
						Alert alert = browser.getDriver().switchTo().alert();
				        alert.accept();
					}
					catch(NoAlertPresentException nae){
						System.err.println(this.getName() + " -> Alert not present");
					}
				}
				catch(MalformedURLException e){
					System.err.println("URL FOR ONE OF PAGES IS MALFORMED");
				}
				
				//System.out.println(this.getName() + " EXPANDING NODE...");
				try {
					expandNodePath();
				} catch (MalformedURLException e) {
					System.err.println("URL FOR ONE OF PAGES IS MALFORMED");
				}
				//System.out.println(this.getName() + " NODE EXPANDED..");
				
				long tEnd = System.currentTimeMillis();
				long tDelta = tEnd - tStart;
				double elapsedSeconds = tDelta / 1000.0;
				
				System.out.println(this.getName() + " -----ELAPSED TIME FOR CRAWL :: "+elapsedSeconds + "-----");
				System.out.println(this.getName() + " #######################################################");
				this.path = workAllocator.retrieveNextPath();
				
				//close all windows opened during crawl
				String baseWindowHdl = driver.getWindowHandle();
				for(String winHandle : driver.getWindowHandles()){
				    driver.switchTo().window(winHandle);
				}
				driver.close();
				driver.switchTo().window(baseWindowHdl);

			}while(!this.pathQueue.isEmpty());
		}catch(OutOfMemoryError e){
			System.err.println(this.getName() + " -> Out of memory error");
		}
		this.browser.close();
		resourceManager.punchOut(this);
	}
	
	/**
	 * Crawls the path for the current BrowserActor.
	 * @throws MalformedURLException 
	 */
	private void crawlPath() throws java.util.NoSuchElementException, UnhandledAlertException, MalformedURLException{
		Iterator pathIterator = this.path.getPath().iterator();
		Path additionalNodes = new Path();
		ActionFactory actionFactory = new ActionFactory(this.browser.getDriver());
		Page pageNode = null;
		//skip first node since we should have already loaded it during initialization
		int i = 0;
		while(pathIterator.hasNext()){
			System.out.println(this.getName() + " -> current path index :: " + i);
			ConcurrentNode<?> pathNode = (ConcurrentNode<?>) pathIterator.next();
			
			String className = pathNode.getData().getClass().getCanonicalName();
			
			if(className.equals("browsing.Page")){
				pageNode = (Page)pathNode.getData();
				System.out.println(this.getName() + " -> PAGE NODE SEEN");
				//verify current page matches current node data
				//if not mark as different
			}
			else if(className.equals("browsing.ElementAction")){
				ElementAction elemAction = (ElementAction)pathNode.getData();
				//execute element action
				boolean actionPerformedSuccessfully;
				do{
					actionPerformedSuccessfully = performAction(elemAction);	
				}while(!actionPerformedSuccessfully);
								
				Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), true);
				//if after performing action page is no longer equal do stuff
			
				//if not at end of path and next node is a Page then don't bother adding new node
				if(i < path.getPath().size()-1 && ((ConcurrentNode<?>)path.getPath().get(i+1)).getData().getClass().getCanonicalName().equals("browsing.Page")){
					i++;
					continue;
				}
				else if(pageNode != null && !pageNode.equals(newPage)){
					
					browser.updatePage( DateFormat.getDateInstance(), true);
					System.out.println(this.getName() + " -> CURRENT PATH SIZE = "+this.path.getPath().size());
					System.out.println(this.getName() + " -> current path index :: " + i);
					//Before adding new page, check if page has been experienced already. If it has load that page
					//Before
					
					System.out.println(this.getName() + " -> Page has changed...adding new page to path");
					ConcurrentNode<Page> newPageNode = new ConcurrentNode<Page>(newPage);
					System.out.println(this.getName() + " PAGE = "+newPageNode.getData().toString());
					pathNode.addOutput(newPageNode);
					newPageNode.addInput(pathNode);
					additionalNodes.add(newPageNode);
				}
				
				//else if after performing action styles on one or more of the elements is no longer equal then mark element as changed.
				//	An element that has changed cannot change again. If it does then the path is marked as dead
			}
			i++;
		}
		System.out.println(this.getName() + " -> EXISTING PATH LENGTH = "+this.path.getPath().size());
		System.out.println(this.getName() + " -> EXISTING ADDITIONAL PATH LENGTH = "+additionalNodes.getPath().size());
		this.path.append(additionalNodes);
		System.out.println(this.getName() + " -> DONE CRAWLING PATH");
	}
	
	/**
	 * Finds all potential expansion nodes from current node
	 * @throws MalformedURLException 
	 */
	private void expandNodePath() throws MalformedURLException{
		System.out.println(this.getName() + " SETTING UP EXPANSION VARIABLES..");
		ConcurrentNode<?> node = (ConcurrentNode<?>) this.path.getPath().getLast();
		String className = node.getData().getClass().getCanonicalName();
		String[] actions = ActionFactory.getActions();

		//if node is a page then find all potential elementActions that can be taken including different values
		//if node is an elementAction find all elementActions for the last seen page node that have not been seen
		//   since the page node was encountered and add them.
		if(className.equals("browsing.Page")){
			//verify current page matches current node data
			//if not mark as different
			Page page = (Page)node.getData();
			Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance(), false);
			if(!page.equals(newPage)){
				return;
			}
			//get all known possible compinations of PageElement actions and add them as potential expansions
			ArrayList<PageElement> elementList = page.getElements();
			for(int elemIdx=0; elemIdx < page.getElements().size(); elemIdx++){
				PageElement elem = (PageElement) elementList.get(elemIdx);
						
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i], elemIdx);
					//System.out.println("ADDING ACTION TO ELEMENT :: " + actions[i]);
					//Clone path then add ElementAciton to path and push path onto path queue					
					ConcurrentNode<ElementAction> elementAction = new ConcurrentNode<ElementAction>(elemAction);
					elementAction.addInput(node);
					node.addOutput(elementAction);
					
					putPathOnQueue(elementAction);
				}				
			}
		}
		else if(className.equals("browsing.ElementAction")){
			ArrayList<ElementAction> elementActionSeenList = new ArrayList<ElementAction>();
			//navigate path back to last seen page
			//for each ElementAction seen, record elementAction.
			Iterator descendingIterator = this.path.getPath().descendingIterator();
			Page page = null;
			
			while(descendingIterator.hasNext()){
				ConcurrentNode<?> descNode = (ConcurrentNode<?>) descendingIterator.next();
				
				if(descNode.getData().getClass().getCanonicalName().equals("browsing.Page")){
					page = (Page)descNode.getData();
					break;
				}
				else{
					elementActionSeenList.add((ElementAction)descNode.getData());
				}
			}
			
			//add each elementAction for last seen page excluding elementActions with elements seen while finding page
			Iterator elementIterator = page.getElements().iterator();
			ArrayList<PageElement> elementList = page.getElements();
			for(int elemIdx=0; elemIdx < page.getElements().size(); elemIdx++){
				PageElement elem = (PageElement) elementList.get(elemIdx);
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i], elemIdx);
					Iterator seenElementIterator = elementActionSeenList.iterator();
					boolean seen = false;
					while(seenElementIterator.hasNext()){
						if(((ElementAction)seenElementIterator.next()).getPageElement().equals(elemAction.getPageElement())){
							seen = true;
						}
					}
					if(!seen){
						//Clone path then add ElementAciton to path and push path onto path queue					
						ConcurrentNode<ElementAction> elementAction = new ConcurrentNode<ElementAction>(elemAction);
						elementAction.addInput(node);
						node.addOutput(elementAction);
					
						putPathOnQueue(elementAction);
					}
				}				
			}
		}
	}
	
	/**
	 * Adds the given {@link Path path} to the queue
	 * 
	 * @param path path to be added
	 */
	private boolean putPathOnQueue(ConcurrentNode<?> node){
		Path clonePath = Path.clone(path);
		clonePath.add(node);
		//System.out.println("CLONE PATH LENGTH :: "+clonePath.getPath().size());
		return this.pathQueue.add(clonePath);
	}

	/**
	 * Get the UUID for this Agent
	 */
	public UUID getActorId(){
		return uuid;
	}
	
	
	/**
	 * Executes the given {@link ElementAction element action} pair. 
	 * 
	 * @param elemAction 
	 * @return whether action was able to be performed on element or not
	 */
	private boolean performAction(ElementAction elemAction){
		ActionFactory actionFactory = new ActionFactory(this.browser.getDriver());
		boolean wasPerformedSuccessfully = true;
		
		try{
			WebElement element = browser.getDriver().findElement(By.xpath(elemAction.getPageElement().getXpath()));
			actionFactory.execAction(element, elemAction.getAction());
			System.err.println(this.getName() + " -> Performed action "+elemAction.getAction()+ " On element with xpath :: "+elemAction.getPageElement().getXpath());
		}
		catch(StaleElementReferenceException e){
			System.out.println(this.getName() + " :: STALE ELEMENT REFERENCE EXCEPTION OCCURRED WHILE ACTOR WAS PERFORMING ACTION : "+
					elemAction.getAction() + ". ");
			//e.printStackTrace();
			wasPerformedSuccessfully = false;
		}
		catch(UnreachableBrowserException e){
			System.err.println(this.getName() + " :: Browser is unreachable.");
			wasPerformedSuccessfully = false;
		}
		catch(ElementNotVisibleException e){
			System.out.println(this.getName() + " :: ELEMENT IS NOT CURRENTLY VISIBLE.");
		}
		catch(NoSuchElementException e){
			System.err.println("NO SUCH ELEMENT EXCEPTION");
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
}
