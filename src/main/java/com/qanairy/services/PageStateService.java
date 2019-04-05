package com.qanairy.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Form;
import com.qanairy.models.PageElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.models.repository.FormRepository;
import com.qanairy.models.repository.PageStateRepository;
import com.qanairy.models.repository.ScreenshotSetRepository;

/**
 * Service layer object for interacting with {@link PageState} database layer
 *
 */
@Service
public class PageStateService {
	private static Logger log = LoggerFactory.getLogger(PageStateService.class.getName());
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private PageElementStateService page_element_service;
	
	@Autowired
	private ScreenshotSetRepository screenshot_repo;
	
	@Autowired
	private FormRepository form_repo;
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state
	 * @return
	 * 
	 * @pre page_state != null
	 */
	public PageState save(PageState page_state){
		assert page_state != null;
		
		System.err.println("Saving page state");
		PageState page_state_record = findByKey(page_state.getKey());
		if(page_state_record != null){
			page_state_record.setLandable(page_state.isLandable());
			page_state_record.setLastLandabilityCheck(page_state.getLastLandabilityCheck());
			
			page_state = page_state_repo.save(page_state_record);
			page_state.setElements(getPageElementStates(page_state.getKey()));
			page_state.setBrowserScreenshots(getScreenshots(page_state.getKey()));
		}
		else {
			//iterate over page elements
			Set<PageElementState> element_records = new HashSet<>();
			for(PageElementState element : page_state.getElements()){
				PageElementState element_record = page_element_service.save(element);
				
				element_records.add(element_record);
			}
			
			page_state.setElements(element_records);
			
			Set<ScreenshotSet> screenshot_records = new HashSet<>();
			for(ScreenshotSet screenshot : page_state.getBrowserScreenshots()){
				ScreenshotSet screenshot_record = screenshot_repo.findByKey(screenshot.getKey());
				if(screenshot_record == null){
					screenshot_record = screenshot_repo.save(screenshot);
				}
				screenshot_records.add(screenshot_record);
			}
			page_state.setBrowserScreenshots(screenshot_records);
			
			Set<Form> form_records = new HashSet<>();
			for(Form form : page_state.getForms()){
				Form form_record = form_repo.findByKey(form.getKey());
				if(form_record == null){
					List<PageElementState> form_element_records = new ArrayList<>();
					for(PageElementState element : page_state.getElements()){
						PageElementState element_record = page_element_service.save(element);
						
						form_element_records.add(element_record);
					}
					
					form.setFormFields(form_element_records);
					
					form_record = form_repo.save(form);
				}
				form_records.add(form_record);
			}
			page_state.setForms(form_records);
			page_state = page_state_repo.save(page_state);
		}

		return page_state;
	}

	public PageState findByKey(String page_key) {
		PageState page_state = page_state_repo.findByKey(page_key);
		if(page_state != null){
			page_state.setElements(getPageElementStates(page_key));
			page_state.setBrowserScreenshots(getScreenshots(page_key));
		}
		return page_state;
	}
	
	public Set<PageElementState> getPageElementStates(String page_key){
		return page_state_repo.getPageElementStates(page_key);
	}

	public Set<ScreenshotSet> getScreenshots(String page_key) {
		return page_state_repo.getScreenshotSets(page_key);
	}
}
