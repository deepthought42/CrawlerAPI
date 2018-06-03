package com.qanairy.models.dao;

import java.util.List;

import com.qanairy.persistence.Action;

/**
 * 
 */
public interface ActionDao {
	public Action save(Action action);
	public Action find(String key);
	public List<Action> getAll();
}
