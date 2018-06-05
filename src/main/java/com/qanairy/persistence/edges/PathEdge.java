package com.qanairy.persistence.edges;

import com.qanairy.persistence.PathObject;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.annotations.InVertex;
import com.syncleus.ferma.annotations.OutVertex;
import com.syncleus.ferma.annotations.Property;

/**
 * 
 *
 */
public abstract class PathEdge extends AbstractEdgeFrame {
    @Property("pathKey")
    public abstract String getPathKey();
    
    @Property("pathKey")
    public abstract void setPathKey(String pathKey);

    @Property("transition_index")
    public abstract Integer getTransitionIndex();
    
    @Property("transition_index")
    public abstract void setTransitionIndex(int idx);
    
    @OutVertex
    public abstract PathObject getPathObjectOut();

    @InVertex
    public abstract PathObject getPathObjectIn();
}
