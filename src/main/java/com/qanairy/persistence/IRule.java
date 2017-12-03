package com.qanairy.persistence;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("Rule") 
public interface IRule {

	@Property("key")
	void setKey(String key);
	
	@Property("rule_type")
	String getType();

	@Property("rule_type")
	void setType(String type);
	
	@Property("value")
	String getValue();

	@Property("value")
	void setValue(String value);
}
