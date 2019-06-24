package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.repository.FormRepository;

@Component
public class FormService {
	private static Logger log = LoggerFactory.getLogger(FormService.class);

	@Autowired
	private FormRepository form_repo;
	
	@Autowired
	private ElementStateService element_service;
	
	public PageState getPageState(Form form) {
		return form_repo.getPageState(form.getKey());
	}
	
	public Form findByKey(String key){
		return form_repo.findByKey(key);
	}

	public Form save(Form form) {
		Form form_record = form_repo.findByKey(form.getKey());
		if(form_record == null){
			
			List<ElementState> db_records = new ArrayList<ElementState>(form.getFormFields().size());
			for(ElementState element : form.getFormFields()){
				db_records.add(element_service.save(element));
			}
			
			form.setFormFields(db_records);
			form.setSubmitField(element_service.save(form.getSubmitField()));
			form.setFormTag(element_service.save(form.getFormTag()));
			
			
			log.warn("form key   ::  "+ form.getKey());
			log.warn("form name  ::  "+form.getName());
			log.warn("form memory id  :: " + form.getMemoryId());
			log.warn("form date discovered   ::  " + form.getDateDiscovered());
			log.warn("form prediction   ::   " + form.getPrediction());
			log.warn("form status  ::  " + form.getStatus());
			log.warn("form type ::  " + form.getType());
			log.warn("FORM FIELDS   :::   " + form.getFormFields());
			
			log.warn("form submit field   ::   "+form.getSubmitField());
			log.warn("form element state tag  :: "+form.getFormTag());
			
			log.warn("FORM REPO   :::    "+form_repo);
			form_record = form_repo.save(form);
		}
		
		return form_record;
	}

}
