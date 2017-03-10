package com.qanairy.rules.formRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minion.browsing.form.FormField;
import com.qanairy.rules.FormRule;
import com.qanairy.rules.RuleType;

public class SpecialCharacterRestriction implements FormRule{

	@Override
	public RuleType getType() {
		return FormRuleType.SPECIAL_CHARACTER_RESTRICTION;
	}

	@Override
	public Boolean evaluate(FormField field) {
		Pattern pattern = Pattern.compile("[0-9]*");

        Matcher matcher = pattern.matcher(field.getInputElement().getText());
		return matcher.matches();
	}

}
