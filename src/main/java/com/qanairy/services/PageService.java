package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.repository.PageRepository;

/**
 * Service layer object for interacting with {@link PageState} database layer
 *
 */
@Service
public class PageService {
	private static Logger log = LoggerFactory.getLogger(PageService.class.getName());
	
	@Autowired
	private PageRepository page_repo;
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state
	 * @return
	 * 
	 * @pre page_state != null
	 */
	public Page save(Page page){
		assert page != null;
		
		Page page_record = findByKey(page.getUrl());
		if(page_record != null){
			page_record.setPageStates(page.getPageStates());
			page = page_record;
		}

		page = page_repo.save(page);
		
		return page;
	}

	public Page findByKey(String page_key) {
		return page_repo.findByKey(page_key);
	}
}
