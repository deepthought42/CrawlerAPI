package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Redirect;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestUser;

/**
 * 
 */
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	public Domain findByKey(@Param("key") String key);
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (p:PageState) WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN p")
	public Set<PageState> getPageStates(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[]->(t:Test) MATCH a=(t)-[]->(p:ElementState) OPTIONAL MATCH b=(p)-->(attr) RETURN p,attr as f")
	public Set<ElementState> getElementStates(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(p) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) MATCH z=(p)-[]->() MATCH (t)-[:HAS_RESULT]->(p1) MATCH x=(p1)-[]->() RETURN a,z,x as w")
	public Set<PathObject> getPathObjects(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() RETURN a,b,c as d")
	public Set<Test> getTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH a=(f)-[:FORM__TAG]->() MATCH b=(f)-[:SUBMIT__FIELD]->() MATCH c=(f)-[:FORM__FIELDS]->()  return a,b,c as d")
	public Set<Form> getForms(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test{status:'UNVERIFIED'}) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) RETURN a")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[]->() MATCH c=(p)-[]->() WHERE t.status='PASSING' OR t.status='FAILING' OR t.status='RUNNING' RETURN a,b,c as g")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_DISCOVERY_RECORD]->(d:DiscoveryRecord) RETURN d")
	public Set<DiscoveryRecord> getDiscoveryRecords(@Param("domain_host") String host);
	
	@Query("MATCH (n:Domain{host:{host}})-[:HAS_DISCOVERY_RECORD]->(d:DiscoveryRecord) RETURN d ORDER BY d.started_at DESC LIMIT 1")
	public DiscoveryRecord getMostRecentDiscoveryRecord(@Param("host") String host);
	
	//needs work done still to make it return all test records by month
	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public Set<TestRecord> getTestsByMonth(@Param("host") String host, @Param("month") int month);

	@Query("MATCH (:Domain{key:{domain_key}})-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_key") String domain_key);

	@Query("MATCH (:Domain{key:{domain_key}})-[r:HAS_TEST_USER]->(t:TestUser{username:{username}}) DELETE r,t")
	public Set<TestUser> deleteTestUser(@Param("domain_key") String domain_key, @Param("username") String username);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) RETURN a")
	public Set<Redirect> getRedirects(@Param("domain_host") String host);

}
