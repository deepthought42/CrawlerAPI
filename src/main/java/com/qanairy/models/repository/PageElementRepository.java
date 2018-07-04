package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.PageElement;

public interface PageElementRepository extends Neo4jRepository<PageElement, Long> {
	public PageElement findByKey(@Param("key") String key);
}
