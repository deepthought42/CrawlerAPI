package com.qanairy.models.rules;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.Rule;

public class Clickable implements Rule {

	@Override
	public RuleType getType() {
		return RuleType.CLICKABLE;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public Boolean evaluate(PageElement val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}

}
