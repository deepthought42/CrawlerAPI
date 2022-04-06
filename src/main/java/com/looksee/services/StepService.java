package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.ActionOLD;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.journeys.Step;
import com.looksee.models.repository.StepRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enables interacting with database for {@link Step Steps}
 */
@Service
@Retry(name = "neoforj")
public class StepService {
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	public Step save(Step step) {
		
		
		log.warn("step :: "+step);
		log.warn("step key :: "+step.getKey());
		log.warn("step element :: "+step.getElementState());
		log.warn("step element id :: "+step.getElementState().getId());
		ElementState element_state = element_state_service.findById(step.getElementState().getId());
		log.warn("step element id :: "+element_state.getId());

		log.warn("step start page :: "+step.getStartPage());
		log.warn("step start page id :: "+step.getStartPage().getId());
		PageState start_page = page_state_service.findById(step.getStartPage().getId()).get();

		log.warn("step end page :: "+step.getEndPage());
		log.warn("step end page id :: "+step.getEndPage().getId());
		
		PageState end_page = page_state_service.findById(step.getEndPage().getId()).get();
		step.setStartPage(start_page);
		step.setEndPage(end_page);
		

		log.warn("step action :: "+step.getAction());
		return step_repo.save(step);
	}

	public ElementState getElementState(String step_key) {
		return step_repo.getElementState(step_key);
	}

	public ActionOLD getAction(String step_key) {
		return step_repo.getAction(step_key);
	}
}
