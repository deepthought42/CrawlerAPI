package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.repository.LoginStepRepository;
import com.looksee.models.repository.StepRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enables interacting with database for {@link SimpleStep Steps}
 */
@Service
@Retry(name = "neoforj")
public class StepService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;

	@Autowired
	private LoginStepRepository login_step_repo;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private TestUserService test_user_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	public Step save(Step step) {
		assert step != null;
		
		log.warn("Step :: "+step);
		log.warn("step key :: "+step.getKey());
		if(step instanceof SimpleStep) {
			Step step_record = step_repo.findByKey(step.getKey());
			
			if(step_record != null) {
				log.warn("found step with key :: "+step_record.getKey());
				return step_record;
			}
			
			
			SimpleStep simple_step = (SimpleStep)step;

			log.warn("step :: "+simple_step);
			log.warn("step key :: "+simple_step.getKey());
			log.warn("step element :: "+simple_step.getElementState());
			log.warn("step element id :: "+simple_step.getElementState().getId());
			log.warn("step start page :: "+simple_step.getStartPage());
			log.warn("step start page id :: "+simple_step.getStartPage().getId());
			log.warn("step end page :: "+simple_step.getEndPage());
			log.warn("step action :: "+simple_step.getAction());
			log.warn("++++++++++++++++++++++++++++++++++++++++++");
			
			
			SimpleStep new_simple_step = new SimpleStep();
			new_simple_step.setAction(simple_step.getAction());
			new_simple_step.setActionInput(simple_step.getActionInput());
			new_simple_step.setKey(step.generateKey());
			new_simple_step = (SimpleStep)step_repo.save(step_record);
			new_simple_step.setStartPage(step_repo.addStartPage(new_simple_step.getId(), simple_step.getStartPage().getId()));
			new_simple_step.setEndPage(step_repo.addEndPage(new_simple_step.getId(), simple_step.getEndPage().getId()));
			new_simple_step.setElementState(step_repo.addElementState(new_simple_step.getId(), simple_step.getElementState().getId()));
			return new_simple_step;
		}
		else if(step instanceof LoginStep) {
			log.warn("looking up loginstep with key :: "+step.getKey());
			LoginStep step_record = login_step_repo.findByKey(step.getKey());
			if(step_record != null) {
				log.warn("found login step with key :: "+step_record.getKey());
				return step_record;
			}
			
			LoginStep login_step = (LoginStep)step;
			
			log.warn("login step :: "+login_step);
			log.warn("login step key :: "+login_step.getKey());
			log.warn("login step username element :: "+login_step.getUsernameElement().getId());
			log.warn("login step password element :: "+login_step.getPasswordElement().getId());
			log.warn("login step submit button :: "+login_step.getSubmitElement());
			log.warn("login step test user :: "+login_step.getTestUser());
			log.warn("login step test user id :: "+login_step.getTestUser().getId());
			log.warn("login step start page :: "+login_step.getStartPage());
			log.warn("login step start page id :: "+login_step.getStartPage().getId());
			log.warn("login step end page :: "+login_step.getEndPage());
			log.warn("++++++++++++++++++++++++++++++++++++++++++");
			
			
			LoginStep new_login_step = new LoginStep();
			new_login_step.setKey(login_step.generateKey());
			log.warn("saving login step");
			new_login_step = login_step_repo.save(new_login_step);
			log.warn("adding start page to login step");
			new_login_step.setStartPage(login_step_repo.addStartPage(new_login_step.getId(), login_step.getStartPage().getId()));
			
			log.warn("setting end page");
			new_login_step.setEndPage(login_step_repo.addEndPage(new_login_step.getId(), login_step.getEndPage().getId()));
			
			//ElementState username_input = element_state_service.findById(login_step.getUsernameElement().getId());
			log.warn("adding username element to login step");
			new_login_step.setUsernameElement(login_step_repo.addUsernameElement(new_login_step.getId(), login_step.getUsernameElement().getId()));
			
			//ElementState password_input = element_state_service.findById(login_step.getPasswordElement().getId());
			log.warn("adding password element to login step");
			new_login_step.setPasswordElement(login_step_repo.addPasswordElement(new_login_step.getId(), login_step.getPasswordElement().getId()));
			
			//ElementState submit_element = element_state_service.findById(login_step.getSubmitElement().getId());
			log.warn("adding submit element to login step");
			new_login_step.setSubmitElement(login_step_repo.addSubmitElement(new_login_step.getId(), login_step.getSubmitElement().getId()));
			
			//TestUser user = test_user_service.findById(login_step.getTestUser().getId());
			log.warn("login step test user id :: "+login_step.getTestUser().getId());
			new_login_step.setTestUser(login_step_repo.addTestUser(new_login_step.getId(), login_step.getTestUser().getId()));
			
			return new_login_step;
		}
		
		return null;
	}

	public ElementState getElementState(String step_key) {
		return step_repo.getElementState(step_key);
	}
}
