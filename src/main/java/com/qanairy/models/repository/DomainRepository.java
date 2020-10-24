package com.qanairy.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageVersion;
import com.qanairy.models.PageLoadAnimation;
import com.qanairy.models.Element;
import com.qanairy.models.PageState;
import com.qanairy.models.Redirect;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.TestUser;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.experience.PerformanceInsight;

/**
 * 
 */
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{key}}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("user_id") String user_id);
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{host}}) RETURN d LIMIT 1")
	public Domain findByHostForUser(@Param("host") String host, @Param("user_id") String user_id);
	
	@Query("MATCH (d:Domain{host:{host}}) RETURN d LIMIT 1")
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (d:Domain{host:{host}})-[:HAS]->(p:PageVersion) RETURN p")
	public List<PageVersion> getPages(@Param("host") String host);
	
	@Query("MATCH (a:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) RETURN d LIMIT 1")
	public Domain findByUrlAndAccountId(@Param("url") String url, @Param("user_id") String user_id);

	@Query("MATCH (d:Domain{url:{url}}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url);

	@Query("MATCH (d:Domain{host:{host}})-[]->(p:PageVersion) MATCH (p)-[]-(ps:PageState) RETURN ps")
	public Set<PageState> getPageStates(@Param("host") String host);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]-(d:Domain{url:{url}}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<Element> getElementStates(@Param("url") String url, @Param("user_id") String user_id);
	
	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{host:{domain_host}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("user_id") String user_id, @Param("domain_host") String host);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() return a,b,c")
	public Set<Form> getForms(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageVersion) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) RETURN COUNT(f)")
	public int getFormCount(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{host:{host}}) MATCH (d)-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("user_id") String user_id, @Param("host") String host);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{domain_key}}) MATCH (d)-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("user_id") String user_id, @Param("domain_key") String domain_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{key:{domain_key}}) MATCH (d)-[r:HAS_TEST_USER]->(t:TestUser{username:{username}}) DELETE r,t")
	public Set<TestUser> deleteTestUser(@Param("user_id") String user_id, @Param("domain_key") String domain_key, @Param("username") String username);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) RETURN a")
	public Set<Redirect> getRedirects(@Param("user_id") String user_id, @Param("url") String host);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{host:{url}}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation) RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{url:{page_url}}) MATCH (p)-[:HAS]->(:PerformanceInsight)")
	public Set<PerformanceInsight> getPerformanceInsights(@Param("user_id") String user_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (:Account{user_id:{user_id}})-[:HAS_DOMAIN]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState{url:{page_url}}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) ORDER BY pi.executed_at DESC LIMIT 1")
	public PerformanceInsight getMostRecentPerformanceInsight(@Param("user_id") String user_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]-(p:PageVersion{key:{page_key}}) OPTIONAL MATCH a=(p)-->(z) RETURN p LIMIT 1")
	public PageVersion getPage(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String page_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}),(p:PageVersion{key:{page_key}}) CREATE (d)-[:HAS]->(p) RETURN p")
	public void addPage(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String page_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]-(p:PageVersion) RETURN p")
	public Set<PageVersion> getPagesForUserId(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH (d:Domain{host:{host}})-[]->(audit:AuditRecord) RETURN audit ORDER BY audit.createdAt DESC LIMIT 1")
	public AuditRecord getMostRecentDomainAuditRecord(@Param("host") String host);

	@Query("MATCH (d:Domain)-[*]->(:PageState{key:{page_state_key}}) RETURN d LIMIT 1")
	public Domain findByPageState(@Param("page_state_key") String page_state_key);

	@Query("MATCH (d:Domain{key:{domain_key}}),(audit:AuditRecord{key:{audit_record_key}}) CREATE (d)-[:HAS]->(audit) RETURN audit")
	public void addAuditRecord(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{key:{domain_key}})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	@Query("MATCH (d:Domain{key:{domain_key}})-[]->(audit:AuditRecord{key:{audit_record_key}}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain)-[*]->(:AuditRecord{key:{audit_record_key}}) RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_key") String audit_record_key);


	@Query("MATCH (domain:Domain) RETURN domain")
	public Set<Domain> getDomains();
}
