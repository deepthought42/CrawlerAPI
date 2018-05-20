package com.qanairy.models;

import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.qanairy.persistence.Action;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;


/**
 * A set of vertex objects that form a sequential movement through a graph
 */
public class ExploratoryPath {
	private static Logger log = LoggerFactory.getLogger(ExploratoryPath.class);
	
	private List<PathObject> path_objects;
	private List<Action> possible_actions;
	
	/**
	 * Creates new instance of Path
	 */
	public ExploratoryPath(){
		super();
	}

	/**
	 * Creates new instance of path setting it to the given path
	 * 
	 * @param current_path
	 */
	public ExploratoryPath(List<PathObject> current_path, List<Action> actions){
		setPathObjects(current_path);
		this.setPossibleActions(actions);
	}

		
	/**
	 * Adds an object to path and sets whether or not this path spans multiple domains
	 * 
	 * @param obj
	 * @return
	 */
	public boolean add(PathObject obj){
		return getPathObjects().add(obj);
	}
	
	public int size(){
		return getPathObjects().size();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o){
		int thisPathLength = this.size();
		int comparatorPathLength = path.size();
				
		if(thisPathLength != comparatorPathLength){
			return false;
		}
		
		List<PathObject> comparatorPathNode = path.getPath();
		for(PathObject obj : getPathObjects()){
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
	 * Gets the last Vertex in a path that is of type {@link PageState}
	 * 
	 * @return
	 */
	public PageState findLastPage(){
		List<PathObject> path_obj_list = getPathObjects();
		PageState page = null;

		for(PathObject obj : path_obj_list){
			if(obj instanceof PageState){
				page = (PageState)obj;
			}
		}

		return page;
	}
	
	/**
	 * Checks if the path has 2 pages that are the equal
	 * 
	 * @param path
	 * @return true if sequence appears more than once
	 */
	public static boolean hasCycle(List<PathObject> path_obj_list, PageState page){
		if(path_obj_list.size() == 1){
			return false;
		}
		
		//extract all pages
		//iterate through pages to see if any match
		for(PathObject path_obj : path_obj_list){			
			if(path_obj instanceof PageState){
				if(((PageState)path_obj).equals(page)){
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
	public static boolean hasExistingElementActionSequence(ExploratoryPath path){
		if(path.size() == 1){
			return false;
		}
		
		//iterate through path
		//if path object is of type PageElement
		//	then load path object
		//		check if path object leads to an action that exists in the paths possible actions list
		//		If there exists an action that matches a possible action 
		//			then get next path object
		//				if path object is of type PageElement
		//					then load path object
		//						check if path object leads to an action that exists in the paths possible actions list
		for(PathObject path_obj : path.getPath()){
			if(path_obj instanceof PageElement){
				PageElementRepository page_elem_repo = new PageElementRepository();
				OrientConnectionFactory connection = new OrientConnectionFactory();
				PageElement page_elem = page_elem_repo.find(connection, page_elem_repo.generateKey((PageElement)path_obj));
				if(page_elem != null){
					List<Action> actions = path.getPossibleActions();
					IPageElement ipage_elem = page_elem_repo.save(connection, page_elem);
					Iterator<IPathEdge> path_edge_iter = ipage_elem.getPathEdges().iterator();
					while(path_edge_iter.hasNext()){
						IPathEdge edge = path_edge_iter.next();
						IPathObject path_object_out = edge.getPathObjectIn();
						PathObjectRepository path_obj_repo = new PathObjectRepository();
						PathObject new_path_obj = path_obj_repo.load(path_object_out);						
						if(new_path_obj.getType().equals("Action")){
							for(Action action : actions){
								if(((Action)new_path_obj).getName().equals(action.getName()) && ((Action)new_path_obj).getValue().equals(action.getValue())){
									return true;
								}
							}
						}
					}
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
	public static boolean hasPageCycle(List<PathObject> path){
		for(int i = path.size()-1; i > 0; i--){
			for(int j = i-1; j>= 0; j--){
				if(path.get(i) instanceof PageState 
						&& path.get(j) instanceof PageState
						&& path.get(i).equals(path.get(j)))
				{
					return true;
				}
			}			
		}
		return false;
	}

	public boolean checkIfSpansMultipleDomains() {
		log.debug("Last page url :: " + this.findLastPage().getUrl());
		String domain = "";
		
		//iterate over path
		List<PathObject> path_obj_list = getPathObjects();
		
		for(PathObject obj : path_obj_list){
			if(obj instanceof PageState){
				PageState page = (PageState)obj;
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

	public PageState firstPage() {
		
		for(PathObject obj : getPathObjects()){
			if(obj instanceof PageState){
				return (PageState)obj;
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

	public List<PathObject> getPathObjects() {
		return path_objects;
	}

	public void setPathObjects(List<PathObject> path_objects) {
		this.path_objects = path_objects;
	}
}


