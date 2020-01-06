package com.qanairy.models.repository;

import java.util.List;

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
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{key:{key}}) RETURN p LIMIT 1")
	public PageState findByKey(@Param("user_id") String user_id, @Param("url") String url, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH a=(p)-[h:HAS]->() WHERE {screenshot_checksum} IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumsContains(@Param("user_id") String user_id, @Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{key:{page_key}}) MATCH (p)-[h:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{key:{page_key}}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String page_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{key:{page_key}}) WHERE {screenshot_checksum} IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("user_id") String user_id, @Param("url") String url, @Param("screenshot_checksum") String screenshot_checksum);

	@Query("MATCH (a:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(f:Form{key:{form_key}}) RETURN p")
	public List<PageState> findPageStatesWithForm(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
}
