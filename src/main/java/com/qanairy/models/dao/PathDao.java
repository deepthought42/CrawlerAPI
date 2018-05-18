package com.qanairy.models.dao;

import com.qanairy.persistence.Path;

/**
 * 
 */
public interface PathDao {
	public Path save(Path path);
	public Path find(String key);
}
