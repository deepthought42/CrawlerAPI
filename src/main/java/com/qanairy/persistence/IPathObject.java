package com.qanairy.persistence;

import java.util.List;

import com.qanairy.persistence.edges.IPathEdge;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;


public interface IPathObject {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("type")
	public String getType();
	
	@Property("type")
	public void setType(String type);
	
	@Adjacency(label="goes_to")
	public Iterable<IPathObject> getInitialPathObject();
	
	@Adjacency(label="goes_to")
	public void addInitialPathObject(IPathObject path_obj);
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public Iterable<IPathEdge> getPathEdges();
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public IPathEdge addPathEdge(IPathObject path_obj);
}
