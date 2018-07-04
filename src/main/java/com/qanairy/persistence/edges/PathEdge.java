package com.qanairy.persistence.edges;

import com.qanairy.models.PathObject;

/**
 * 
 *
 */
public abstract class PathEdge {
    public abstract String getPathKey();
    
    public abstract void setPathKey(String pathKey);

    public abstract Integer getTransitionIndex();
    
    public abstract void setTransitionIndex(int idx);
    
    public abstract PathObject getPathObjectOut();

    public abstract PathObject getPathObjectIn();
}
