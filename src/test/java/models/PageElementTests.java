package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.PageElement;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class PageElementTests {
	
	@Test
	public void pageElementCreateRecord(){
		PageElement page_element = new PageElement("test element", "//div", "div", null);
		IPageElement page_element_record = page_element.create(new OrientConnectionFactory());
		
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
	
	@Test
	public void pageUpdateRecord(){
		PageElement page_element = new PageElement("update test element", "//div", "div", null);
		IPageElement page_element_record = page_element.update(new OrientConnectionFactory());
		

		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
	
	@Test
	public void pageFindRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		PageElement page_element = new PageElement("find test element", "//body/div", "div", null);
		page_element.create(connection);
		IPageElement page_element_record = page_element.find(connection);
			
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
}
