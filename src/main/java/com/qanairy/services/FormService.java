package com.qanairy.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.experience.BugMessage;
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
	private DomainService domain_service;
	
	@Autowired
	private BugMessageRepository bug_message_repo;
	
	public PageState getPageState(String user_id, String url, Form form) {
		return form_repo.getPageState(user_id, url, form.getKey());
	}
	
	public Form findByKey(String user_id, String url, String key){
		return form_repo.findByKey(user_id, url, key);
	}

	public Form save(String user_id, String url, Form form) {
		Form form_record = form_repo.findByKey(user_id, url, form.getKey());
		if(form_record == null){
			
			List<ElementState> db_records = new ArrayList<ElementState>(form.getFormFields().size());
			for(ElementState element : form.getFormFields()){
				db_records.add(element_service.saveFormElement(user_id, element));
			}
			
			form.setFormFields(db_records);
			form.setSubmitField(element_service.saveFormElement(user_id, form.getSubmitField()));
			form.setFormTag(element_service.saveFormElement(user_id, form.getFormTag()));
			
			
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

	public Form findById(String user_id, long domain_id, long form_id) {
		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			Optional<Domain> optional_domain = domain_service.findById(domain_id);
			log.info("Does the domain exist :: "+optional_domain.isPresent());
	    	if(optional_domain.isPresent()){
	    		Domain domain = optional_domain.get();
		    	
				form.setFormFields(form_repo.getElementStates(user_id, domain.getUrl(), form.getKey()));
				for(ElementState element : form.getFormFields()){
					element.setRules(element_service.getRules(user_id, element.getKey()));
				}
				form.setFormTag(form_repo.getFormElement(user_id, domain.getUrl(), form.getKey()));
				form.setSubmitField(form_repo.getSubmitElement(user_id, domain.getUrl(), form.getKey()));
				return form;
	    	}
	    	else {
	    		//throw domain not found exception
	    	}
		}
		return null;
	}
	
	
	public Form addBugMessage(long form_id, BugMessage msg) {
		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			boolean msg_exists = false;
			Form form = opt_form.get();
			//check if form has error message already
			for(BugMessage bug_msg : form.getBugMessages()) {
				if( bug_msg.equals(msg)) {
					msg_exists = true;
				}
			}
			if(!msg_exists) {
				BugMessage bug_msg = bug_message_repo.save(msg);
				form.addBugMessage(bug_msg);
				log.warn("form :: "+form.getBugMessages());
			}
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

	public Form clearBugMessages(String user_id, String form_key) {
		return form_repo.clearBugMessages(user_id, form_key);
	}
}
