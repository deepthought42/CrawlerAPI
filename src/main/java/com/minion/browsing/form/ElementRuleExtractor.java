package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.rules.BooleanRule;
import com.qanairy.rules.BooleanRuleType;
import com.qanairy.rules.NumericRule;
import com.qanairy.rules.NumericRuleType;
import com.qanairy.rules.PatternRule;
import com.qanairy.rules.PatternRuleType;
import com.qanairy.rules.Rule;

public class ElementRuleExtractor {
    private static final Logger log = LoggerFactory.getLogger(ElementRuleExtractor.class);

	public static List<Rule<?,?>> extractRules(PageElement elem){
		List<Rule<?,?>> rules = new ArrayList<Rule<?,?>>();

		for(Attribute attr : elem.getAttributes()){
			if(attr.getName().equalsIgnoreCase("required")){
				BooleanRule required = new BooleanRule(BooleanRuleType.REQUIRED, true);
				rules.add(required);
				log.info("Form field is required");
			}
			else if(attr.getName().equalsIgnoreCase("disabled")){
				BooleanRule disabled = new BooleanRule(BooleanRuleType.ENABLED, false);
				rules.add(disabled);
				log.info("Field is disabled");
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().toString().contains("number")){
				BooleanRule number_only = new BooleanRule(BooleanRuleType.NUMBER_ONLY, true);
				rules.add(number_only);
				log.info("form input is of number type. Numbers only.");
			}
			else if(attr.getName().equalsIgnoreCase("readonly")){
				BooleanRule read_only = new BooleanRule(BooleanRuleType.READ_ONLY, true);
				rules.add(read_only);
				log.info("Form field is read only");
			}
			else if(attr.getName().equalsIgnoreCase("min")){
				NumericRule min_val = new NumericRule(NumericRuleType.MIN_VALUE, Integer.parseInt(attr.getVals()[0]));
				rules.add(min_val);
				log.info("form field has a minimum value of : " + attr.getVals()[0]);
			}
			else if(attr.getName().equalsIgnoreCase("max")){
				NumericRule max_val = new NumericRule(NumericRuleType.MAX_VALUE, Integer.parseInt(attr.getVals()[0]));
				rules.add(max_val);
				log.info("form field has a maximum value of : " + attr.getVals()[0]);
			}
			else if(attr.getName().equalsIgnoreCase("minlength")){
				NumericRule max_val = new NumericRule(NumericRuleType.MIN_LENGTH, Integer.parseInt(attr.getVals()[0]));
				rules.add(max_val);
				log.info("form field has a minimum length of : " + attr.getVals()[0]);
			}
			else if(attr.getName().equalsIgnoreCase("maxlength")){
				NumericRule max_val = new NumericRule(NumericRuleType.MAX_LENGTH, Integer.parseInt(attr.getVals()[0]));
				rules.add(max_val);
				log.info("form field has a maximum length of : " + attr.getVals()[0]);
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals()[0].equalsIgnoreCase("email")){
				String email_regex_str = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
				PatternRule email_rule = new PatternRule(PatternRuleType.REGEX, Pattern.compile(email_regex_str, Pattern.CASE_INSENSITIVE));
				rules.add(email_rule);
			}
			else if(attr.getName().equalsIgnoreCase("formnovalidate")){
				BooleanRule no_validate = new BooleanRule(BooleanRuleType.NO_VALIDATE, true);
				rules.add(no_validate);
			}
		}
		
		return rules;
	}
}
