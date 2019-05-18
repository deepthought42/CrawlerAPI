package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;

/**
 * 
 */
@Repository
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	public PageState findByKey(@Param("key") String key);

	@Query("MATCH a=(p:PageState)-[h:HAS_ELEMENT]->(e:ElementState) WHERE {screenshot_checksum} IN p.screenshot_checksum RETURN a LIMIT 1")
	public PageState findByScreenshotChecksumsContains(@Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{key:{page_key}})-[h:HAS_ELEMENT]->(e:ElementState) RETURN e")
	public Set<ElementState> getElementStates(@Param("page_key") String key);
	
	@Query("MATCH (p:PageState{url:{url}})-[h:HAS_ELEMENT]->(e:ElementState{key:{element_key}}) RETURN p")
	public Set<PageState> getElementPageStatesWithSameUrl(@Param("url") String url, @Param("element_key") String key);
}
