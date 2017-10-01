package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.rules.FormRule;
import com.qanairy.rules.formRules.AlphabeticRestrictionRule;
import com.qanairy.rules.formRules.DisabledRule;
import com.qanairy.rules.formRules.FormRuleType;
import com.qanairy.rules.formRules.NumericRule;
import com.qanairy.rules.formRules.PatternRule;
import com.qanairy.rules.formRules.ReadOnlyRule;
import com.qanairy.rules.formRules.RequirementRule;
import com.qanairy.rules.formRules.SpecialCharacterRestriction;

public class ElementRuleExtractor {
	private static Logger log = LogManager.getLogger(ElementRuleExtractor.class);

	public static List<FormRule> extractRules(PageElement elem){
		List<FormRule> rules = new ArrayList<FormRule>();

		for(Attribute attr : elem.getAttributes()){
			if(attr.getName().trim().equalsIgnoreCase("required")){
				FormRule required = new RequirementRule();
				rules.add(required);
				System.out.println("Form field is required : "+rules.size());
			}
			else if(attr.getName().trim().equalsIgnoreCase("disabled")){
				FormRule disabled = new DisabledRule();
				rules.add(disabled);
				System.out.println("Field is disabled");
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.contains("number")){
				FormRule alphabetic_restriction_rule = new AlphabeticRestrictionRule();
				FormRule special_character_rule = new SpecialCharacterRestriction();
				
				rules.add(alphabetic_restriction_rule);
				rules.add(special_character_rule);
				System.out.println("form input is of number type. Numbers only.");
			}
			else if(attr.getName().equalsIgnoreCase("readonly")){
				rules.add(new ReadOnlyRule());
				System.out.println("Form field is read only");
			}
			else if(attr.getName().equalsIgnoreCase("min")){
				FormRule min_val = new NumericRule(FormRuleType.MIN_VALUE,Integer.parseInt(attr.getVals().get(0)));
				rules.add(min_val);
				System.out.println("form field has a minimum value of : " + attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("max")){
				FormRule max_val = new NumericRule(FormRuleType.MAX_VALUE, Integer.parseInt(attr.getVals().get(0)));
				rules.add(max_val);
				System.out.println("form field has a maximum value of : " + attr.getVals().get(0));
			}
			//minlength only works for certain frameworks such as angularjs that support it as a custom html5 attribute
			else if(attr.getName().equalsIgnoreCase("minlength")){
				NumericRule min_length = new NumericRule(FormRuleType.MIN_LENGTH, Integer.parseInt(attr.getVals().get(0)));
				rules.add(min_length);
				System.out.println("form field has a minimum length of : " + attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("maxlength")){
				NumericRule max_length = new NumericRule(FormRuleType.MAX_LENGTH, Integer.parseInt(attr.getVals().get(0)));
				rules.add(max_length);
				System.out.println("form field has a maximum length of : " + attr.getVals().get(0));
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().get(0).equalsIgnoreCase("email")){
				String email_regex_str = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
				PatternRule email_rule = new PatternRule(Pattern.compile(email_regex_str, Pattern.CASE_INSENSITIVE));
				rules.add(email_rule);
			}
			else if(attr.getName().equalsIgnoreCase("pattern")){
				String regex_str = attr.getVals().get(0);
				PatternRule pattern_rule = new PatternRule(Pattern.compile(regex_str));
				rules.add(pattern_rule);
			}
		}
		
		return rules;
	}
}
