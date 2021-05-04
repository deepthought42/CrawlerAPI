package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.Action;
import com.looksee.models.repository.ActionRepository;

@Service
public class ActionService {

	@Autowired
	private ActionRepository action_repo;
	
	/**
	 * Objects are expected to be immutable as of 3/14/19. When this method is ran, if the 
	 * object already exists then it will be loaded from the database by key, otherwise it will be saved
	 * 
	 * @param action {@link Action} 
	 * @return
	 */
	public Action save(Action action){
		Action action_record = action_repo.findByKey(action.getKey());
		if(action_record == null){
			action_record = action_repo.save(action);
		}
		return action_record;
	}

	/**
	 * Retrieve data from database
	 * 
	 * @param key
	 * @return
	 */
	public Action findByKey(String key) {
		return action_repo.findByKey(key);
	}
}
