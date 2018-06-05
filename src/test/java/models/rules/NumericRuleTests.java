package models.rules;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.dao.RuleDao;
import com.qanairy.models.dao.impl.RuleDaoImpl;
import com.qanairy.models.rules.NumericRule;
import com.qanairy.models.rules.RuleType;
import com.qanairy.persistence.Rule;

/**
 * 
 */
public class NumericRuleTests {

	@Test(groups="Regression")
	public void assertNumericRulePersists(){
		Rule rule = new NumericRule(RuleType.MAX_LENGTH, null);
		RuleDao dao = new RuleDaoImpl();
		dao.save(rule);
		
		Rule created_rule = dao.find(rule.getKey());
		Assert.assertEquals(created_rule.getType(), rule.getType());
		Assert.assertEquals(created_rule.getValue(), null);
	}
}
