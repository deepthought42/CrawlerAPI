package com.minion.persistence.edges;

import com.minion.persistence.IPathObject;
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

    @OutVertex
    IPathObject getPathObjectOut();

    @InVertex
    IPathObject getPathProjectIn();
}
