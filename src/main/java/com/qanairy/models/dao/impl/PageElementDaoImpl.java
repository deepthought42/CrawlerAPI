package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;

import com.qanairy.models.dao.AttributeDao;
import com.qanairy.models.dao.PageElementDao;
import com.qanairy.models.dao.RuleDao;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.Rule;

/**
 * 
 */
public class PageElementDaoImpl implements PageElementDao {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PageElement save(PageElement element) {
		assert element != null;
		
		PageElement page_element_record = find(element.getKey());

		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(page_element_record == null){
			page_element_record = connection.getTransaction().addFramedVertex(PageElement.class);
			page_element_record.setType("PageElement");
			
			/*List<IIPageElement> child_elements_persist = new ArrayList<IIPageElement>();
			for(PageElement elem : this.child_elements){
				IIPageElement child_element = elem.save(framedGraph);
				child_elements_persist.add(child_element);
			}
			*/
			//page_elem_record.setChildElements(child_elements_persist);
			
			page_element_record.setName(element.getName());
			page_element_record.setText(element.getText());

			page_element_record.setXpath(element.getXpath());
			page_element_record.setKey(element.getKey());
			
			
		}
    
		page_element_record.setCssValues(element.getCssValues());
		page_element_record.setScreenshot(element.getScreenshot());
		
		AttributeDao attribute_dao = new AttributeDaoImpl();
		//page_element_record.setAttributes(element.getAttributes());
		for(Attribute attribute : element.getAttributes()){
			page_element_record.addAttribute(attribute_dao.save(attribute));
		}
		
		//page_element_record.setRules(element.getRules());
		RuleDao rule_dao= new RuleDaoImpl();
		for(Rule rule : element.getRules()){
			page_element_record.addRule(rule_dao.save(rule));	
		}
		
		return page_element_record;
	}

	@Override
	public PageElement find(String key) {
		PageElement attr = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			attr = connection.getTransaction().getFramedVertices("key", key, PageElement.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();
		return attr;
	}
}
