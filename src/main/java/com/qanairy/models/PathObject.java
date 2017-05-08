package com.qanairy.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An object wrapper that allows data to be dynamically placed in data structures
 * 
 *
 */
public abstract class PathObject{
    private static Logger log = LogManager.getLogger(PathObject.class);
    private String type = null;
    
    public String getType(){
    	return this.type;
    }
    
    /**
     * Sets type to the classname passed. System generally expects classname to be simpleClassName()
     * @param classname
     */
    public void setType(String type){
    	this.type = type;
    }
	
	public abstract PathObject clone();
}
