package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;

/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
@NodeEntity
public class PatternRule extends Rule {
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String value;
	private RuleType type;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(PageElement elem) {
		for(Attribute attribute: elem.getAttributes()){
			if(attribute.getName().equals("vals")){
				String pattern = "/^" + attribute.getVals().toString() + " $/";
				Matcher matcher = Pattern.compile(this.value).matcher(pattern);
			    return matcher.matches();
			}
		}
		return null;
	}
	
	public PatternRule(String pattern){
		this.value = pattern;
		setType(RuleType.PATTERN);
		setKey(super.generateKey());
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
		this.type = type;	
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public RuleType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValue() {
		return this.value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}

}
