package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.ActionOLD;
import com.looksee.models.ElementState;
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
	
	public Step findByKey(String step_key) {
		return step_repo.findByKey(step_key);
	}

	public Step save(Step step) {
		return step_repo.save(step);
	}

	public ElementState getElementState(String step_key) {
		return step_repo.getElementState(step_key);
	}

	public ActionOLD getAction(String step_key) {
		return step_repo.getAction(step_key);
	}
}
