package structs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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
	
	public double reward = 0.0;
	public double cost = 0.0;
	private Boolean isUseful;

	public ArrayList<PathObject> vertexPath = null;
	
	/**
	 * Creates new instance of Path
	 */
	public Path(){
		this.isUseful = null;
		this.vertexPath = new ArrayList<PathObject>();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.isUseful = null;
		this.vertexPath = new  ArrayList<PathObject>();
		this.append(current_path);
	}
	
	/**
	 * 
	 * @return
	 */
	public void append(Path appendablePath){
		
		for(PathObject obj : appendablePath.getPath()){
			this.vertexPath.add(obj);
		}				
	}
		
	public boolean add(PathObject obj){
		return this.vertexPath.add(obj);
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<PathObject> getPath(){
		return this.vertexPath;
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
		int thisPathLength = this.vertexPath.size();
		int comparatorPathLength = path.getPath().size();
				
		if(thisPathLength != comparatorPathLength){
			System.out.println("PATHS ARE NOT EQUAL");
			return false;
		}
		for(int i = 0; i < thisPathLength; i++){
			Object thisPathNode = this.vertexPath.get(i);
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

	public double getCost(){
		return this.cost;
	}
	
	public double getReward(){
		return this.reward;
	}
	
	/**
	 * Calculates the cost of traversing the path based on the cost of individual vertices within this path
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateCost(){
		this.cost=0;
		for(PathObject vertex_obj : this.getPath()){
			this.cost += ((IObjectValuationAccessor)vertex_obj.getData()).getCost();
		}
		return this.cost;
	}
	
	/**
	 * Gets the estimated reward value for this path 
	 * 
	 * @param graph
	 * @return
	 */
	public double calculateReward(){
		this.reward = 0;
		for(PathObject vertex_obj : this.getPath()){
			this.reward += ((IObjectValuationAccessor)vertex_obj.getData()).getReward();
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
	public Page getLastPageVertex(){
		for(int i = this.vertexPath.size()-1; i >= 0; i--){
			PathObject descNode = this.vertexPath.get(i);
			if(descNode.getData() instanceof Page){
				return (Page)descNode.getData();
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
		System.err.println( " EXPANDING PATH...");
		ArrayList<Path> pathList = new ArrayList<Path>();
		Path new_path = Path.clone(path);
		
		Page page = path.getLastPageVertex();
		if(page == null){
			return null;
		}
		//get last page
		String[] actions = ActionFactory.getActions();
		
		//get all elements for this page
		WebDriver phantom_driver = Browser.openWithPhantomjs(page.getUrl().toString());
		List<PageElement> page_elements = Page.getVisibleElements(phantom_driver, "//");
	
		//iterate over all elements
		for(PageElement page_element : page_elements){
			//System.err.println("Page element index "+page_elem_vertex_idx);
			new_path.add(page_element);
			
			//for each element in elements iterate over actions
			for(String action : actions){
				Path action_path = Path.clone(new_path);
				Action action_obj = new Action(action);
				action_path.add(action_obj);
				pathList.add(action_path);
			}
			
			//clone path and add in action and element
			new_path = Path.clone(path);
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
				if(path.getPath().get(i).getData() instanceof Page 
						&& path.getPath().get(j).getData() instanceof Page
						&& path.getPath().get(i).getData().equals(path.getPath().get(j).getData()))
				{
					return true;
				}
			}			
		}
		return false;
	}
}
