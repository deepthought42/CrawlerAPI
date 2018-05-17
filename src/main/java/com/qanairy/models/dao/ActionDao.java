package com.qanairy.models.dao;

import com.qanairy.persistence.Action;

/**
 * 
 */
public interface ActionDao {
	public Action save(Action action);
	public Action find(String key);
}
