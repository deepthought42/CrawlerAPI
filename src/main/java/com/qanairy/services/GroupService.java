package com.qanairy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.Group;
import com.qanairy.models.repository.GroupRepository;

@Service
public class GroupService {

	@Autowired
	private GroupRepository group_repo;
	
	public Group save(Group group){
		Group group_record = findByKey(group.getKey());
		if(group_record == null){
			group_record = group_repo.save(group);
		}
		return group_record;
	}
	
	public Group findByKey(String key){
		return group_repo.findByKey(key);
	}
}
