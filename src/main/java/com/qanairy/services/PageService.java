package com.qanairy.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.experience.PerformanceInsight;
import com.qanairy.models.repository.PageRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.PerformanceInsightRepository;

/**
 * Methods for interacting with page object
 */
@Service
public class PageService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageService.class);
	
	@Autowired
	private PageRepository page_repo;
	
	@Autowired
	private PageStateRepository page_state_service;
	
	@Autowired
	private PerformanceInsightRepository performance_insight_repo;
	
	
	/**
	 * Saves {@link Page} to database
	 * 
	 * @param page
	 * 
	 * @return {@link Page} object reference to database object
	 * 
	 * @pre page != null;
	 */
	public Page save(String user_id, Page page){
		assert page != null;
		assert user_id != null;
		
		Page page_record = findByKey(user_id, page.getKey());
		if(page_record != null){
			page_record.setPerformanceScore(page.getPerformanceScore());
			page_record.setAccessibilityScore(page.getAccessibilityScore());
			page_record.setSeoScore(page.getSeoScore());
			return page_repo.save(page_record);
		}
		
		System.out.println("page repo ::  "+page_repo);
		System.out.println("Page   ::   "+page);
		return page_repo.save(page);
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
	public Page findByKey(String user_id, String key){
		assert key != null;
		assert !key.isEmpty();
		assert user_id != null;
		assert !user_id.isEmpty();
		
		return page_repo.findByKey(user_id, key);
	}

	/**
	 * 
	 * 
	 * @param page
	 * @param performance_insight
	 * 
	 * @pre user_id != null
	 * @pre !user_id.isEmpty()
	 * @pre domain_url != null;
	 * @pre !domain_url.isEmpty();
	 * @pre page_key != null
	 * @pre !page_key.isEmpty()
	 * @pre performance_insight_key != null
	 * @pre !performance_insight_key.isEmpty();
	 */
	public boolean addPerformanceInsight(String user_id, String domain_url, String page_key, String performance_insight_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert domain_url != null;
		assert !domain_url.isEmpty();
		assert page_key != null;
		assert !page_key.isEmpty();
		assert performance_insight_key != null;
		assert !performance_insight_key.isEmpty();
		
		//check if performance insight already exists for page
		PerformanceInsight performance_insight = page_repo.getPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
		if(performance_insight == null) {
			page_repo.addPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
			return true;
		}
		return false;
	}

	/**
	 * Retrieves a List of all {@link PerformanceInsight}s associated with a {@link Page} that has a given key
	 * 
	 * @param page_key
	 * @return
	 * 
	 * @pre page_key != null
	 * @pre !page_key.isEmpty()
	 */
	public List<PerformanceInsight> findAllInsights(String page_key) {
		assert page_key != null;
		assert !page_key.isEmpty();
		
		return page_repo.getAllPerformanceInsights(page_key);
	}
	
	/**
	 * Retrieves the latest {@link PerformanceInsight} for a {@link Page} with a given key
	 * 
	 * @param page_key
	 * @return
	 * 
	 * @pre page_key != null
	 * @pre !page_key.isEmpty()
	 */
	public PerformanceInsight findLatestInsight(String page_key) {
		assert page_key != null;
		assert !page_key.isEmpty();
		
		PerformanceInsight insight = page_repo.getLatestPerformanceInsight(page_key);
		insight.setAudits(performance_insight_repo.getAllAudits(page_key, insight.getKey()));
		return insight;
	}
	
	/**
	 * 
	 * @param page_key
	 * @return
	 * 
	 * @pre page_key != null
	 * @pre !page_key.isEmpty()
	 */
	public void addPageState(String user_id, String page_key, PageState page_state) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert page_key != null;
		assert !page_key.isEmpty();
		assert page_state != null;
		
		PageState page_state_record = page_state_service.findByKey(user_id, page_state.getKey());
		if(page_state_record == null) {
			page_state_record = page_state_service.save(page_state);
		}
		Page page = page_repo.findByKey(user_id, page_key);
		page.addPageState(page_state_record);
		page_repo.save(page);
	}
}
