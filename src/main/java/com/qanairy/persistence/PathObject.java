package com.qanairy.persistence;

import com.syncleus.ferma.VertexFrame;
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
}
