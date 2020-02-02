package com.qanairy.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Screenshot;
import com.qanairy.models.repository.FormRepository;
import com.qanairy.models.repository.PageStateRepository;

/**
 * Service layer object for interacting with {@link PageState} database layer
 *
 */
@Service
public class PageStateService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageStateService.class.getName());
	
	@Autowired
	private ScreenshotService screenshot_service;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private FormRepository form_repo;
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state
	 * @return
	 * 
	 * @pre page_state != null
	 */
	public PageState save(String user_id, String url, PageState page_state) {
		assert page_state != null;
		
		PageState page_state_record = null;
		
		int page_cnt = 0;
		boolean page_err = false;
		do{
			page_err = false;
			try{
				for(String checksum : page_state.getScreenshotChecksums()){
					List<PageState> page_state_records = page_state_repo.findByScreenshotChecksumsContains(user_id, url, checksum);
					if(!page_state_records.isEmpty()){
						page_state_record = page_state_records.get(0);
						page_state_record.setScreenshotChecksum(page_state.getScreenshotChecksums());
						page_state_record = page_state_repo.save(page_state_record);
						break;
					}
				}
				
				if(page_state_record == null){
					
					for(Screenshot screenshot : page_state.getScreenshots()){
						List<PageState> page_state_records = findByScreenshotChecksum(user_id, url, screenshot.getChecksum());
						if(page_state_records.isEmpty()){
							continue;
						}
						page_state_record = findByAnimationImageChecksum(user_id, screenshot.getChecksum());
						if(page_state_record != null){
							page_state_record.setElements(getElementStates(user_id, page_state_record.getKey()));
							break;
						}
					}
				}
				
				if(page_state_record != null){
					page_state_record.setForms(page_state.getForms());
					
					Map<String, Screenshot> screenshot_map = new HashMap<>();
					for(Screenshot screenshot : page_state.getScreenshots()){
						screenshot_map.put(screenshot.getKey(), screenshot);
					}
					for(Screenshot screenshot : getScreenshots(user_id, page_state.getKey())){
						screenshot_map.remove(screenshot.getKey());
					}
					
					for(Screenshot screenshot : screenshot_map.values()){
						page_state_record.addScreenshot(screenshot_service.save(screenshot));
					}
					
					page_state_record = page_state_repo.save(page_state_record);
					
					page_state_record.setElements(getElementStates(user_id, page_state_record.getKey()));
					page_state_record.setScreenshots(getScreenshots(user_id, page_state_record.getKey()));
				}
				else {
					log.warn("page state wasn't found in database. Saving new page state to neo4j");
					page_state_record = findByKey(user_id, page_state.getKey());
		
					if(page_state_record != null){
						page_state_record.setForms(page_state.getForms());
	
						for(String screenshot_checksum : page_state.getScreenshotChecksums()){
							page_state_record.addScreenshotChecksum(screenshot_checksum);
						}
						
						Map<String, Screenshot> screenshot_map = new HashMap<>();
						for(Screenshot screenshot : page_state.getScreenshots()){
							screenshot_map.put(screenshot.getKey(), screenshot);
						}
						for(Screenshot screenshot : getScreenshots(user_id, page_state.getKey())){
							screenshot_map.remove(screenshot.getKey());
						}
						
						for(Screenshot screenshot : screenshot_map.values()){
							page_state_record.addScreenshot(screenshot_service.save(screenshot));
						}
						
						page_state_record = page_state_repo.save(page_state_record);
						page_state_record.setElements(getElementStates(user_id, page_state_record.getKey()));
						page_state_record.setScreenshots(getScreenshots(user_id, page_state_record.getKey()));
					}
					else{
						//iterate over page elements
						List<ElementState> element_records = new ArrayList<>(page_state.getElements().size());
						for(ElementState element : page_state.getElements()){
							boolean err = false;
							int cnt = 0;
							do{
								err = false;
								try{
									element_records.add(element_state_service.save(user_id, element));
								}catch(Exception e){
									log.warn("error saving element to new page state :  "+e.getMessage());
									//e.printStackTrace();
									err = true;
								}
								cnt++;
							}while(err && cnt < 5);
							
							if(err){
								element_records.add(element);
							}
						}
						
						page_state.setElements(element_records);
						
						Set<Form> form_records = new HashSet<>();
						for(Form form : page_state.getForms()){
							Form form_record = form_repo.findByKey(user_id, url, form.getKey());
							if(form_record == null){
								List<ElementState> form_element_records = new ArrayList<>();
								for(ElementState element : form.getFormFields()){
									log.warn("saving form element to page state");
									ElementState element_record = element_state_service.saveFormElement(user_id, element);
									
									form_element_records.add(element_record);
								}
								
								form.setFormFields(form_element_records);
								form_record = form_repo.save(form);
							}
							form_records.add(form_record);
						}
		
						//reduce screenshots to just unique records
						Map<String, Screenshot> screenshot_map = new HashMap<>();
						for(Screenshot screenshot : page_state.getScreenshots()){
							screenshot_map.put(screenshot.getKey(), screenshot);
						}
						
						List<Screenshot> screenshot_list = new ArrayList<Screenshot>();
						for(Screenshot screenshot : screenshot_map.values()){
							screenshot_list.add(screenshot_service.save(screenshot));	
						}
						
						page_state.setScreenshots(screenshot_list);
						page_state.setForms(form_records);
						page_state_record = page_state_repo.save(page_state);
					}
				}
			}catch(ClientException e){
				page_err = true;
			}
			page_cnt++;
		}while(page_err && page_cnt < 5);
		
		return page_state_record;
	}

	public void addToForms(String user_id, String page_key, Form form){
		PageState page_state = page_state_repo.findByKey(user_id, page_key);
		page_state.addForm(form);
		page_state_repo.save(page_state);
	}
	
	public PageState findByKey(String user_id, String page_key) {
		PageState page_state = page_state_repo.findByKey(user_id, page_key);
		if(page_state != null){
			page_state.setElements(getElementStates(user_id, page_key));
			page_state.setScreenshots(getScreenshots(user_id, page_key));
		}
		return page_state;
	}
	
	public List<PageState> findByScreenshotChecksum(String user_id, String url, String screenshot_checksum){
		return page_state_repo.findByScreenshotChecksumsContains(user_id, url, screenshot_checksum);		
	}
	
	public PageState findByAnimationImageChecksum(String user_id, String screenshot_checksum){
		return page_state_repo.findByAnimationImageChecksum(user_id, screenshot_checksum);		
	}
	
	public List<ElementState> getElementStates(String user_id, String page_key){
		return page_state_repo.getElementStates(user_id, page_key);
	}
	
	public List<Screenshot> getScreenshots(String user_id, String page_key){
		List<Screenshot> screenshots = page_state_repo.getScreenshots(user_id, page_key);
		if(screenshots == null){
			return new ArrayList<Screenshot>();
		}
		return screenshots;
	}
	
	public List<PageState> findPageStatesWithForm(String user_id, String url, String page_key) {
		return page_state_repo.findPageStatesWithForm(user_id, url, page_key);
	}

	public Collection<ElementState> getExpandableElements(List<ElementState> elements) {
		List<ElementState> expandable_elements = new ArrayList<>();
		for(ElementState elem : elements) {
			if(elem.isDisplayed() && elem.isLeaf() && !elem.isPartOfForm()) {
				expandable_elements.add(elem);
			}
		}
		return expandable_elements;
	}
}
