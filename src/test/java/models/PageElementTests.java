package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.AttributePOJO;
import com.qanairy.models.PageElementPOJO;
import com.qanairy.models.dao.PageElementDao;
import com.qanairy.models.dao.impl.PageElementDaoImpl;
import com.qanairy.models.rules.Clickable;
import com.qanairy.models.rules.NumericRestrictionRule;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;


/**
 * 
 *
 */
public class PageElementTests {
	
	@Test(groups="Regression")
	public void pageElementSaveWithAttributeNoRulesPersists(){
		List<Attribute> attributes = new ArrayList<Attribute>();
		List<String> attr_strings = new ArrayList<String>();
		attr_strings.add("spacejam");
		attributes.add(new AttributePOJO("class", attr_strings));
		
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", "purple");
		
		PageElement page_element = new PageElementPOJO("71a7731fee27df1b6e01a589ab1f386aaf83f2b9456083d6e49264c91941b911", "test element", "//div", "div", attributes, css_map);
		PageElementDao page_elem_dao = new PageElementDaoImpl();
		
		page_elem_dao.save(page_element);
		
		PageElement page_element_record = page_elem_dao.find(page_element.getKey());
		
		Assert.assertTrue(page_element_record.getAttributes().size() == page_element.getAttributes().size());
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}

	@Test(groups="Regression")
	public void pageElementSaveWithAttributeAndRulesPersists(){
		List<Attribute> attributes = new ArrayList<Attribute>();
		List<String> attr_strings = new ArrayList<String>();
		attr_strings.add("spacejam");
		attributes.add(new AttributePOJO("class", attr_strings));
		
		List<Rule> rules = new ArrayList<Rule>();
		rules.add(new Clickable());
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", "purple");
		
		PageElement page_element = new PageElementPOJO("71a7731fee27df1b6e01a589ab1f386aaf83f2b9456083d6e49264c91941b911", "test element", "//div", "div", attributes, css_map, rules);
		PageElementDao page_elem_dao = new PageElementDaoImpl();
		
		page_elem_dao.save(page_element);
		
		PageElement page_element_record = page_elem_dao.find(page_element.getKey());
		
		Assert.assertTrue(page_element_record.getAttributes().size() == page_element.getAttributes().size());
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
}
