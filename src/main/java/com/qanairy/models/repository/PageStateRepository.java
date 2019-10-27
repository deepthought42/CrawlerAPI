package com.qanairy.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.Screenshot;

/**
 * 
 */
@Repository
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	@Query("MATCH a=(p:PageState{key:{key}}) RETURN p LIMIT 1")
	public PageState findByKey(@Param("key") String key);

	@Query("MATCH a=(p:PageState)-[h:HAS]->() MATCH (p)-[]->(s:Screenshot{checksum: {screenshot_checksum}}) RETURN a LIMIT 1")
	public PageState findByScreenshotChecksumsContains(@Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{key:{page_key}})-[h:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("page_key") String key);
	
	@Query("MATCH (p:PageState{url:{url}})-[h:HAS]->(e:ElementState{key:{element_key}}) RETURN p")
	public Set<PageState> getElementPageStatesWithSameUrl(@Param("url") String url, @Param("element_key") String key);

	@Query("MATCH (p:PageState{key:{page_key}})-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("page_key") String page_key);

	@Query("MATCH a=(p:PageState) WHERE {screenshot_checksum} IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("screenshot_checksum") String screenshot_checksum);
}
