package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;

/**
 * 
 */
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	public PageState findByKey(@Param("key") String key);
	
	@Query("MATCH (p:PageState{key:{page_key}})-[h:HAS_SCREENSHOT]->(s:ScreenshotSet) RETURN s")
	public Set<ScreenshotSet> getScreenshotSets(@Param("page_key") String key);
}
