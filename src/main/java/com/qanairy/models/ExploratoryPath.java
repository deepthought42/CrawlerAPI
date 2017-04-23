package com.qanairy.models;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.BrowserActor;
import com.minion.browsing.ActionFactory;
import com.minion.browsing.IObjectValuationAccessor;
import com.minion.persistence.edges.IPathEdge;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.OrientConnectionFactory;


/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class ExploratoryPath extends Path{
    private static final Logger log = LoggerFactory.getLogger(BrowserActor.class);
	
    private String key;
	private boolean isUseful;
	private boolean spansMultipleDomains = false;
	
	private List<PathObject> path = null;
	private List<Action> possible_actions = null;
	
	/**
	 * Creates new instance of Path
	 */
	public ExploratoryPath(){
		this.isUseful = false;
		this.spansMultipleDomains = false;
		this.path = new ArrayList<PathObject>();
		this.setPossibleActions(new ArrayList<?>());
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public ExploratoryPath(List<PathObject> current_path, List<Action> actions){
		this.isUseful = false;
		this.path = current_path;
		this.setPossibleActions(actions);
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
	
	public boolean getIsUseful(){
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
			if(!obj.getClass().getCanonicalName().equals(comparatorPathNode.getClass().getCanonicalName())){
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
			PathObject path_obj_clone = obj.clone();
			clone_list.add(path_obj_clone);
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
		log.info("getting last page");
		List<PathObject> path_obj_list = this.getPath();
		Page page = null;

		for(PathObject obj : path_obj_list){
			if(obj instanceof Page){
				log.info("last page acquired");
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
		
		List<PathObject> path_obj_list = path.getPath();
		Page page = null;
		for(PathObject path_obj : path_obj_list){

			for(PathObject path_obj2 : path_obj_list){

				if(path_obj	 instanceof Page){
					log.info("last page acquired");
					page = (Page)path_obj;
					
					if(path_obj.equals(path)){
						return true;
					}
				}
				path_obj = path_obj.getNext();
			}while(path_obj.getNext() != null);
		}
		for(int i = path.size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.getPath().equals(path.getPath()) 
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
		log.info("checking path for domains :: "+path.getClass().getName());
		log.info("Last page url :: " + this.findLastPage().getUrl());
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

	
	
	public String getKey() {
		return this.key;
	}

	

	public Page getFirstPage() {
		
		for(PathObject obj : this.getPath()){
			if(obj instanceof Page){
				return (Page)obj;
			}
		}
		return null;
	}

	public List<Action> getPossibleActions() {
		return possible_actions;
	}

	public void setPossibleActions(List<Action> possible_actions) {
		this.possible_actions = possible_actions;
	}
}


