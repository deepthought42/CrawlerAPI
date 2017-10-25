package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.Attribute;
import com.qanairy.models.PageElement;
import com.qanairy.models.Test;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAttribute;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PageElementRepository implements IPersistable<PageElement, IPageElement> {

	/**
	 * {@inheritDoc}
	 */
	public PageElement create(OrientConnectionFactory connection, PageElement elem) {
		PageElement page_elem = find(connection, generateKey(elem));
		if(page_elem == null){
			convertToRecord(connection, elem);
			connection.save();
		}
		else{
			elem = page_elem;
		}
		return elem;
	}

	/**
	 * {@inheritDoc}
	 */
	public PageElement update(OrientConnectionFactory connection, PageElement elem) {
		PageElement page_elem_record = find(connection, elem.getKey());
		IPageElement page_elem = null;
		
		if(page_elem_record != null){
			page_elem = convertToRecord(connection, page_elem_record);

			page_elem.setName(elem.getName());
			page_elem.setAttributes(elem.getAttributes());
			page_elem.setCssValues(elem.getCssValues());
			page_elem.setText(elem.getText());
			page_elem.setXpath(elem.getXpath());
		}
	
		return convertFromRecord(page_elem);
	}
	
	@Override
	public PageElement find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IPageElement> page_elements = (Iterable<IPageElement>) DataAccessObject.findByKey(key, connection, IPageElement.class);
		Iterator<IPageElement> iter = page_elements.iterator();
		PageElementRepository page_elem_repo = new PageElementRepository();
		
		PageElement page_element = null;
		if(iter.hasNext()){
			page_element = page_elem_repo.convertFromRecord(iter.next());
		}
		
		return page_element;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public PageElement convertFromRecord(IPageElement data) {
		PageElement elem = new PageElement();
		elem.setKey(data.getKey());
		elem.setXpath(data.getXpath());
		elem.setText(data.getText());
		elem.setName(data.getName());
		elem.setType(data.getType());
		elem.setAttributes(data.getAttributes());
		elem.setCssValues(data.getCssValues());
		return elem;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public IPageElement convertToRecord(OrientConnectionFactory connection, PageElement elem) {
		elem.setKey(generateKey(elem));

		@SuppressWarnings("unchecked")
		Iterable<IPageElement> html_tags = (Iterable<IPageElement>) DataAccessObject.findByKey(elem.getKey(), connection, IPageElement.class);
		
		Iterator<IPageElement> iter = html_tags.iterator();
		IPageElement page_elem_record = null;

		if(!iter.hasNext()){
			page_elem_record = connection.getTransaction().addVertex("class:"+IPageElement.class.getSimpleName()+","+UUID.randomUUID(), IPageElement.class);
			page_elem_record.setType("PageElement");
			List<IAttribute> attribute_persist_list = new ArrayList<IAttribute>();
			for(Attribute attribute : elem.getAttributes()){
				AttributeRepository attribute_repo = new AttributeRepository();
				IAttribute attribute_persist = attribute_repo.convertToRecord(connection, attribute);
				attribute_persist_list.add(attribute_persist);
			}
						
			/*List<IIPageElement> child_elements_persist = new ArrayList<IIPageElement>();
			for(PageElement elem : this.child_elements){
				IIPageElement child_element = elem.convertToRecord(framedGraph);
				child_elements_persist.add(child_element);
			}
			*/
			//page_elem_record.setChildElements(child_elements_persist);
			
			page_elem_record.setAttributes(elem.getAttributes());
			page_elem_record.setCssValues(elem.getCssValues());
			page_elem_record.setName(elem.getName());
			page_elem_record.setText(elem.getText());
			page_elem_record.setXpath(elem.getXpath());
			page_elem_record.setKey(elem.getKey());
		}
		else{
			page_elem_record = iter.next();
		}
		return page_elem_record;
	}

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness of key as well 
	 * as easy identity of {@link Test} when generated in the wild via discovery
	 * 
	 * @return
	 */
	@Override
	public String generateKey(PageElement page_elem) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(page_elem.getXpath());   

		//return obj.getXpath();
	}

	@Override
	public List<PageElement> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}