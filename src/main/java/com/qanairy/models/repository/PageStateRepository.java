package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.PageState;

/**
 * 
 */
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	public PageState findByKey(@Param("key") String key);
	public PageState findBySrc(@Param("src") String src);
}
