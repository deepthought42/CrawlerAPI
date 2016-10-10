package com.minion.persistence;

import com.minion.browsing.PathObject;
import com.tinkerpop.frames.Adjacency;

public interface IPathObject {
	//private String[] actions = ActionFactory.getActions();
	@Adjacency(label="consists_of")
	public IPathObject getData();

	@Adjacency(label="consists_of")
	public void setData(Object data);
	
	@Adjacency(label="goes_to")
	public PathObject<?> getNext();
	
	@Adjacency(label="goes_to")
	public void setNext(IPathObject path_obj);
	
}
