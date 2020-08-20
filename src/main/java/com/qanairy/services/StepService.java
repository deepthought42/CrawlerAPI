package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.journeys.Step;
import com.qanairy.models.repository.StepRepository;

/**
 * Enables interacting with database for {@link Step Steps}
 */
@Service
public class StepService {
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	public Step save(Step step) {
		log.warn("Step being saved..."+step);
		log.warn("step class :: "+step.getClass().getSimpleName());
		Step step_record = step_repo.findByKey(step.getKey());
		if(step_record == null) {
			step_record = step_repo.save(step);
		}
		return step_record;
	}

	public ElementState getElementState(String step_key) {
		return step_repo.getElementState(step_key);
	}

	public Action getAction(String step_key) {
		return step_repo.getAction(step_key);
	}
}
