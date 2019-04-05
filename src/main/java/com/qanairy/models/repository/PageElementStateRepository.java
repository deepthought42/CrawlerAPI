package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.PageElementState;

public interface PageElementStateRepository extends Neo4jRepository<PageElementState, Long> {
	public PageElementState findByKey(@Param("key") String key);
	
	public PageElementState findByTextAndName(@Param("text") String text, @Param("name") String name);
}
