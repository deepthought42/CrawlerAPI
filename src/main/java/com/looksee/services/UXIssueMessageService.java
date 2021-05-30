package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.stereotype.Service;

import com.looksee.models.ElementState;
import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.repository.UXIssueMessageRepository;

@Service
public class UXIssueMessageService {
	
	@Autowired
	private UXIssueMessageRepository issue_message_repo;
	
	public UXIssueMessage save(UXIssueMessage low_header_contrast_observation) {
		return issue_message_repo.save(low_header_contrast_observation);
	}

	/**
	 * Find {@link UXIssueMessage} with a given key
	 * @param key used for identifying {@link UXIssueMessage}
	 * 
	 * @return updated {@link UXIssueMessage} object
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 */
	public UXIssueMessage findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();
		
		return issue_message_repo.findByKey(key);
	}
	
	/**
	 * Add recommendation string to observation with a given key
	 * 
	 * @param key for finding observation to be updated
	 * @param recommendation to be added to observation
	 * 
	 * @return updated UXIssueMessage record
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 * @pre priority != null
	 * @pre priority.isEmpty()
	 */
	public UXIssueMessage addRecommendation(String key, String recommendation) {
		assert key != null;
		assert !key.isEmpty();
		assert recommendation != null;
		assert !recommendation.isEmpty();
		
		UXIssueMessage observation = findByKey(key);
    	return save(observation);
	}

	/**
	 * Update priority of observation with a given key
	 * 
	 * @param key for finding observation to be updated
	 * @param priority to be set on observation
	 * @return updated UXIssueMessage record
	 * 
	 * @pre key != null
	 * @pre !key.isEmpty()
	 * @pre priority != null
	 * @pre priority.isEmpty()
	 */
	public UXIssueMessage updatePriority(String key, String priority) {
		assert key != null;
		assert !key.isEmpty();
		assert priority != null;
		assert !priority.isEmpty();
		
		UXIssueMessage observation = findByKey(key);
    	//observation.setPriority(Priority.create(priority));
    	return save(observation);	
	}

	public ElementState getElement(long id) {
		return issue_message_repo.getElement(id);
	}
}
