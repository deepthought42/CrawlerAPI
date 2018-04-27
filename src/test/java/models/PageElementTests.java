package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.dto.PageElementRepository;
import com.qanairy.persistence.OrientConnectionFactory;


/**
 * 
 *
 */
public class PageElementTests {
	
	@Test(groups="Regression")
	public void pageElementCreateRecord(){
		List<Attribute> attributes = new ArrayList<Attribute>();
		List<String> attr_strings = new ArrayList<String>();
		attr_strings.add("spacejam");
		attributes.add(new Attribute("class", attr_strings));
		
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", "purple");
		
		PageElement page_element = new PageElement("71a7731fee27df1b6e01a589ab1f386aaf83f2b9456083d6e49264c91941b911", "test element", "//div", "div", attributes, css_map);
		PageElementRepository page_elem_repo = new PageElementRepository();
		
		PageElement page_element_record = page_elem_repo.create(new OrientConnectionFactory(), page_element);
		
		Assert.assertTrue(page_element_record.getAttributes().size() == page_element.getAttributes().size());
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
	
	@Test(groups="Regression")
	public void pageElementFindRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		List<Attribute> attributes = new ArrayList<Attribute>();
		List<String> attr_strings = new ArrayList<String>();
		attr_strings.add("spacejam");
		attributes.add(new Attribute("class", attr_strings));
		
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", "purple");
		
		PageElement page_element = new PageElement("find test element", "//body/div", "div", attributes, css_map);
		PageElementRepository page_elem_repo = new PageElementRepository();

		page_element = page_elem_repo.create(connection, page_element);
		PageElement page_element_record = page_elem_repo.find(connection, page_element.getKey());
			
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
}
