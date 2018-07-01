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
	
	@Query("MATCH a=(p:PageState)-[:HAS_SCREENSHOT]->() WHERE  (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) AND ( (:Test)-[:HAS_PATH_OBJECT]->(p) OR (:Test)-[:HAS_RESULT]->(p)) RETURN a")
	public Set<PageState> getPageStates(@Param("domain_host") String host);
	
	@Query("MATCH (d:Domain{host:{domain_host}})-[r:HAS_TEST]->(t:Test) MATCH (t:Test)-[h:HAS_PATH_OBJECT]->(p:PageElement) RETURN p")
	public Set<PageElement> getPageElements(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test)  MATCH a=(t)-[:HAS_RESULT]->(:PageState) RETURN a")
	public Set<Test> getTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[r]->(d) WHERE (d:PageState OR d:Group) AND t.status='UNVERIFIED' RETURN t,r, d as l")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[:HAS_TEST_RECORD]->(:TestRecord) MATCH c=(t)-[:HAS_GROUP]->(:Group) WHERE t.status='PASSING' OR t.status='FAILING' RETURN a,b,c as c")
	//@Query("MATCH (:Domain{host:'staging-marketing.qanairy.com'})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT|:HAS_TEST_RECORD|:HAS_GROUP]->(d) WHERE (t.status='PASSING' OR t.status='FAILING') AND (d:PageState or d:Group or d:TestRecord) RETURN t,d as c")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("domain_host") String host);
}
