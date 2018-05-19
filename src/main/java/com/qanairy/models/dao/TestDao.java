package com.qanairy.models.dao;

import java.util.List;

import com.qanairy.persistence.Test;

/**
 * 
 */
public interface TestDao {
	public Test save(Test test);
	public Test find(String key);
	public List<Test> findByUrl(String url);
	public Test findByName(String name);
}
