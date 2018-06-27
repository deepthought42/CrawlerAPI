package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.Test;

/**
 * 
 */
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	public Domain findByKey(@Param("key") String key);
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (d:Domain{host:{domain_host}})-[r:HAS_TEST]->(t:Test) MATCH z=(p:PageState)-[:HAS_SCREENSHOT]->(s:ScreenshotSet) WHERE (t)-[:HAS_PATH_OBJECT]->(p) OR (t)-[:HAS_RESULT]->(p) RETURN z")
	public Set<PageState> getPageStates(@Param("domain_host") String host);
	
	@Query("MATCH (d:Domain{host:{domain_host}})-[r:HAS_TEST]->(t:Test) MATCH (t:Test)-[h:HAS_PATH_OBJECT]->(p:PageElement) RETURN p")
	public Set<PageElement> getPageElements(@Param("domain_host") String host);
	
	@Query("MATCH (n:Domain{host:{domain_host}})-[h:HAS_TEST]->(t:Test)  MATCH a=(t:Test)-[h2:HAS_RESULT]->(p:PageState) RETURN a")
	public Set<Test> getTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[h:HAS_TEST]->(t:Test{correct:'UNVERIFIED'}) MATCH a=(t:Test)-[h2:HAS_RESULT]->(p:PageState) RETURN a")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:'staging-marketing.qanairy.com'})-[h:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[:HAS_TEST_RECORD]->(:TestRecord) WHERE t.correct='PASSING' OR t.correct = 'FAILING' RETURN a,b as c")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[h:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("domain_host") String host);
}
