package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Action;
import com.qanairy.models.repository.ActionRepository;

@Service
public class ActionService {

	@Autowired
	private ActionRepository action_repo;
	
	public Action save(Action action){
		Action action_record = action_repo.findByKey(action.getKey());
		if(action_record == null){
			action_record = action_repo.save(action);
		}
		return action_record;
	}
}
