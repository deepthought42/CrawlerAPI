package com.qanairy.models.rules;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.ElementState;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
@NodeEntity
public class ReadOnlyRule extends Rule {
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private RuleType type;
	private String value;


	public ReadOnlyRule(){
		setValue(null);
		setType(RuleType.READ_ONLY);
		setKey(super.generateKey());
	}

	
	@Override
	public Boolean evaluate(ElementState elem) {
		//Check if field is read-only
		return elem.getAttributes().contains("readonly");
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
	public RuleType getType() {
		return this.type;
	}


	@Override
	public void setType(RuleType type) {
		this.type = type;
	}


	@Override
	public String getValue() {
		return this.value;
	}


	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	
}
