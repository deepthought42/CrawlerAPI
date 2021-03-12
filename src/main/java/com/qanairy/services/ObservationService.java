package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.audit.Observation;
import com.qanairy.models.enums.Priority;
import com.qanairy.models.repository.ObservationRepository;

@Service
public class ObservationService {

	@Autowired
	private ObservationRepository observation_repo;
	
	/**
	 * Find {@link Observation} with a given key
	 * @param key used for identifying {@link Observation}
	 * 
	 * @return updated {@link Observation} object
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 */
	public Observation findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();
		
		return observation_repo.findByKey(key);
	}
	
	/**
	 * Save a given {@link Observation} to the database
	 * 
	 * @param observation {@link Observation} to be saved
	 * 
	 * @return updated {@link Observation} object
	 * 
	 * @pre observation != null
	 */
	public Observation save(Observation observation) {
		assert observation != null;

		return observation_repo.save(observation);
	}

	/**
	 * Add recommendation string to observation with a given key
	 * 
	 * @param key for finding observation to be updated
	 * @param recommendation to be added to observation
	 * 
	 * @return updated Observation record
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 * @pre priority != null
	 * @pre priority.isEmpty()
	 */
	public Observation addRecommendation(String key, String recommendation) {
		assert key != null;
		assert !key.isEmpty();
		assert recommendation != null;
		assert recommendation.isEmpty();
		
		Observation observation = findByKey(key);
    	observation.addRecommendation(recommendation);
    	return save(observation);
	}

	/**
	 * Update priority of observation with a given key
	 * 
	 * @param key for finding observation to be updated
	 * @param priority to be set on observation
	 * @return updated Observation record
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 * @pre priority != null
	 * @pre priority.isEmpty()
	 */
	public Observation updatePriority(String key, String priority) {
		assert key != null;
		assert !key.isEmpty();
		assert priority != null;
		assert priority.isEmpty();
		
		Observation observation = findByKey(key);
    	observation.setPriority(Priority.create(priority));
    	return save(observation);	
	}
}
