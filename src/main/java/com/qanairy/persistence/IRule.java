package com.qanairy.persistence;

import com.qanairy.rules.RuleType;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("Rule") 
public interface IRule {

	@Property("key")
	void setKey(RuleType type);
	
	@Property("type")
	RuleType getType();

	@Property("type")
	void setType(RuleType type);
	
	@Property("value")
	String getValue();

	@Property("value")
	void setValue(String value);
}
