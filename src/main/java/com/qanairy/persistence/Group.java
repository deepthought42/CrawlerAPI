package com.qanairy.persistence;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

/**
 * Represents a {@link Group} to be stored in orientDB database
 */
public abstract class Group extends AbstractVertexFrame implements Persistable{
	@Property("name")
	public abstract String getName();
	
	@Property("name")
	public abstract void setName(String name);
	
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);

	@Property("description")
	public abstract void setDescription(String description);
	
	@Property("description")
	public abstract String getDescription();
}
