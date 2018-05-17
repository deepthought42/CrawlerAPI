package com.qanairy.models;

import java.util.List;

import com.qanairy.persistence.Action;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.edges.PathEdge;

/**
 * Defines an action in name only
 */
public class ActionPOJO extends Action{
	private String name;
	private String key;
	private String value;
	private String type;
	private List<PathEdge> edges;
	
	/**
	 * Construct empty action object
	 */
	public ActionPOJO(){
		setType("Action");
		this.name = null;
		this.key = null;
		this.value = "";
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public ActionPOJO(String action_name) {
		setType("Action");
		this.name = action_name;
		this.value = "";
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public ActionPOJO(String action_name, String value) {
		setType("Action");
		this.name = action_name;
		this.value = value;
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject clone() {
		Action action_clone = new ActionPOJO(this.getName(), this.getValue());
		return action_clone;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}


	@Override
	public List<PathEdge> getPathEdges(){
	    return this.edges;
	}
	
	@Override
	public boolean addPathEdge(PathObject path_obj){
		return this.edges.add(new PathEdgePOJO());
	}
}
