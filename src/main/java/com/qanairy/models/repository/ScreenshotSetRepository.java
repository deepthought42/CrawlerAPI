package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.ScreenshotSet;

/**
 * 
 */
public interface ScreenshotSetRepository extends Neo4jRepository<ScreenshotSet, Long> {
	public ScreenshotSet findByKey(@Param("key") String key);
}
