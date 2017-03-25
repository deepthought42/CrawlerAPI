package com.qanairy.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object wrapper that allows data to be dynamically placed in data structures
 * 
 *
 */
public abstract class PathObject{
    private static final Logger log = LoggerFactory.getLogger(PathObject.class);
    private String type = null;
    
    public String getType(){
    	return this.type;
    }
    
    /**
     * Sets type to the classname passed. System generally expects classname to be simpleClassName()
     * @param classname
     */
    public void setType(String classname){
    	this.type = classname;
    }
	
	public abstract PathObject clone();
}
