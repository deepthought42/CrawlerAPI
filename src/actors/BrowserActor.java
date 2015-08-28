package actors;
import graph.Graph;
import graph.Vertex;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import browsing.ElementAction;
import browsing.Page;
import browsing.PageAlert;
import browsing.PageElement;
import browsing.PageState;
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
	private Graph graph = null;
	private Path path = null;
	private Browser browser = null;
	private ResourceManagementActor resourceManager = null;
	private WorkAllocationActor workAllocator = null;
	private List<Integer> elementIdxChanges = null;
	
	/**
	 * 
	 * @param url
	 * @throws MalformedURLException
	 */
	public BrowserActor(String url) throws MalformedURLException {
		this.url = url;
		browser = new Browser(url);
		this.path = new Path();
	}

	/**
	 * Creates instance of BrowserActor with given url for entry into website
	 * 
	 * @param url	url of page to be accessed
	 * @param queue observable path queue
	 * @throws MalformedURLException 
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(String url, 
						Path path,
						ObservableQueue<Path> path_queue,
						Graph graph,
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws MalformedURLException {
		assert(path_queue != null);
		assert(path_queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.url = url;
		this.path = path;
		browser = new Browser(url);
		this.pathQueue = path_queue;
		this.graph = graph;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
		elementIdxChanges = null;
		
		if(this.path.getPath().isEmpty()){
			Vertex<Page> vertex = new Vertex<Page>(browser.getPage());
			graph.addVertex(vertex);
			
			this.path.add(graph.findVertexIndex(vertex));
			System.out.println(this.getName() + " -> Added Vertex Index to path while constructing BrowserActor");
		}
		elementIdxChanges = new ArrayList<Integer>();
	}
	
	/**
	 * Creates instance of browserActor with existing path to crawl.
	 * 
	 * @param queue ovservable path queue
	 * @param path	path to use to navigate to desired page
	 * @throws MalformedURLException 
	 * 
	 * @pre queue != null
	 * @pre !queue.isEmpty()
	 */
	public BrowserActor(ObservableQueue<Path> queue, 
						Graph graph, 
						Path path, 
						ResourceManagementActor resourceManager, 
						WorkAllocationActor workAllocator) throws MalformedURLException {
		assert(queue != null);
		assert(queue.isEmpty());
		
		this.uuid = UUID.randomUUID();
		this.graph = graph;
		Vertex<?> node = graph.getVertices().get(path.getPath().get(0)); 
		assert(((Page)node.getData()).getUrl() != null);
		this.path = path;
		this.url = ((Page)node.getData()).getUrl().toString();
		
		//System.out.println(this.getName() + " BROWSER ACTOR :: PATH HAS "+ path.getPath().size() + " NODES IN PATH");
		browser = new Browser(url);

		this.pathQueue = queue;
		this.resourceManager = resourceManager;
		this.workAllocator = workAllocator;
		elementIdxChanges = new ArrayList<Integer>();
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
					System.out.println(this.getName() + " -> Path is empty. Adding to path");
					Vertex<Page> vertex = new Vertex<Page>(browser.getPage());
					graph.addVertex(vertex);
					
					//System.out.println(this.getName() + " -> Vertex added to path. Method has returned.");
					//need to add edge to vertex
					this.path.add(graph.findVertexIndex(vertex));
				}
				else{
					System.out.println(this.getName() + " -> PATH IS NOT EMPTY. Working on path.");
					//System.out.println(this.getName() + " -> VERTEX INDEX : "+ this.path.getPath().get(0));
					//System.out.println(this.getName() + " -> VERTEX DATA TYPE :: " + graph.getVertices().get(this.path.getPath().get(0)).getData());
					this.url = ((Page)(graph.getVertices().get(this.path.getPath().get(0))).getData()).getUrl().toString();
					System.out.println(this.getName() + " -> NEW URL :: " + this.url);
					browser.getDriver().get(this.url);
				}
				boolean successfulCrawl = false;
				try{
					successfulCrawl = crawlPath();
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
				
				
				if(successfulCrawl){
					try {
						System.out.println(this.getName() + " EXPANDING NODE...");
						expandNodePath();
					} catch (MalformedURLException e) {
						System.err.println("URL FOR ONE OF PAGES IS MALFORMED");
					}
					catch(NullPointerException e){
						e.printStackTrace();
					}
				}
			
				long tEnd = System.currentTimeMillis();
				long tDelta = tEnd - tStart;
				double elapsedSeconds = tDelta / 1000.0;
				
				System.out.println(this.getName() + " -----ELAPSED TIME PATH NODE EXPANSION :: "+elapsedSeconds + "-----");
				System.out.println(this.getName() + " #######################################################");
				this.path = workAllocator.retrieveNextPath();
				System.out.println(this.getName() + " -> PATH RETRIEVED.");
				//close all windows opened during crawl
				try{
					String baseWindowHdl = browser.getDriver().getWindowHandle();
					Set<String> handles = browser.getDriver().getWindowHandles();
					if(handles.size() > 1){
						for(String winHandle : handles){
							browser.getDriver().switchTo().window(winHandle);
						}
						browser.getDriver().close();
						browser.getDriver().switchTo().window(baseWindowHdl);
						System.out.println(this.getName() + " -> CLOSED POPUP WINDOW.");
					}
				}
				catch(NullPointerException e){}
				catch(UnhandledAlertException e){
					e.printStackTrace();
				}
				
			}while(!this.pathQueue.isEmpty());
		}catch(OutOfMemoryError e){
			System.err.println(this.getName() + " -> Out of memory error");
			e.printStackTrace();
		}
		catch(NullPointerException e){
			System.err.println(this.getName() + " -> NULL POINTER EXCEPTION OCCURRED. --EXITING BROWSER ACTOR--");
			e.printStackTrace();
		}
		this.browser.getDriver().quit();
		resourceManager.punchOut(this);
	}
	
	/**
	 * Crawls the path for the current BrowserActor.
	 * @throws MalformedURLException 
	 */
	private boolean crawlPath() throws java.util.NoSuchElementException, UnhandledAlertException, MalformedURLException{
		long tStart = System.currentTimeMillis();

		
		Iterator<Integer> pathIterator = this.path.getPath().iterator();
		Path additionalNodes = new Path();
		Page pageNode = null;
		//skip first node since we should have already loaded it during initialization
		int i = 0;
		while(pathIterator.hasNext()){
			//System.out.println(this.getName() + " -> current path index :: " + i);
			int path_node_index = pathIterator.next();
			Vertex<?> pathNode = graph.getVertices().get(path_node_index);
						
			if(pathNode.getData() instanceof Page){
				pageNode = (Page)pathNode.getData();
				//verify current page matches current node data
				//if not mark as different
				if(!browser.getDriver().getPageSource().equals(pageNode.getSrc())){
					System.out.println(this.getName() + " -> page node source does not match expected Page source");
				}
				//If new page is assign elementIdxChanges to empty list
				elementIdxChanges = new ArrayList<Integer>();
			}
			else if(pathNode.getData() instanceof ElementAction){
				long tStart_pageState = System.currentTimeMillis();

				ElementAction elemAction = (ElementAction)pathNode.getData();
				
				//determine if elementAction is a child/descendent element action of another elementAction. 
				//If it is do not execute it
				for(Vertex<?> vertex : graph.getVertices()){
					if(vertex.getData() instanceof ElementAction){
						ElementAction elementActionVertex = (ElementAction)vertex.getData();
						if(elementActionVertex.getAction().equals(elemAction.getAction())
							&& elemAction.getPageElement().isChildElement(elementActionVertex.getPageElement())){
							
							ArrayList<Integer> toIndices = graph.getToIndices(graph.findVertexIndex(vertex));
							
							int element_action_count = 0;
							int impactful_vertex_count = 0;
							if(toIndices != null && !toIndices.isEmpty()){
								for(Integer index : toIndices){
									if(graph.getVertices().get(index).getData() instanceof ElementAction){
										element_action_count++;
									}
									else if(graph.getVertices().get(index).getData() instanceof PageState
											|| graph.getVertices().get(index).getData() instanceof Page
											|| graph.getVertices().get(index).getData() instanceof PageAlert){
										impactful_vertex_count++;
									}
								}
								if(impactful_vertex_count > 0){
									System.err.println(this.getName() + " ->  ||||  Parent vertex exists and has edges that do not lead to an element action");
									return false;
								}
							}
						}
					}
				}
				
				
				//execute element action
				boolean actionPerformedSuccessfully;
				do{
					actionPerformedSuccessfully = performAction(elemAction);	
				}while(!actionPerformedSuccessfully);
				
				/*
				 *Only use following for non phantomjs browsers
				 *
				 if(PageAlert.isAlertPresent(browser.getDriver())){
					if(i == this.path.getPath().size()-1 || (i < this.path.getPath().size() && !graph.getVertices().get(this.path.getPath().get(i+1)).getClass().equals(PageAlert.class))){
						PageAlert pageAlert = new PageAlert(pageNode, "accept", PageAlert.getMessage(PageAlert.getAlert(browser.getDriver())));
						
						Vertex<PageAlert> alertVertex = new Vertex<PageAlert>(pageAlert);
						//add edge from last path vertex to alertVertex
						graph.addVertex(alertVertex);
						//System.out.println(this.getName()  + " -> Vertex added");
						graph.addEdge(pathNode, alertVertex);
					}
					continue;
				}*/
				
				URL currentUrl = new URL(browser.getDriver().getCurrentUrl());
								
				Integer existingNodeIndex = graph.findVertexIndex(pathNode);
				Vertex<?> existingNode = null;
				
				if(existingNodeIndex == -1){
					existingNode = new Vertex<Page>(new Page(browser.getDriver(), DateFormat.getDateInstance()));
					if(graph.addVertex(existingNode)){
						graph.addEdge(path_node_index, existingNodeIndex);
						System.out.println(this.getName() + " -> Added new page to Graph");
					}
					else{
						System.out.println(this.getName() + " -> Failed to add page to PageMonitor");
					}
				}
				else{
					existingNode = graph.getVertices().get(existingNodeIndex);
					//System.out.println(this.getName() + " -> Node already existed. Using existing node");
					if(i >= this.path.getPath().size()){
						System.out.println(this.getName()+" -> Reached end of path.");
						//Still need to add in a way to add the current elementAction node to the new pageNode
						return false;
					}
				}
			
				//if not at end of path and next node is a Page or pageState then don't bother adding new node
				if(i < path.getPath().size()-1 && (graph.getVertices().get(path.getPath().get(i+1)).getClass().equals(Page.class) 
						|| graph.getVertices().get(path.getPath().get(i+1)).getClass().equals(PageState.class))){
					i++;
					continue;
				}
				//need to check if page is equal as well as if page state has changed
				else if(pageNode != null && !pageNode.equals(existingNode)){
					browser.updatePage( DateFormat.getDateInstance(), true);
					//Before adding new page, check if page has been experienced already. If it has load that page
					//System.out.println(this.getName() + " -> Page has changed...adding new page to path");
					//additionalNodes.add(existingNodeIndex);
				}
				else{
					long tStart_elementCheck = System.currentTimeMillis();

					
					
					
					PageState pageState = null;
					//else if after performing action styles on one or more of the elements is no longer equal then mark element as changed.
					List<PageElement> pageElements = pageNode.getElements();
					for(int idx=0; idx < pageElements.size(); idx++){
						WebElement elem = browser.getDriver().findElement(By.xpath(pageElements.get(idx).getXpath()));
						PageElement newElem = new PageElement(browser.getDriver(), elem, pageNode, null);
						if(!newElem.equals(pageElements.get(idx))){
							System.out.println(this.getName() + " -> Node differs from initial page node. Adding index to list of changed elements");
							if(elementIdxChanges.contains(idx)){
								System.out.println(this.getName() + " -> Node has changed previously. Exiting crawl.");
								return false;
							}
							elementIdxChanges.add(idx);
							
							//remove element from page list and replace with new element
							pageElements.remove(idx);
							pageElements.add(idx, newElem);
							
							pageState = new PageState(pageNode.getUuid());
							pageState.addChangedPageElement(newElem);
						}
					}
					
					
					long tEnd_pageState = System.currentTimeMillis();
					long tDelta = tEnd_pageState - tStart_elementCheck;
					double elapsedSeconds = tDelta / 1000.0;
					System.out.println(this.getName() + " -----ELAPSED TIME TO CHECK FOR ALL ELEMENTS EQUAL IN BOTH LISTS :: "+elapsedSeconds + "-----");
					System.out.println(this.getName() + " #######################################################");
					
					if(pageState == null){
						return false;
					}
					else{
						Vertex<PageState> pageStateVertex = new Vertex<PageState>(pageState);
						graph.addVertex(pageStateVertex);
						graph.addEdge(existingNode, pageStateVertex);
						additionalNodes.add(graph.findVertexIndex(pageStateVertex));
					}
				}
				
				
				
				
				
				
				long tEnd_pageState = System.currentTimeMillis();
				long tDelta = tEnd_pageState - tStart_pageState;
				double elapsedSeconds = tDelta / 1000.0;
				System.out.println(this.getName() + " -----ELAPSED TIME TO PERFORM ELEMENT ACTION SUCCESSFULLY :: "+elapsedSeconds + "-----");
				System.out.println(this.getName() + " #######################################################");

			}
			else if(pathNode.getData() instanceof PageAlert){
				System.err.println(this.getName() + " -> Handling Alert");
				PageAlert alert = (PageAlert)pathNode.getData();
				alert.performChoice(browser.getDriver());
			}
			i++;
		}
		this.path.append(additionalNodes);
		
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		
		System.out.println(this.getName() + " -----ELAPSED crawl TIME :: "+elapsedSeconds + "-----");
		System.out.println(this.getName() + " #######################################################");
		
		return true;
	}
	
	/**
	 * Finds all potential expansion nodes from current node for {@link Elements}, {@link ElementActions}, {@link PageStates} and {@link Page}
	 * @throws MalformedURLException 
	 */
	private void expandNodePath() throws MalformedURLException{
		long tStart = System.currentTimeMillis();

		Vertex<?> node_vertex = graph.getVertices().get(this.path.getPath().get(this.path.getPath().size()-1));
		
		Class<?> className = node_vertex.getData().getClass();
		String[] actions = ActionFactory.getActions();

		//if node is a page then find all potential elementActions that can be taken including different values
		//if node is an elementAction find all elementActions for the last seen page node that have not been seen
		//   since the page node was encountered and add them.
		if(className.equals(Page.class)){
			//verify current page matches current node data
			//if not mark as different
			Page page = (Page)node_vertex.getData();
			Page newPage = new Page(browser.getDriver(), DateFormat.getDateInstance());
			if(!page.equals(newPage)){
				return;
			}
			//get all known possible compinations of PageElement actions and add them as potential expansions
			ArrayList<PageElement> elementList = page.getElements();
			for(int elemIdx=0; elemIdx < page.getElements().size(); elemIdx++){
				PageElement elem = (PageElement) elementList.get(elemIdx);
						
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i], elemIdx);
					//Clone path then add ElementAciton to path and push path onto path queue					
					Vertex<ElementAction> elementActionVertex = new Vertex<ElementAction>(elemAction);
										
					boolean addedVertex = graph.addVertex(elementActionVertex);
					//Add edge to graph for vertex
					graph.addEdge(node_vertex, elementActionVertex);
					int vertex_idx = graph.findVertexIndex(elementActionVertex);
					if(!path.getPath().contains(vertex_idx)){
						Path new_path = Path.clone(path);
						new_path.add(vertex_idx);
						putPathOnQueue(new_path);
					}
				}				
			}
		}
		else if(className.equals(ElementAction.class)){
			ArrayList<ElementAction> elementActionSeenList = new ArrayList<ElementAction>();
			//navigate path back to last seen page
			//for each ElementAction seen, record elementAction.
			Page page = null;
			Vertex<?> descNode = null;
			for(int i = 0; i < this.path.getPath().size(); i--){
				 descNode = graph.getVertices().get(this.path.getPath().get(i));
				
				if(descNode.getData() instanceof Page){
					page = (Page)descNode.getData();
					break;
				}
				else{
					elementActionSeenList.add((ElementAction)descNode.getData());
				}
			}
			
			//add each elementAction for last seen page excluding elementActions with elements seen while finding page
			ArrayList<PageElement> elementList = page.getElements();
			for(int elemIdx=0; elemIdx < elementList.size(); elemIdx++){
				PageElement elem = (PageElement) elementList.get(elemIdx);
				for(int i = 0; i < actions.length; i++){
					ElementAction elemAction = new ElementAction(elem, actions[i], elemIdx);
					Iterator<?> seenElementIterator = elementActionSeenList.iterator();
					boolean seen = false;
					while(seenElementIterator.hasNext()){
						if(((ElementAction)seenElementIterator.next()).getPageElement().equals(elemAction.getPageElement())){
							seen = true;
						}
					}
					if(!seen){
						//Clone path then add ElementAction to path and push path onto path queue					
						Vertex<ElementAction> elementActionVertex = new Vertex<ElementAction>(elemAction);
						
						graph.addVertex(elementActionVertex);
						graph.addEdge(node_vertex, elementActionVertex);
						
						//need to add edge to graph
						int vertex_idx = graph.findVertexIndex(elementActionVertex);
						//if(!path.getPath().contains(vertex_idx)){
						Path new_path = Path.clone(path);
						new_path.add(vertex_idx);
						putPathOnQueue(new_path);
						//}
					}
				}				
			}
		}
		else if(className.equals(PageAlert.class)){
			System.err.println(this.getName() + " -> Handling Alert from expanding Node.");
			PageAlert alert = (PageAlert)node_vertex.getData();
			alert.performChoice(browser.getDriver());
		}
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		
		System.out.println(this.getName() + " -----ELAPSED TIME EXPANDING PATH NODE :: "+elapsedSeconds + "-----");
		System.out.println(this.getName() + " #######################################################");
	}
	
	/**
	 * Adds the given {@link Vertex vertex} to the queue
	 * 
	 * @param path path to be added
	 */
	private boolean putPathOnQueue(Path path){
		return this.pathQueue.add(path);
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
			System.err.println(this.getName() + " -> NO SUCH ELEMENT EXCEPTION");
			wasPerformedSuccessfully = false;
		}
		
		return wasPerformedSuccessfully;
	}
}
