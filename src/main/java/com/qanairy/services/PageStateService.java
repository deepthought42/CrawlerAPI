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
	private FormService form_service;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private ElementStateService page_element_service;
	
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
		
		PageState page_state_record = null;
		log.warn("Page state service saving   :: " + page_state);
		log.warn("page state browser " + page_state.getBrowser());
		log.warn("page state key " + page_state.getKey());
		log.warn("page state screenshot url ::  " + page_state.getScreenshotUrl());
		log.warn("page state source  ::  " + page_state.getSrc());
		log.warn("page state animated urls :: "+page_state.getAnimatedImageUrls());
		log.warn("page state screenshots  :: "+page_state.getScreenshots());
				
		for(Screenshot screenshot : page_state.getScreenshots()){
			page_state_record = findByScreenshotChecksum(screenshot.getChecksum());
			log.warn("page state record found :: " + page_state_record);
			if(page_state_record != null){
				break;
			}
		}
		if(page_state_record != null){
			page_state_record.setLandable(page_state.isLandable());
			page_state_record.setLastLandabilityCheck(page_state.getLastLandabilityCheck());
			page_state_record.setElements(page_state.getElements());
	
			Set<Form> forms = new HashSet<Form>();
			for(Form form : page_state.getForms()){
				forms.add(form_service.save(form));
			}
			
			page_state_record.setForms(forms);
			
			page_state_record = page_state_repo.save(page_state_record);
			page_state_record.setElements(getElementStates(page_state.getKey()));
			page_state_record.setScreenshots(getScreenshots(page_state_record.getKey()));
		}
		else {
			page_state_record = findByKey(page_state.getKey());

			if(page_state_record != null){
				page_state_record.setLandable(page_state.isLandable());
				page_state_record.setLastLandabilityCheck(page_state.getLastLandabilityCheck());
				page_state_record.setElements(page_state.getElements());
				page_state_record.setForms(page_state.getForms());
				for(String screenshot_checksum : page_state.getScreenshotChecksums()){
					page_state_record.addScreenshotChecksum(screenshot_checksum);
				}
				
				log.warn("page state screenshots for page update  :  "+page_state.getScreenshots());
				List<Screenshot> screenshots = new ArrayList<Screenshot>(page_state.getScreenshots().size());
				for(Screenshot screenshot : page_state.getScreenshots()){
					screenshots.add(screenshot_service.save(screenshot));
				}
				page_state_record.setScreenshots(screenshots);
				page_state_record = page_state_repo.save(page_state_record);
				page_state_record.setElements(getElementStates(page_state_record.getKey()));
				page_state_record.setScreenshots(screenshots);
			}
			else{
				//iterate over page elements
				Set<ElementState> element_records = new HashSet<>();
				for(ElementState element : page_state.getElements()){
					ElementState element_record = page_element_service.save(element);
					
					element_records.add(element_record);
				}
				page_state.setElements(element_records);
				
				Set<Form> form_records = new HashSet<>();
				for(Form form : page_state.getForms()){
					Form form_record = form_repo.findByKey(form.getKey());
					if(form_record == null){
						List<ElementState> form_element_records = new ArrayList<>();
						for(ElementState element : page_state.getElements()){
							ElementState element_record = page_element_service.save(element);
							
							form_element_records.add(element_record);
						}
						
						form.setFormFields(form_element_records);
						
						form_record = form_repo.save(form);
					}
					form_records.add(form_record);
				}

				List<Screenshot> screenshots = new ArrayList<Screenshot>(page_state.getScreenshots().size());
				for(Screenshot screenshot : page_state.getScreenshots()){
					screenshots.add(screenshot_service.save(screenshot));
				}
				page_state.setScreenshots(screenshots);
				page_state.setForms(form_records);
				page_state_record = page_state_repo.save(page_state);
			}
		}
		
		return page_state_record;
	}

	public void addToForms(String page_key, Form form){
		PageState page_state = page_state_repo.findByKey(page_key);
		page_state.addForm(form);
		page_state_repo.save(page_state);
	}
	
	public PageState findByKey(String page_key) {
		PageState page_state = page_state_repo.findByKey(page_key);
		if(page_state != null){
			page_state.setElements(getElementStates(page_key));
			page_state.setScreenshots(getScreenshots(page_key));
			
			log.warn("FINDING BY KEY :: " + page_state.getScreenshots());
		}
		return page_state;
	}
	
	public PageState findByScreenshotChecksum(String screenshot_checksum){
		return page_state_repo.findByScreenshotChecksumsContains(screenshot_checksum);		
	}
	
	public Set<ElementState> getElementStates(String page_key){
		return page_state_repo.getElementStates(page_key);
	}
	
	public List<Screenshot> getScreenshots(String page_key){
		return page_state_repo.getScreenshots(page_key);
	}
	
	public Set<PageState> getElementPageStatesWithSameUrl(String url, String key){
		return page_state_repo.getElementPageStatesWithSameUrl(url, key);
	}
}
