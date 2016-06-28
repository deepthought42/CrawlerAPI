package structs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import actors.BrowserActor;
import browsing.ActionFactory;
import browsing.Browser;
import browsing.IObjectValuationAccessor;
import browsing.PathObject;
import browsing.Page;
import browsing.PageElement;
import browsing.actions.Action;

/**
 * A set of vertex objects that form a sequential movement through a graph
 * 
 * @author Brandon Kindred
 *
 */
public class Path {
    private static final Logger log = Logger.getLogger(BrowserActor.class);
	
	private Boolean isUseful;
	private boolean spansMultipleDomains;
	public List<PathObject> path = null;

	/**
	 * Creates new instance of Path
	 */
	public Path(){
		this.isUseful = null;
		this.setSpansMultipleDomains(false);
		this.path = new ArrayList<PathObject>();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.isUseful = null;
		this.path = new  ArrayList<PathObject>();
		this.append(current_path);
	}
	
	/**
	 * 
	 * @return
	 */
	public void append(Path appendablePath){
		
		for(PathObject obj : appendablePath.getPath()){
			this.path.add(obj);
		}				
	}
		
	public boolean add(PathObject obj){
		return this.path.add(obj);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<PathObject> getPath(){
		return this.path;
	}
	
	public void setIsUseful(boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public Boolean isUseful(){
		return this.isUseful;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Path path){
		int thisPathLength = this.path.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			System.out.println("PATHS ARE NOT EQUAL");
			return false;
		}
		for(int i = 0; i < thisPathLength; i++){
			Object thisPathNode = this.path.get(i);
			Object comparatorPathNode = path.getPath().get(i);
			
			if(!thisPathNode.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
				System.out.println("NODE CLASS NAMES ARE NOT EQUAL");
				return false;
			}
			if(thisPathNode != comparatorPathNode){
				System.out.println("NODE DATA NOT EQUAL.");
				return false;
			}
		}
		
		System.out.println("NODE PATHS ARE EQUAL");
		return true;		
	}
	
	/**
	 * Calculates the cost of traversing the path based on the cost of individual vertices within this path
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateCost(){
		int cost=0;
		for(PathObject vertex_obj : this.getPath()){
			cost += ((IObjectValuationAccessor)vertex_obj.data()).getCost();
		}
		
		return cost;
	}
	
	/**
	 * Gets the estimated reward value for this path 
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateReward(){
		int reward = 0;
		for(PathObject vertex_obj : this.getPath()){
			reward += ((IObjectValuationAccessor)vertex_obj.data()).getReward();
		}
		
		return reward;
	}
	
	/**
	 * Clone {@link Path} object
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		for(PathObject obj : path.getPath()){
			clonePath.add(obj);
		}
		
		return clonePath;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * 
	 * @return
	 */
	public Page getLastPage(){
		for(int i = this.path.size()-1; i >= 0; i--){
			PathObject descNode = this.path.get(i);
			if(descNode.data() instanceof Page){
				return (Page)descNode.data();
			}
		}
		return null;
	}
	
	/**
	 * Produces all possible element, action combinations that can be produced from the given path
	 * 
	 * @throws MalformedURLException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static ArrayList<Path> expandPath(Path path)  {
		log.info( " EXPANDING PATH...");
		ArrayList<Path> pathList = new ArrayList<Path>();
		//Path new_path = Path.clone(path);
		
		Page page = path.getLastPage();
		if(page == null){
			return null;
		}
		//get last page
		String[] actions = ActionFactory.getActions();
		
		//get all elements for this page
		WebDriver webdriver = null;
		try {
			webdriver = new Browser(page.getUrl().toString()).getDriver();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<PageElement> page_elements = Page.getVisibleElements(webdriver, "");
	
		//iterate over all elements
		for(PageElement page_element : page_elements){
			//iterate over all actions
			for(String action : actions){
				Path action_path = Path.clone(path);
				Action action_obj = new Action(action);
								
				action_path.add(page_element);
				action_path.add(action_obj);
				pathList.add(action_path);
			}			
		}
		
		return pathList;
	}
	
	/**
	 * Checks if the path has 2 sequential elements that appear in more than 1 location
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(Path path){
		if(path.getPath().size() == 1){
			return false;
		}
		
		for(int i = path.getPath().size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().get(i).equals(path.getPath().get(j)) 
					&& path.getPath().get(i-1).equals(path.getPath().get(j-1))){
					return true;
				}
			}			
		}
		return false;
	}
	
	/**
	 * Checks if the path has the same page more than once. 
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasPageCycle(Path path){
		for(int i = path.getPath().size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().get(i).data() instanceof Page 
						&& path.getPath().get(j).data() instanceof Page
						&& path.getPath().get(i).data().equals(path.getPath().get(j).data()))
				{
					return true;
				}
			}			
		}
		return false;
	}

	public boolean isSpansMultipleDomains() {
		return spansMultipleDomains;
	}

	public void setSpansMultipleDomains(boolean spansMultipleDomains) {
		this.spansMultipleDomains = spansMultipleDomains;
	}
}
