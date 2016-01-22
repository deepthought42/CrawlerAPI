package structs;

import java.util.ArrayList;

import browsing.ActionFactory;
import browsing.IBrowserObject;
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
	public double reward = 0.0;
	private double cost = 0.0;
	private ArrayList<IBrowserObject> vertexPath = null;
	
	/**
	 * 
	 */
	public Path(){
		this.vertexPath = new ArrayList<IBrowserObject>();
	}

	/**
	 * 
	 * @param current_path
	 */
	public Path(Path current_path){
		this.vertexPath = new  ArrayList<IBrowserObject>();
		this.append(current_path);
	}
	
	/**
	 * 
	 * @return
	 */
	public void append(Path appendablePath){
		
		for(IBrowserObject obj : appendablePath.getPath()){
			this.vertexPath.add(obj);
		}				
	}
		
	public boolean add(IBrowserObject obj){
		return this.vertexPath.add(obj);
	}
	/**
	 * 
	 * @return
	 */
	public ArrayList<IBrowserObject> getPath(){
		return this.vertexPath;
	}
	
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
		for(IBrowserObject vertex_obj : this.getPath()){
			this.cost += vertex_obj.getCost();
		}
		return this.cost;
	}
	
	/**
	 * Gets the estimated reward value for this path 
	 * @param graph
	 * @return
	 */
	public double calculateReward(){
		this.reward = 0;
		for(IBrowserObject vertex_obj : this.getPath()){
			this.reward += vertex_obj.getReward();
		}
		
		return reward;
	}
	
	/**
	 * Clone {@link Path} object
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		for(IBrowserObject obj : path.getPath()){
			clonePath.add(obj);
		}
		
		return clonePath;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * @return
	 */
	public IBrowserObject getLastPageVertex(){
		for(int i = this.vertexPath.size()-1; i >= 0; i--){
			IBrowserObject descNode = this.vertexPath.get(i);
			if(descNode instanceof Page){
				System.err.println("PAGE VERTEX FOUND AND RETURNED");
				return descNode;
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
		System.out.println( " EXPANDING PATH...");
		ArrayList<Path> pathList = new ArrayList<Path>();
		Path new_path = Path.clone(path);
		
		IBrowserObject page_vertex = path.getLastPageVertex();
		if(page_vertex == null){
			return null;
		}
		//get last page
		Class<?> className = page_vertex.getClass();
		String[] actions = ActionFactory.getActions();
		
		if(className.equals(Page.class)){
			Page page = ((Page)page_vertex);
		
			//get all elements for this page
			ArrayList<PageElement> page_elements = page.getElements();
		
			//iterate over all elements
			for(PageElement page_element : page_elements){
				//System.err.println("Page element index "+page_elem_vertex_idx);
				new_path.add(page_element);
				
				//for each element in elements iterate over actions
				for(String action : actions){
					Path action_path = Path.clone(new_path);
					IBrowserObject action_obj = new Action(action);
					action_path.add(action_obj);
					pathList.add(action_path);
				}
				
				//clone path and add in action and element
				new_path = Path.clone(path);
			}
		}
		
		return pathList;
	}
}
