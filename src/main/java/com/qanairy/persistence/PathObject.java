package com.qanairy.persistence;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Direction;
import com.qanairy.persistence.edges.PathEdge;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.Incidence;
import com.syncleus.ferma.annotations.Property;

/**
 * 
 */
public interface PathObject extends VertexFrame {
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
	@Property("type")
	public String getType();
	
	@Property("type")
	public void setType(String type);
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public List<? extends PathEdge> getPathEdges();
	
	@Incidence(direction=Direction.OUT, label="goes_to")
	public boolean addPathEdge(PathObject path_obj)
}
