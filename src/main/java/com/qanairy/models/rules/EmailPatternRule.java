package com.qanairy.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;

@NodeEntity
public class EmailPatternRule extends Rule {

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private RuleType type;
	private String value;
	
	public EmailPatternRule() {
		setType(RuleType.EMAIL_PATTERN);
		setValue("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
		setKey(super.generateKey());
	}

	@Override
	public Boolean evaluate(ElementState page_element) {
		for(Attribute attribute: page_element.getAttributes()){
			if(attribute.getName().equals("vals")){
				String pattern = "/^" + attribute.getVals().toString() + " $/";
				Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
			    return matcher.matches();
			}
		}
		return null;
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
	public RuleType getType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getValue() {
		return this.value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}
}
