package com.qanairy.persistence.edges;

import com.qanairy.persistence.IPathObject;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;
import com.tinkerpop.frames.Property;

/**
 * 
 * @author Brandon Kindred
 *
 */
public interface IPathEdge {
    @Property("pathKey")
    public String getPathKey();
    
    @Property("pathKey")
    public void setPathKey(String pathKey);

    @Property("transition_index")
    public Integer getTransitionIndex();
    
    @Property("transition_index")
    public void setTransitionIndex(int idx);
    @OutVertex
    IPathObject getPathObjectOut();

    @InVertex
    IPathObject getPathObjectIn();
}
