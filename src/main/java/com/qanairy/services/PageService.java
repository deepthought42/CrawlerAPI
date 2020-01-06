package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Page;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.repository.PageRepository;

/**
 * Methods for interacting with page object
 */
@Service
public class PageService {

	@Autowired
	private PageRepository page_repo;
	
	/**
	 * Saves {@link Page} to database
	 * 
	 * @param page
	 * 
	 * @return {@link Page} object reference to database object
	 * 
	 * @pre page != null;
	 */
	public Page save(Page page){
		assert page != null;
		
		Page page_record = findByKey(page.getKey());
		if(page_record == null){
			page_record = page_repo.save(page);
		}
		
		return page_record;
	}
	
	/**
	 * Retrieve page from database using key
	 * 
	 * @param key
	 * 
	 * @return {@link Page} record
	 * 
	 * @pre key != null;
	 * @pre !key.isEmpty();
	 */
	public Page findByKey(String key){
		assert key != null;
		assert !key.isEmpty();
		
		return page_repo.findByKey(key);
	}

	/**
	 * 
	 * 
	 * @param page
	 * @param performance_insight
	 */
	public boolean addPerformanceInsight(String user_id, String domain_url, String page_key, String performance_insight_key) {
		//check if performance insight already exists for page
		PerformanceInsight performance_insight = page_repo.getPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
		if(performance_insight == null) {
			page_repo.addPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
			return true;
		}
		return false;
	}
}
