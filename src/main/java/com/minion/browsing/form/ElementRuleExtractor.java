package com.minion.browsing.form;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.minion.browsing.Browser;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.rules.AlphabeticRestrictionRule;
import com.qanairy.models.rules.Clickable;
import com.qanairy.models.rules.DisabledRule;
import com.qanairy.models.rules.EmailPatternRule;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.PatternRule;
import com.qanairy.models.rules.ReadOnlyRule;
import com.qanairy.models.rules.RequirementRule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.models.rules.SpecialCharacterRestriction;
import com.qanairy.persistence.Rule;

public class ElementRuleExtractor {
	private static Logger log = LoggerFactory.getLogger(ElementRuleExtractor.class);

	public static List<Rule> extractInputRules(PageElement elem){
		List<Rule> rules = new ArrayList<Rule>();

		for(Attribute attr : elem.getAttributes()){
			if(attr.getName().trim().equalsIgnoreCase("required")){
				Rule required = new RequirementRule();
				rules.add(required);
			}
			else if(attr.getName().trim().equalsIgnoreCase("disabled")){
				Rule disabled = new DisabledRule();
				rules.add(disabled);
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.contains("number")){
				Rule alphabetic_restriction_rule = new AlphabeticRestrictionRule();
				Rule special_character_rule = new SpecialCharacterRestriction();
				
				rules.add(alphabetic_restriction_rule);
				rules.add(special_character_rule);
			}
			else if(attr.getName().equalsIgnoreCase("readonly")){
				rules.add(new ReadOnlyRule());
			}
			else if(attr.getName().equalsIgnoreCase("min")){
				Rule min_val = new NumericRule(RuleType.MIN_VALUE, attr.getVals().get(0));
				rules.add(min_val);
			}
			else if(attr.getName().equalsIgnoreCase("max")){
				Rule max_val = new NumericRule(RuleType.MAX_VALUE, attr.getVals().get(0));
				rules.add(max_val);
			}
			//minlength only works for certain frameworks such as angularjs that support it as a custom html5 attribute
			else if(attr.getName().equalsIgnoreCase("minlength")){
				NumericRule min_length = new NumericRule(RuleType.MIN_LENGTH, attr.getVals().get(0));
				rules.add(min_length);
			}
			else if(attr.getName().equalsIgnoreCase("maxlength")){
				NumericRule max_length = new NumericRule(RuleType.MAX_LENGTH, attr.getVals().get(0));
				rules.add(max_length);
			}
			else if(attr.getName().equalsIgnoreCase("type") && attr.getVals().get(0).equalsIgnoreCase("email")){
				EmailPatternRule email_rule = new EmailPatternRule();
				rules.add(email_rule);
			}
			else if(attr.getName().equalsIgnoreCase("pattern")){
				String regex_str = attr.getVals().get(0);
				PatternRule pattern_rule = new PatternRule(regex_str);
				rules.add(pattern_rule);
			}
		}
		
		return rules;
	}

	public static List<Rule> extractMouseRules(PageElement page_element) {
		List<Rule> rules = new ArrayList<Rule>();

		//iterate over possible mouse actions. 
		//if an element action interaction causes change
			//then add the appropriate rule to the list
		Rule clickable = new Clickable();
		rules.add(clickable);
		return rules;
	}
}
