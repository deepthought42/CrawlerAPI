package com.qanairy.persistence;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

public abstract class Action extends AbstractVertexFrame implements PathObject, Persistable{
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);
	
	@Property("name")
	public abstract String getName();
	
	@Property("name")
	public abstract void setName(String name);
	
	@Property("value")
	public abstract String getValue();
	
	@Property("value")
	public abstract void setValue(String value);
}
