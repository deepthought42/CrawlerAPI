package com.qanairy.models.dao;

import com.qanairy.persistence.Group;

/**
 * 
 */
public interface GroupDao {
	public Group save(Group group);
	public Group find(String key);
}
