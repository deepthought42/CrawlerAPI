package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.models.Page;
import com.qanairy.models.PathObject;


/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class Path {
	private static Logger log = LoggerFactory.getLogger(Path.class);
	
    private String key;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	private List<PathObject> path = null;
	
	/**
	 * Creates new instance of Path
	 */
	public Path(){
		this.isUseful = false;
		this.spansMultipleDomains = false;
		this.path = new ArrayList<PathObject>();
		this.key = null;
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(List<PathObject> current_path){
		this.isUseful = false;
		this.path = current_path;
		this.key = null;
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public Path(String key, boolean isUseful, boolean spansMultipleDomains, List<PathObject> current_path){
		this.isUseful = isUseful;
		this.path = current_path;
		this.key = key;
		this.spansMultipleDomains = spansMultipleDomains;
	}
		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject obj){
		return this.getPath().add(obj);
	}
	
	/**
	 * @return The {@link List} of {@link PathObject}s that comprise a path
	 */
	public List<PathObject> getPath(){
		return this.path;
	}
	
	public void setPath( List<PathObject> path){
		this.path = path;
	}
	
	public void setIsUseful(boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public boolean isUseful(){
		return this.isUseful;
	}
	
	public int size(){
		return this.getPath().size();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Path path){
		int thisPathLength = this.size();
		int comparatorPathLength = path.size();
				
		if(thisPathLength != comparatorPathLength){
			return false;
		}
		
		List<PathObject> comparatorPathNode = path.getPath();
		for(PathObject obj : this.getPath()){
			if(!obj.getClass().getSimpleName().equals(comparatorPathNode.getClass().getSimpleName())){
				return false;
			}
			if(!obj.equals(comparatorPathNode)){
				return false;
			}
		}
		
		return true;		
	}
	
	/**
	 * Clone {@link Path} object
	 * 
	 * @param path
	 * @return
	 */
	public static Path clone(Path path){
		Path clonePath = new Path();
		
		List<PathObject> path_obj = path.getPath();
		List<PathObject> clone_list = new ArrayList<PathObject>();
		for(PathObject obj : path_obj){
			//PathObject path_obj_clone = obj.clone();
			clone_list.add(obj);
		}
		
		clonePath.setPath(clone_list);
		clonePath.setKey(path.getKey());
		clonePath.setIsUseful(path.isUseful());
		clonePath.setSpansMultipleDomains(path.getSpansMultipleDomains());
		
		return clonePath;
	}
	
	/**
	 * Gets the last Vertex in a path that is of type {@link Page}
	 * 
	 * @return
	 */
	public Page findLastPage(){
		List<PathObject> path_obj_list = this.getPath();
		Page page = null;

		for(PathObject obj : path_obj_list){
			if(obj != null && obj.getType().equals("Page")){
				page = (Page)obj;
			}
		}

		return page;
	}
	
	/**
	 * Checks if the path has 2 sequential elements that appear in more than 1 location
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(Path path){
		if(path.size() == 1){
			return false;
		}
		
		//extract all pages
		//iterate through pages to see if any match
		List<Page> page_list = new ArrayList<Page>();
		List<PathObject> path_obj_list = path.getPath();
		Page page = null;
		for(PathObject path_obj : path_obj_list){
			if(path_obj instanceof Page){
				page_list.add((Page)path_obj);
			}
		}
		
		boolean cycle_exists = false;
		for(int first_page_idx =0; first_page_idx < page_list.size()-1 && !cycle_exists; first_page_idx++){
			for(int second_page_idx =1; second_page_idx < page_list.size() && !cycle_exists; first_page_idx++){
				if(page_list.get(first_page_idx).equals(page_list.get(second_page_idx))){
					cycle_exists = true;
					break;
				}
			}	
		}

		return cycle_exists;
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
				if(path.getPath().get(i) instanceof Page 
						&& path.getPath().get(j) instanceof Page
						&& path.getPath().get(i).equals(path.getPath().get(j)))
				{
					return true;
				}
			}			
		}
		return false;
	}

	public boolean getSpansMultipleDomains() {
		return spansMultipleDomains;
	}
	
	public void setSpansMultipleDomains(boolean isSpanningMultipleDomains) {
		this.spansMultipleDomains= isSpanningMultipleDomains;
	}

	public boolean checkIfSpansMultipleDomains() {
		String domain = "";
		
		//iterate over path
		List<PathObject> path_obj_list = this.getPath();
		
		for(PathObject obj : path_obj_list){
			if(obj instanceof Page){
				Page page = (Page)obj;
				String curr_domain = page.getUrl().toString();
				if(domain.isEmpty()){
					domain = curr_domain;
				}
				else if(!domain.equals(curr_domain)){
					return true;
				}
			}
		}
		
		//if path domains change then return true
		return false;
	}

		
	public void setKey(String key) {
		this.key = key;
		
	}
	
	public String getKey() {
		return this.key;
	}

	
	public Page firstPage() {
		
		for(PathObject obj : this.getPath()){
			if(obj instanceof Page){
				return (Page)obj;
			}
		}
		return null;
	}
}


