package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Page;
import com.qanairy.models.PageState;

/**
 * 
 */
public interface PageRepository extends Neo4jRepository<Page, Long> {
	public Page findByKey(@Param("key") String key);
	
	@Query("MATCH (p:Page{url:{url}})-[h:HAS]->(e:PageState) RETURN e")
	public Set<PageState> getPageStates(@Param("url") String url);
}
