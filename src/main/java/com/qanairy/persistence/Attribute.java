package com.qanairy.persistence;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.qanairy.persistence.serializers.AttributeSerializer;
import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Property;

/**
 * Represents {@link Attribute} to be stored in database
 */
@JsonSerialize(using = AttributeSerializer.class)
public abstract class Attribute extends AbstractVertexFrame implements Persistable {
	@Property("key")
	public abstract String getKey();
	
	@Property("key")
	public abstract void setKey(String key);

	@Property("name")
	public abstract void setName(String name);
	
	@Property("name")
	public abstract String getName();
	
	@Property("vals")
	public abstract List<String> getVals();
	
	@Property("vals")
	public abstract void setVals(List<String> vals);
}
