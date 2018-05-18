package com.qanairy.models.dao;

import com.qanairy.persistence.PathObject;

public interface PathObjectDao {
	public PathObject save(PathObject path_obj);
	public PathObject find(String key);
}
