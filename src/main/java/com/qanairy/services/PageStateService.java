package com.qanairy.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
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
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private PageElementService page_element_service;
	
	@Autowired
	private ScreenshotSetRepository screenshot_repo;
	
	@Autowired
	private FormRepository form_repo;
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state
	 * @return
	 */
	public PageState save(PageState page_state){
		//iterate over page elements
		Set<PageElement> element_records = new HashSet<>();
		for(PageElement element : page_state.getElements()){
			PageElement element_record = page_element_service.save(element);
			
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
				List<PageElement> form_element_records = new ArrayList<>();
				for(PageElement element : page_state.getElements()){
					PageElement element_record = page_element_service.save(element);
					
					form_element_records.add(element_record);
				}
				
				form.setFormFields(form_element_records);
				
				form_record = form_repo.save(form);
			}
			form_records.add(form_record);
		}
		page_state.setForms(form_records);
		
		page_state = page_state_repo.save(page_state);

		
		return page_state;
	}
}
