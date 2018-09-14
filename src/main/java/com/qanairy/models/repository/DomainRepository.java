package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;

/**
 * 
 */
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	public Domain findByKey(@Param("key") String key);
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PAGE_STATE]->(p:PageState) RETURN p")
	public Set<PageState> getPageStates(@Param("domain_host") String host);

	@Query("MATCH a=(p:PageState)-[:HAS_SCREENSHOT]->() WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN a")
	public Set<PageState> getResults(@Param("domain_host") String host);

	@Query("MATCH a=(p:PageElement)-[]->() WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN a")
	public Set<PageElement> getPageElements(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(p) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) MATCH z=(p)-[]->() MATCH (t)-[:HAS_RESULT]->(p1) MATCH x=(p1)-[]->() RETURN a,z,x as w")
	public Set<PathObject> getPathObjects(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() RETURN a,b,c as d")
	public Set<Test> getTests(@Param("domain_host") String host);

	//CURRENT QUERY DOESN"T WORK THE COMMENTED ONE DOES
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[:HAS_TEST_RECORD]->() MATCH c=(p)-[:HAS_SCREENSHOT]->() WHERE t.status='UNVERIFIED' RETURN a,b,c as d")
	//@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[:HAS_TEST_RECORD]->(:TestRecord) MATCH c=(t)-[:HAS_GROUP]->(:Group) WHERE t.status='UNVERIFIED' RETURN a,b,c as c")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[]->() MATCH c=(p)-[]->() WHERE t.status='PASSING' OR t.status='FAILING' RETURN a,b,c as d")
	//@Query("MATCH (:Domain{host:'staging-marketing.qanairy.com'})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT|:HAS_TEST_RECORD|:HAS_GROUP]->(d) WHERE (t.status='PASSING' OR t.status='FAILING') AND (d:PageState or d:Group or d:TestRecord) RETURN t,d as c")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("domain_host") String host);
}
