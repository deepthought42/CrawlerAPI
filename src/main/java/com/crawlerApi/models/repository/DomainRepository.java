package com.crawlerApi.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.ActionOLD;
import com.crawlerApi.models.Domain;
import com.crawlerApi.models.PageLoadAnimation;
import com.crawlerApi.models.Test;
import com.crawlerApi.models.TestRecord;
import com.crawlerApi.models.audit.performance.PerformanceInsight;
import com.crawlerApi.models.journeys.Redirect;

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

	@Query("MATCH (d:Domain{url:$url}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url);

	@Query("MATCH(account:Account)-[]-(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) WHERE id(account)=$account_id RETURN a")
	public Set<ActionOLD> getActions(@Param("account_id") long account_id, @Param("url") String url);

	@Query("MATCH(account:Account)-[]-(d:Domain{host:$domain_host}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) WHERE id(account)=$account_id RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("account_id") long account_id, @Param("domain_host") String host);

	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) WHERE id(account)=$account_id RETURN COUNT(f)")
	public int getFormCount(@Param("account_id") long account_id, @Param("url") String url);

	@Query("MATCH(account:Account)-[]-(d:Domain{host:$host}) MATCH (d)-[:HAS_TEST]->(t:Test) WHERE id(account)=$account_id  RETURN COUNT(t)")
	public int getTestCount(@Param("account_id") long account_id, @Param("host") String host);

	@Query("MATCH (d:Domain)-[r:HAS_TEST_USER]->(t:TestUser{username:$username}) WHERE id(d)=$domain_id AND id(t)=$user_id DELETE r,t return count(t)")
	public int deleteTestUser(@Param("domain_id") long domain_id, @Param("user_id") long user_id);

	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) WHERE id(account)=$account_id RETURN a")
	public Set<Redirect> getRedirects(@Param("account_id") long account_id, @Param("url") String host);
	
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) WHERE id(account)=$account_id RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("account_id") long account_id, @Param("url") String url);
	
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{host:$url}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation) WHERE id(account)=$account_id RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("account_id") long account_id, @Param("url") String url);

	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) WHERE id(account)=$account_id RETURN pi")
	public Set<PerformanceInsight> getPerformanceInsights(@Param("account_id") long account_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) WHERE id(account)=$account_id ORDER BY pi.executed_at DESC LIMIT 1")
	public PerformanceInsight getMostRecentPerformanceInsight(@Param("account_id") long account_id, @Param("url") String url, @Param("page_url") String page_url);

	@Query("MATCH (d:Domain)-[*]->(:PageState{key:$page_state_key}) RETURN d LIMIT 1")
	public Domain findByPageState(@Param("page_state_key") String page_state_key);

	@Query("MATCH (d:Domain) WITH d MATCH (audit:AuditRecord{key:$audit_record_key}) WHERE id(d) = $domain_id MERGE (d)-[:HAS]->(audit) RETURN audit")
	public void addAuditRecord(@Param("domain_id") long domain_id, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain)-[:HAS]->(audit_record:DomainAuditRecord) WHERE id(audit_record)=$audit_record_id RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (domain:Domain) RETURN domain")
	public Set<Domain> getDomains();

	@Query("MATCH (d:Domain) WITH d MATCH (user:TestUser) WHERE id(d)=$domain_id AND id(user)=$test_user_id MERGE (d)-[:HAS_TEST_USER]->(user) RETURN user")
	public void addTestUser(@Param("domain_id") long domain_id, @Param("test_user_id") long test_user_id);

	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) WHERE id(account)=$account_id RETURN domain")
	Set<Domain> getDomainsForAccount(@Param("account_id") long account_id);

	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) WHERE id(account)=$account_id AND domain.url=$url RETURN domain LIMIT 1")
    public Domain findByAccountId(@Param("account_id") long account_id, @Param("url") String url);
}
