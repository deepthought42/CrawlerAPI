package com.qanairy.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Page;
import com.qanairy.models.PageState;
import com.qanairy.models.experience.PerformanceInsight;

/**
 * 
 */
public interface PageRepository extends Neo4jRepository<Page, Long> {
	public Page findByKey(@Param("key") String key);
	
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
}
