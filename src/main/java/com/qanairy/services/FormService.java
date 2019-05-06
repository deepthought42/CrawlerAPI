package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qanairy.models.Form;
import com.qanairy.models.PageState;
import com.qanairy.models.repository.FormRepository;

@Component
public class FormService {

	@Autowired
	FormRepository form_repo;
	
	public PageState getPageState(Form form) {
		return form_repo.getPageState(form.getKey());
	}

}
