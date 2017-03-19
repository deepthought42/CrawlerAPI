package com.qanairy.models.dto;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.Page;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPage;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PageRepository implements IPersistable<Page, IPage> {

	/**
	 * {@inheritDoc}
	 */
	public Page create(OrientConnectionFactory connection, Page page) {
		IPage page_record = find(connection);
		if(page_record == null){
			page_record = convertToRecord(connection, page);
			connection.save();
		}
		
		return page;
	}

	/**
	 * {@inheritDoc}
	 */
	public Page update(OrientConnectionFactory connection, Page page) {
		IPage page_record = find(connection);
		if(page != null){
			page_record.setElementCounts(page.getElementCounts());
			page_record.setLandable(page.isLandable());
			page_record.setScreenshot(page.getScreenshot());
			page_record.setSrc(page.getSrc());
			page_record.setType(page.getType());
			page_record.setUrl(page.getUrl().toString());
			page_record.setTotalWeight(page.getTotalWeight());
			page_record.setImageWeight(page.getImageWeight());
			connection.save();
		}
		
		return page;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPage find(OrientConnectionFactory connection, String key){
		@SuppressWarnings("unchecked")
		Iterable<IPage> pages = (Iterable<IPage>) DataAccessObject.findByKey(key, connection, IPage.class);
		Iterator<IPage> iter = pages.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param result
	 * @return
	 */
	@Override
	public Page convertFromRecord(IPage result) {
		Page page = new Page();
		page.setType(Page.class.getSimpleName());
		page.setScreenshot(result.getScreenshot());
		page.setKey(result.getKey());
		page.setLandable(result.isLandable());
		page.setSrc(result.getSrc());
		page.setElementCounts(result.getElementCounts());
		
		try {
			page.setUrl(new URL(result.getUrl()));
		} catch (MalformedURLException e) {
			page.setUrl(null);
			e.printStackTrace();
		}

		return page;
	}
	
	/**
	 * Converts Page to IPage for persistence
	 * 
	 * @param page
	 */
	public IPage convertToRecord(OrientConnectionFactory connection, Paqe page){
		page.setKey(generateKey(page));
		find(connection, page.getKey());
		@SuppressWarnings("unchecked")
		Iterable<IPage> pages = (Iterable<IPage>) DataAccessObject.findByKey(this.getKey(), connection, IPage.class);
		
		Iterator<IPage> iter = pages.iterator();
		IPage page = null;
		
		if(iter.hasNext()){
			page = pages.iterator().next();
		}
		else{
			page = connection.getTransaction().addVertex("class:"+IPage.class.getCanonicalName()+","+UUID.randomUUID(), IPage.class);
			page.setKey(this.getKey());
			page.setElementCounts(this.getElementCounts());
			page.setLandable(this.isLandable());
			page.setScreenshot(this.getScreenshot());
			page.setSrc(this.getSrc());
			page.setType(this.getType());
			page.setUrl(this.getUrl().toString());
			page.setTotalWeight(this.getTotalWeight());
			page.setImageWeight(this.getImageWeight());
		}

		return page;
	}
}