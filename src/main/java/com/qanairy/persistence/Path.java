package com.qanairy.persistence;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Direction;

import com.qanairy.persistence.edges.PathEdge;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.Incidence;
import com.syncleus.ferma.annotations.Property;

/**
 * Test object data access interface for use with tinkerpop/frames
 *
 */
public abstract class Path extends AbstractVertexFrame {
	/**
	 * @return the key for the current test
	 */
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
		
	/**
	 * @return {@link Boolean} value indicating usefulness for a path.
	 * A value of null means that it's usefulness is unknown.
	 */
	@Property("useful")
	public abstract Boolean isUseful();
	
	/**
	 * Sets the useful property to either true/false/null 
	 */
	@Property("useful")
	public abstract void setIsUseful(Boolean isUseful);
	
	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public abstract boolean getIfSpansMultipleDomains();

	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public abstract void setSpansMultipleDomains(boolean spanningMultipleDomains);

	@Property("path_list")
	public abstract void setPath(List<String> path_obj_key_list);
	
	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Property("path_list")
	public abstract List<String> getPath();
	
	@Property("path_list")
	public abstract boolean addToPath(String key);
	
	@Adjacency(label="contains")
	public abstract void addPathObject(PathObject path_obj);
	
	@Adjacency(label="contains")
	public abstract List<? extends PathObject> getPathObjects();

}