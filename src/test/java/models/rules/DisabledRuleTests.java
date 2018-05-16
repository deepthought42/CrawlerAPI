package models.rules;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.dao.RuleDao;
import com.qanairy.models.dao.impl.RuleDaoImpl;
import com.qanairy.models.rules.Clickable;
import com.qanairy.persistence.Rule;

/**
 * 
 */
public class DisabledRuleTests {

	@Test(groups="Regression")
	public void assertRulePersists(){
		Rule rule = new Clickable();
		RuleDao dao = new RuleDaoImpl();
		dao.save(rule);
		
		Rule created_rule = dao.find(rule.getKey());
		Assert.assertTrue(created_rule.getType().equals(rule.getType()));
		Assert.assertTrue(created_rule.getValue() == null);
	}
}
