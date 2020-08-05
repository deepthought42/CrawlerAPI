package com.qanairy.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.experience.PerformanceInsight;

/**
 * 
 */
@Repository
public interface PageRepository extends Neo4jRepository<Page, Long> {
	
	@Deprecated
	@Query("MATCH(:Account{user_id:{user_id}})-[*]->(p:Page{key:{page_key}}) RETURN p LIMIT 1")
	public Page findByKeyAndUser(@Param("user_id") String user_id, @Param("page_key") String key);
	
	@Query("MATCH (p:Page{key:{page_key}}) RETURN p LIMIT 1")
	public Page findByKey(@Param("page_key") String key);
	
	@Query("MATCH (p:Page{url:{url}})-[h:HAS]->(e:PageState) RETURN e")
	public Set<PageState> getPageStates(@Param("url") String url);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]->(p:Page{key:{page_key}}),(insight:PerformanceInsight{key:{insight_key}}) CREATE (p)-[h:HAS]->(insight) RETURN insight")
	public PerformanceInsight addPerformanceInsight(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String page_key, @Param("insight_key") String insight_key);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]->(p:Page{key:{page_key}}) MATCH (p)-[]->(insight:PerformanceInsight{key:{insight_key}}) RETURN insight LIMIT 1")
	public PerformanceInsight getPerformanceInsight(
			@Param("user_id") String user_id, 
			@Param("url") String url, 
			@Param("page_key") String page_key, 
			@Param("insight_key") String insight_key);

	@Query("MATCH (p:Page{key:{page_key}})-[]->(insight:PerformanceInsight) RETURN insight")
	public List<PerformanceInsight> getAllPerformanceInsights(@Param("page_key") String page_key);
	
	@Query("MATCH (p:Page{key:{page_key}})-[]->(insight:PerformanceInsight) RETURN insight ORDER BY insight.executed_at DESC LIMIT 1")
	public PerformanceInsight getLatestPerformanceInsight(@Param("page_key") String page_key);

	@Query("MATCH (p:Page{key:{page_key}})-[:HAS]->(page_state:PageState) RETURN page_state ORDER BY page_state.created_at DESC LIMIT 1")
	public PageState findMostRecentPageState(@Param("page_key") String page_key);

	@Query("MATCH (p:Page{key:{page_key}})-[]->(page_state:PageState{key:{page_state_key}}) RETURN page_state LIMIT 1")
	public Optional<PageState> findPageStateForPage(@Param("page_key") String page_key, @Param("page_state_key") String page_state_key);

	@Query("MATCH (p:Page{key:{page_key}}),(ps:PageState{key:{page_state_key}}) CREATE (p)-[h:HAS]->(ps) RETURN ps")
	public void addPageState(@Param("page_key") String page_key, @Param("page_state_key") String page_state_key);
}
