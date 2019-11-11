package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.message.BugMessage;
import com.qanairy.models.repository.BugMessageRepository;
import com.qanairy.models.repository.FormRepository;

@Service
public class FormService {
	private static Logger log = LoggerFactory.getLogger(FormService.class);

	@Autowired
	private FormRepository form_repo;
	
	@Autowired
	private ElementStateService element_service;
	
	@Autowired
	private BugMessageRepository bug_message_repo;
	
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

	public Form findById(long form_id) {
		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			form.setFormFields(form_repo.getElementStates(form.getKey()));
			for(ElementState element : form.getFormFields()){
				element.setRules(element_service.getRules(element.getKey()));
			}
			form.setFormTag(form_repo.getFormElement(form.getKey()));
			form.setSubmitField(form_repo.getSubmitElement(form.getKey()));
			return form;
		}
		return null;
	}
	
	
	public Form addBugMessage(long form_id, BugMessage msg) {
		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			BugMessage bug_msg = bug_message_repo.save(msg);
			form.addBugMessage(bug_msg);
			log.warn("form :: "+form.getBugMessages());

			log.warn("form bug message size :: "+form.getBugMessages().size());
			log.warn("form name :: "+form.getName());
			log.warn("form memory id :: "+form.getMemoryId());
			log.warn("form date discovered :: "+form.getDateDiscovered());
			log.warn("form fields :: "+form.getFormFields());
			log.warn("form tag  :: "+form.getFormTag());
			log.warn("form submit field :: "+form.getSubmitField());
			log.warn("form status  :: "+form.getStatus());
			log.warn("form type :: "+form.getType());
			log.warn("form id :: "+form.getId());
			log.warn("form key :: "+form.getKey());
			log.warn("form prediction ::  "+form.getPrediction());
			log.warn("form repo   :: "+form_repo);
			
			return form_repo.save(form);
			
		}
		return null;
	}
	
	public Form removeBugMessage(long form_id, BugMessage msg) {
		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			form.removeBugMessage(msg);
			
			return form_repo.save(form);
		}
		return null;
	}

	public Form clearBugMessages(Long id) {
		Optional<Form> opt_form = form_repo.findById(id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			form.setBugMessages(new ArrayList<>());
			
			return form_repo.save(form);
		}
		return null;
	}

}
