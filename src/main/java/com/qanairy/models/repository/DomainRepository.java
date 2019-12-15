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
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{key}}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("user_id") String user_id);
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{host}}) RETURN d LIMIT 1")
	public Domain findByHost(@Param("host") String host, @Param("user_id") String user_id);
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url, @Param("user_id") String user_id);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]-(p:PageState) OPTIONAL MATCH a=(p)-->(z) RETURN a")
	public Set<PageState> getPageStates(@Param("user_id") String user_id, @Param("domain_host") String host);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]-(d:Domain{host:{domain_host}}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<ElementState> getElementStates(@Param("domain_host") String host, @Param("user_id") String user_id);
	
	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("user_id") String user_id, @Param("domain_host") String host);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("user_id") String user_id, @Param("domain_host") String host);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH  a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() return a,b,c")
	public Set<Form> getForms(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH RETURN COUNT(f)")
	public int getFormCount(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test{status:'UNVERIFIED'}) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) OPTIONAL MATCH z=(p)-->(:Screenshot) OPTIONAL MATCH y=(t)-->(:Group) RETURN a,y,z")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host, @Param("user_id") String user_id);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH x=(t)-->(:TestRecord) WHERE t.status='PASSING' OR t.status='FAILING' OR t.status='RUNNING' OPTIONAL MATCH y=(t)-->(:Group) RETURN a,x,y")
	public Set<Test> getVerifiedTests(@Param("url") String url, @Param("user_id") String user_id);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[:HAS_DISCOVERY_RECORD]->(d:DiscoveryRecord) RETURN d")
	public Set<DiscoveryRecord> getDiscoveryRecords(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[:HAS_DISCOVERY_RECORD]->(dr:DiscoveryRecord) WHERE NOT dr.status = 'STOPPED' RETURN dr ORDER BY dr.started_at DESC LIMIT 1")
	public DiscoveryRecord getMostRecentDiscoveryRecord(@Param("url") String url, @Param("user_id") String user_id);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{domain_key}}) MATCH (d)-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("user_id") String user_id, @Param("domain_key") String domain_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{domain_key}}) MATCH (d)-[r:HAS_TEST_USER]->(t:TestUser{username:{username}}) DELETE r,t")
	public Set<TestUser> deleteTestUser(@Param("user_id") String user_id, @Param("domain_key") String domain_key, @Param("username") String username);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) RETURN a")
	public Set<Redirect> getRedirects(@Param("user_id") String user_id, @Param("domain_host") String host);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation)  RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("user_id") String user_id, @Param("domain_host") String host);

}
