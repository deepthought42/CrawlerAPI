package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.PageElement;
import com.qanairy.models.dto.PageElementRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class PageElementTests {
	
	@Test
	public void pageElementCreateRecord(){
		PageElement page_element = new PageElement("test element", "//div", "div", null);
		PageElementRepository page_elem_repo = new PageElementRepository();
		
		PageElement page_element_record = page_elem_repo.create(new OrientConnectionFactory(), page_element);
		
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
	
	@Test
	public void pageElementUpdateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		PageElement page_element = new PageElement("test element2", "//div/test", "div", null);
		PageElementRepository page_elem_repo = new PageElementRepository();
		page_elem_repo.create(connection, page_element);
		page_element.setName("updated test element2");
		
		PageElement page_element_record = page_elem_repo.update(connection, page_element);
		

		Assert.assertTrue(page_element_record.getKey().equals(page_elem_repo.generateKey(page_element)));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
	
	@Test
	public void pageElementFindRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		PageElement page_element = new PageElement("find test element", "//body/div", "div", null);
		PageElementRepository page_elem_repo = new PageElementRepository();

		page_elem_repo.create(connection, page_element);
		PageElement page_element_record = page_elem_repo.find(connection, page_element.getKey());
			
		Assert.assertTrue(page_element_record.getKey().equals(page_element.getKey()));
		Assert.assertTrue(page_element_record.getCssValues().keySet().size() == page_element.getCssValues().keySet().size());
		Assert.assertTrue(page_element_record.getName().equals(page_element.getName()));
		Assert.assertTrue(page_element_record.getText().equals(page_element.getText()));
		Assert.assertTrue(page_element_record.getXpath().equals(page_element.getXpath()));
	}
}
