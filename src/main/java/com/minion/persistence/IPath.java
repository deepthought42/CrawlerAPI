package com.minion.persistence;

import java.util.List;

import com.qanairy.models.PathObject;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Test object data access interface for use with tinkerpop/frames
 *
 */
@TypeValue("Path") public interface IPath{
	/**
	 * @return the key for the current test
	 */
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
		
	/**
	 * @return {@link Boolean} value indicating usefulness for a path.
	 * A value of null means that it's usefulness is unknown.
	 */
	@Property("useful")
	public Boolean isUseful();
	
	/**
	 * Sets the useful property to either true/false/null 
	 */
	@Property("useful")
	public void setUsefulness(Boolean isUseful);
	
	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public boolean isSpansMultipleDomains();

	/**
	 * @return whether or not this path goes into another domain
	 */
	@Property("spansMultipleDomains")
	public void setSpansMultipleDomains(boolean spanningMultipleDomains);

	/**
	 * @return {@link List} of {@link PathObject}s representing a path sequence
	 */
	@Adjacency(label="starts_with")
	public IPathObject getPath();

	/**
	 * Sets the {@link List} of {@link PathObject}s representing the path sequence
	 */
	@Adjacency(label="starts_with")
	public void setPath(IPathObject path_obj);
}