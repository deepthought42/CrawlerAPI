package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
		elem.setKey(generateKey(elem));
		convertToRecord(connection, elem);
		connection.save();
		
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
			connection.save();
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
		
		return elem;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public IPageElement convertToRecord(OrientConnectionFactory framedGraph, PageElement elem) {
		@SuppressWarnings("unchecked")
		Iterable<IPageElement> html_tags = (Iterable<IPageElement>) DataAccessObject.findByKey(elem.getKey(), framedGraph, IPageElement.class);
		
		Iterator<IPageElement> iter = html_tags.iterator();
		IPageElement page_elem_record = null;

		
		if(!iter.hasNext()){
			page_elem_record = framedGraph.getTransaction().addVertex("class:"+IPageElement.class.getSimpleName()+","+UUID.randomUUID(), IPageElement.class);

			List<IAttribute> attribute_persist_list = new ArrayList<IAttribute>();
			/*for(Attribute attribute : this.attributes){
				IAttribute attribute_persist = attribute.convertToRecord(framedGraph);
				attribute_persist_list.add(attribute_persist);
			}
			
			html_tagpage_elem_recordibutes(attribute_persist_list);
			*/
			/*List<IIPageElement> child_elements_persist = new ArrayList<IIPageElement>();
			for(PageElement elem : this.child_elements){
				IIPageElement child_element = elem.convertToRecord(framedGraph);
				child_elements_persist.add(child_element);
			}
			*/
			//page_elem_record.setChildElements(child_elements_persist);
			
			page_elem_record.setCssValues(elem.getCssValues());
			page_elem_record.setName(elem.getName());
			page_elem_record.setText(elem.getText());
			page_elem_record.setXpath(elem.getXpath());
			page_elem_record.setKey(elem.getKey());
		}
		else{
			page_elem_record = html_tags.iterator().next();
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
	public String generateKey(PageElement obj) {
		return obj.getXpath();
	}
}