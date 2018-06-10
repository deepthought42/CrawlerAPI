package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.minion.api.MessageBroadcaster;
import com.qanairy.models.dao.PageElementDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.ScreenshotSetDao;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.ScreenshotSet;

public class PageStateDaoImpl implements PageStateDao {

	@Override
	public PageState save(PageState page) {
		assert(page != null);
		
		PageState page_record = find(page.getKey());
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(page_record == null){
			page_record = connection.getTransaction().addFramedVertex(PageState.class);
			page_record.setKey(page.getKey());
			page_record.setElementCounts(page.getElementCounts());
			page_record.setLandable(page.isLandable());
			page_record.setType(PageState.class.getSimpleName());
			page_record.setUrl(page.getUrl());
			page_record.setTotalWeight(page.getTotalWeight());
			page_record.setImageWeight(page.getImageWeight());
			page_record.setSrc(page.getSrc());
			
			ScreenshotSetDao screenshot_dao = new ScreenshotSetDaoImpl();
			
			for(ScreenshotSet screenshot : page.getBrowserScreenshots()){
				page_record.addBrowserScreenshot(screenshot_dao.save(screenshot));
			}
			
			PageElementDao page_elem_dao = new PageElementDaoImpl();
			for(PageElement elem: page.getElements()){
				page_record.addElement(page_elem_dao.save(elem));
			}
		}
		connection.close();
		return page_record;
	}

	@Override
	public PageState find(String key) {
		PageState page_state = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			page_state = connection.getTransaction().getFramedVertices("key", key, PageState.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find Page State record");
		}
		connection.close();
		return page_state;
	}
}
