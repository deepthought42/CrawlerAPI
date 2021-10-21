package com.looksee.models.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.Action;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.Form;
import com.looksee.models.PageLoadAnimation;
import com.looksee.models.PageState;
import com.looksee.models.Redirect;
import com.looksee.models.Test;
import com.looksee.models.TestRecord;
import com.looksee.models.TestUser;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.performance.PerformanceInsight;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * 
 */
@Repository
@Retry(name = "neoforj")
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{key:$key}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("username") String username);
	
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHostForUser(@Param("host") String host, @Param("username") String username);
	
	@Query("MATCH (d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (d:Domain{host:$host})-[:HAS]->(p:PageState) RETURN p")
	public Set<PageState> getPages(@Param("host") String host);

	@Query("MATCH (d:Domain{url:$url}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url);

	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id RETURN p")
	public Set<PageState> getPageStates(@Param("domain_id") long domain_id);

	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]-(d:Domain{url:$url}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<Element> getElementStates(@Param("url") String url, @Param("username") String username);
	
	@Query("MATCH(:Account{user_id:$user_id})-[]-(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH(:Account{user_id:$user_id})-[]-(d:Domain{host:$domain_host}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("user_id") String user_id, @Param("domain_host") String host);

	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() return a,b,c")
	public Set<Form> getForms(@Param("username") String username, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) RETURN COUNT(f)")
	public int getFormCount(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH(:Account{user_id:$user_id})-[]-(d:Domain{host:$host}) MATCH (d)-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("user_id") String user_id, @Param("host") String host);

	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{key:$domain_key}) MATCH (d)-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("username") String username, @Param("domain_key") String domain_key);

	@Query("MATCH (:Account{acct_username:$acct_username})-[:HAS_DOMAIN]->(d:Domain{key:$domain_key}) MATCH (d)-[r:HAS_TEST_USER]->(t:TestUser{username:$username}) DELETE r,t")
	public Set<TestUser> deleteTestUser(@Param("acct_username") String acct_username, @Param("domain_key") String domain_key, @Param("username") String username);

	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) RETURN a")
	public Set<Redirect> getRedirects(@Param("user_id") String user_id, @Param("url") String host);
	
	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("user_id") String user_id, @Param("url") String url);
	
	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{host:$url}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation) RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(:PerformanceInsight)")
	public Set<PerformanceInsight> getPerformanceInsights(@Param("user_id") String user_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) ORDER BY pi.executed_at DESC LIMIT 1")
	public PerformanceInsight getMostRecentPerformanceInsight(@Param("user_id") String user_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (d:Domain),(p:PageState{key:$page_key}) WHERE id(d)=$domain_id CREATE (d)-[:HAS]->(p) RETURN p")
	public PageState addPage(@Param("domain_id") long domain_id, @Param("page_key") String page_key);
	
	@Query("MATCH (:Account{user_id:$user_id})-[]-(d:Domain{url:$url}) MATCH (d)-[]-(p:PageState) RETURN p")
	public Set<PageState> getPagesForUserId(@Param("user_id") String user_id, @Param("url") String url);

	@Query("MATCH (d:Domain{url:$url})-[]->(audit:DomainAuditRecord) RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("url") String url);

	@Query("MATCH (audit:DomainAuditRecord)-[]->(d:Domain) WHERE id(d) = $id RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("id") long id);

	@Query("MATCH (d:Domain)-[*]->(:PageState{key:$page_state_key}) RETURN d LIMIT 1")
	public Domain findByPageState(@Param("page_state_key") String page_state_key);

	@Query("MATCH (d:Domain),(audit:AuditRecord{key:$audit_record_key}) WHERE id(d) = $domain_id CREATE (d)<-[:HAS]-(audit) RETURN audit")
	public void addAuditRecord(@Param("domain_id") long domain_id, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{key:$domain_key})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	@Query("MATCH (d:Domain{key:$domain_key})<-[]-(audit:AuditRecord{key:$audit_record_key}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain)-[*]->(:AuditRecord{key:$audit_record_key}) RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_key") String audit_record_key);


	@Query("MATCH (domain:Domain) RETURN domain")
	public Set<Domain> getDomains();
	
	@Query("MATCH (d:Domain)-[]->(p:PageState{key:$page_key}) WHERE id(d)=$domain_id RETURN p")
	public Optional<PageState> getPage(@Param("domain_id") long domain_id, @Param("page_key") String page_key);
}
