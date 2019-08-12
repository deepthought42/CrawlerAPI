package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.ElementState;

public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	@Query("MATCH (p:ElementState{key:{key}}) OPTIONAL MATCH (p)-->(x) RETURN p,x")
	public ElementState findByKey(@Param("key") String key);
	
	public ElementState findByTextAndName(@Param("text") String text, @Param("name") String name);

	public ElementState findByScreenshotChecksum(@Param("screenshot_checksum") String screenshotChecksum);
	
	@Query("MATCH (:ElementState{key:{element_key}})-[hd:HAS]->(Rule{key:{key}}) DELETE hd")
	public void removeRule(@Param("element_key") String element_key, @Param("key") String key);
}
