package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.ElementState;

public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	public ElementState findByKey(@Param("key") String key);
	
	public ElementState findByTextAndName(@Param("text") String text, @Param("name") String name);

	public ElementState findByScreenshotChecksum(@Param("screenshot_checksum") String screenshotChecksum);
}
