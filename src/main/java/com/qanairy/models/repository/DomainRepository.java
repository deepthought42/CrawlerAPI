package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageLoadAnimation;
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
	public Domain findByUrl(@Param("url") String url);

	@Query("MATCH (p:PageState) OPTIONAL MATCH a=(p)-->(:Screenshot) WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN p,a")
	public Set<PageState> getPageStates(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN e,b")
	public Set<ElementState> getElementStates(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(p) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) MATCH z=(p)-[]->() MATCH (t)-[:HAS_RESULT]->(p1) MATCH x=(p1)-[]->() RETURN a,z,x as w")
	public Set<PathObject> getPathObjects(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH  a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() return a,b,c")
	public Set<Form> getForms(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test{status:'UNVERIFIED'}) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) OPTIONAL MATCH z=(p)-->(:Screenshot) OPTIONAL MATCH y=(t)-->(:Group) RETURN a,y,z")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[]->(z) MATCH c=(p)-[]->() MATCH d=(z)-[]->() WHERE t.status='PASSING' OR t.status='FAILING' OR t.status='RUNNING' RETURN a,b,c,d")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{url:{url}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("url") String url);

	@Query("MATCH (n:Domain{url:{url}})-[:HAS_DISCOVERY_RECORD]->(d:DiscoveryRecord) RETURN d")
	public Set<DiscoveryRecord> getDiscoveryRecords(@Param("url") String url);
	
	@Query("MATCH (n:Domain{url:{url}})-[:HAS_DISCOVERY_RECORD]->(d:DiscoveryRecord) WHERE NOT d.status = 'STOPPED' RETURN d ORDER BY d.started_at DESC LIMIT 1")
	public DiscoveryRecord getMostRecentDiscoveryRecord(@Param("url") String url);
	
	//needs work done still to make it return all test records by month
	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public Set<TestRecord> getTestsByMonth(@Param("host") String host, @Param("month") int month);

	@Query("MATCH (:Domain{key:{domain_key}})-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_key") String domain_key);

	@Query("MATCH (:Domain{key:{domain_key}})-[r:HAS_TEST_USER]->(t:TestUser{username:{username}}) DELETE r,t")
	public Set<TestUser> deleteTestUser(@Param("domain_key") String domain_key, @Param("username") String username);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) RETURN a")
	public Set<Redirect> getRedirects(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{url:{url}})-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("url") String url);
	
	@Query("MATCH (p:PageLoadAnimation) WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("domain_host") String host);

}
