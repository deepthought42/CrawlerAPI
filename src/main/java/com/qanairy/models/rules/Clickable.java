package com.qanairy.models.rules;

import com.qanairy.models.ElementState;

public class Clickable extends Rule {	
	public Clickable(){
		setType(RuleType.CLICKABLE);
		setValue("");
		setKey(generateKey());
	}

	@Override
	public Boolean evaluate(ElementState val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}
}
