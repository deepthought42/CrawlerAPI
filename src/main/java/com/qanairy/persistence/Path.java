package com.qanairy.persistence;

import java.util.List;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
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
	public abstract boolean doesSpanMultipleDomains();

	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public abstract void setSpansMultipleDomains(boolean spanningMultipleDomains);

	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="starts_with")
	public abstract PathObject getPathStartsWith();

	/**
	 * Sets the {@link List} of {@link PathObject}s representing the path sequence
	 */
	@Adjacency(label="starts_with")
	public abstract void setPathStartsWith(PathObject path_obj);
	
	public abstract void setPath(List<PathObject> path_obj_list);
	
	public abstract List<PathObject> getPath();
	
	public abstract int size();
}