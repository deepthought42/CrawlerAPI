package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.persistence.Page;
import com.qanairy.persistence.Path;
import com.qanairy.persistence.PathObject;

/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class PathPOJO extends Path {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Path.class);
	
    private String key;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	private List<PathObject> path = null;

	private PathObject path_entry_obj;
	
	/**
	 * Creates new instance of Path
	 */
	public PathPOJO(){
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
	public PathPOJO(List<PathObject> current_path){
		this.isUseful = false;
		this.path = current_path;
		this.key = null;
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public PathPOJO(String key, boolean isUseful, boolean spansMultipleDomains, List<PathObject> current_path){
		this.isUseful = isUseful;
		this.path = current_path;
		this.key = key;
		this.spansMultipleDomains = spansMultipleDomains;
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

	public Page firstPage() {
		for(PathObject obj : this.getPath()){
			if(obj instanceof Page){
				return (Page)obj;
			}
		}
		return null;
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
		Path clonePath = new PathPOJO();
		
		List<PathObject> path_obj = path.getPath();
		List<PathObject> clone_list = new ArrayList<PathObject>();
		for(PathObject obj : path_obj){
			clone_list.add(obj);
		}
		
		clonePath.setPath(clone_list);
		clonePath.setKey(path.getKey());
		clonePath.setIsUseful(path.isUseful());
		clonePath.setSpansMultipleDomains(path.doesSpanMultipleDomains());
		
		return clonePath;
	}
	
	public boolean doesSpanMultipleDomains() {
		return spansMultipleDomains;
	}
	
	public void setSpansMultipleDomains(boolean isSpanningMultipleDomains) {
		this.spansMultipleDomains= isSpanningMultipleDomains;
	}
	
	public void setKey(String key) {
		this.key = key;
		
	}
	
	public String getKey() {
		return this.key;
	}

	@Override
	public void setPathStartsWith(PathObject path_obj) {
		this.path_entry_obj = path_obj;
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
	
	public void setIsUseful(Boolean isUseful){
		this.isUseful = isUseful;
	}
	
	public Boolean isUseful(){
		return this.isUseful;
	}
	
	public int size(){
		return this.getPath().size();
	}

	@Override
	public PathObject getPathStartsWith() {
		return this.path_entry_obj;
	}
}


