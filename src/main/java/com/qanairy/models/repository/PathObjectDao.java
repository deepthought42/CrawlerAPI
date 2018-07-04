package com.qanairy.models.repository;

import com.qanairy.models.PathObject;

public interface PathObjectDao {
	public PathObject save(PathObject path_obj);
	public PathObject find(String key);
}
