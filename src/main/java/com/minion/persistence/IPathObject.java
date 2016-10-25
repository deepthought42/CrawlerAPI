package com.minion.persistence;

import java.util.List;

import com.minion.persistence.edges.IPathEdge;
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
	public IPathObject getNext();
	
	@Adjacency(label="goes_to")
	public void setNext(IPathObject path_obj);
	
	@Adjacency(label="goes_to")
	public void setNext(List<IPathObject> path_obj);
	
	@Incidence(label="goes_to")
	public Iterable<IPathEdge> getPathEdges();
	
	@Incidence(label="goes_to")
	public IPathEdge addPathEdge(IPathObject path_obj);
}
