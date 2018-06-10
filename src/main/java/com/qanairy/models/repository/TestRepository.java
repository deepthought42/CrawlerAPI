package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.Test;

/**
 * 
 */
public interface TestRepository extends Neo4jRepository<Test, Long> {
	public Test findByKey(@Param("key") String key);
	public List<Test> findByUrl(@Param("url") String url);
	public Test findByName(@Param("name") String name);
}
