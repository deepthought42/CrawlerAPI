package com.looksee.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.audit.AuditRecord;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neo4j")
public interface AccountRepository extends Neo4jRepository<Account, Long> {
	@Query("MATCH (account:Account{email:$email}) RETURN account LIMIT 1")
	Account findByEmail(@Param("email") String username);

	@Query("MATCH (account:Account{user_id:$user_id}) RETURN account")
	Account findByUserId(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account{email:$email})-[:HAS]->(domain:Domain) RETURN domain")
	public Set<Domain> getDomainsForUser(@Param("email") String email);
	
	/** 
	 * NOTE:: relic from old days. Remove at first chance
	 * @param username
	 * @param month
	 * @return
	 */
	@Query("MATCH (account:Account {username:$user_id})-[]->(d:DiscoveryRecord) WHERE datetime(d.started_at).month=$month return d")
	@Deprecated
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(@Param("username") String username, 
														  @Param("month") int month);
	
	@Query("MATCH (account:Account {email:$email})-[hd:HAS]->(Domain{key:$domain_key}) DELETE hd")
	public void removeDomain(@Param("email") String email, @Param("domain_key") String domain_key);

	@Query("MATCH (account:Account {username:$acct_key})-[]->(d:Domain) MATCH (d)-[:HAS_TEST]-(t:Test) MATCH (t)-[:HAS_TEST_RECORD]->(tr:TestRecord) WHERE datetime(tr.ran_at).month=$month RETURN COUNT(tr)")
	public int getTestCountByMonth(@Param("acct_key") String acct_key, 
								   @Param("month") int month);

	@Query("MATCH (account:Account{user_id:$user_id})-[edge]->() DELETE edge")
	public void deleteAccountEdges(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account{user_id:$user_id}) DELETE account")
	public void deleteAccount(@Param("user_id") String user_id);

	@Query("MATCH (account:Account{api_key:$api_key}) RETURN account")
	public Account getAccountByApiKey(@Param("api_key") String api_key);

	@Query("MATCH (t:Account{email:$email}),(a:Domain{key:$domain_key}) CREATE (t)-[r:BELONGS_TO]->(a) RETURN r")
	public void addDomain(@Param("domain_key") String key, @Param("email") String username);

	@Query("MATCH (account:Account{email:$email})-[:HAS]->(domain:Domain{url:$url}) RETURN domain LIMIT 1")
	public Domain findDomain(@Param("email") String email, @Param("url") String url);

	@Query("MATCH (t:Account{username:$username}),(a:AuditRecord) WHERE id(a)=$audit_record_id CREATE (t)-[r:HAS]->(a) RETURN r")
	public AuditRecord addAuditRecord(@Param("username") String username, @Param("audit_record_id") long audit_record_id);

	@Query("MATCH (t:Account),(a:AuditRecord) WHERE id(a)=$audit_record_id AND id(t)=$account_id CREATE (t)-[r:HAS]->(a) RETURN a")
	public AuditRecord addAuditRecord(@Param("account_id") long account_id, @Param("audit_record_id") long audit_record_id);

	@Query("MATCH (account:Account)-[]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN account")
	public Set<Account> findAllForAuditRecord(@Param("audit_record_id") long id);
}
