package com.qanairy.persistence;

import com.qanairy.persistence.edges.IPathEdge;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;

/**
 * 
 */
public interface IPathObject {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("type")
	public String getType();
	
	@Property("type")
	public void setType(String type);
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public Iterable<IPathEdge> getPathEdges();
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public IPathEdge addPathEdge(IPathObject path_obj);
	
	@GremlinGroovy("it.out('goes_to').dedup")
	public Iterable<IPathObject> getNextPathObject();
}
