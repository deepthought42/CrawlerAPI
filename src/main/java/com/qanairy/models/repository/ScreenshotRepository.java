package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Screenshot;

/**
 * 
 */
public interface ScreenshotRepository extends Neo4jRepository<Screenshot, Long> {
	public Screenshot findByKey(@Param("key") String key);
}
