package com.qanairy.models.rules;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.PageElement;

@NodeEntity
public class Clickable extends Rule {
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private RuleType type;
	private String value;
	
	public Clickable(){
		setType(RuleType.CLICKABLE);
		setValue("");
		setKey(generateKey());
	}
	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setType(RuleType type) {
		this.type = RuleType.CLICKABLE;
	}
	
	@Override
	public RuleType getType() {
		return RuleType.CLICKABLE;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public Boolean evaluate(PageElement val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}
}
