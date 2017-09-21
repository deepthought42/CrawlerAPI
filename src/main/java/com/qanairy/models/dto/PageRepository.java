package com.qanairy.models.dto;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
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
		page.setKey(generateKey(page));
		Page page_record = find(connection, generateKey(page));
		if(page_record == null){
			convertToRecord(connection, page);
			connection.save();
		}
		
		return page;
	}

	/**
	 * {@inheritDoc}
	 */
	public Page update(OrientConnectionFactory connection, Page page) {
		if(page.getKey() == null || page.getKey().isEmpty()){
			page.setKey(generateKey(page));
		}
		Page page2 = find(connection, page.getKey());
		IPage page_record = null;
		if(page2 != null){
			page_record = convertToRecord(connection, page2);
			page_record.setElementCounts(page.getElementCounts());
			page_record.setLandable(page.isLandable());
			page_record.setScreenshot(page.getScreenshot());
			page_record.setSrc(page.getSrc());
			page_record.setUrl(page.getUrl().toString());
			page_record.setTotalWeight(page.getTotalWeight());
			page_record.setImageWeight(page.getImageWeight());
			connection.save();
		}
		PageRepository page_repo = new PageRepository();
		
		return page_repo.convertFromRecord(page_record);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page find(OrientConnectionFactory connection, String key){
		@SuppressWarnings("unchecked")
		Iterable<IPage> pages = (Iterable<IPage>) DataAccessObject.findByKey(key, connection, IPage.class);
		Iterator<IPage> iter = pages.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return convertFromRecord(iter.next());
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
		page.setScreenshot(result.getScreenshot());
		page.setKey(result.getKey());
		page.setLandable(result.isLandable());
		
		//NOTE :: SOURCE IS COMMENTED OUT DUE TO SIZE OF DATA AND LACK OF NECESSITY OUTSIDE OF GENERATING A KEY
		//page.setSrc(result.getSrc());
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
	public IPage convertToRecord(OrientConnectionFactory connection, Page page){
		if(page.getKey() == null || page.getKey().isEmpty()){
			page.setKey(generateKey(page));
		}

		@SuppressWarnings("unchecked")
		Iterator<IPage> pages_iter = ((Iterable<IPage>) DataAccessObject.findByKey(page.getKey(), connection, IPage.class)).iterator();
		
		IPage page_record = null;
		
		if(pages_iter.hasNext()){
			page_record = pages_iter.next();
		}
		else{
			page_record = connection.getTransaction().addVertex("class:"+IPage.class.getSimpleName()+","+UUID.randomUUID(), IPage.class);
			page_record.setKey(page.getKey());
			page_record.setElementCounts(page.getElementCounts());
			page_record.setLandable(page.isLandable());
			page_record.setScreenshot(page.getScreenshot());
			page_record.setSrc(page.getSrc());
			page_record.setType((Page.class.getSimpleName()));
			page_record.setUrl(page.getUrl().toString());
			page_record.setTotalWeight(page.getTotalWeight());
			page_record.setImageWeight(page.getImageWeight());
		}

		return page_record;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Page page) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(page.getSrc());   
	}

	@Override
	public List<Page> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}