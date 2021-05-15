package com.looksee.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.TestRecord;
import com.looksee.models.audit.AuditRecord;

@Repository
public interface AccountRepository extends Neo4jRepository<Account, Long> {
	@Query("MATCH (account:Account{username:$username}) RETURN account LIMIT 1")
	Account findByUsername(@Param("username") String username);

	@Query("MATCH (account:Account{user_id:$user_id}) RETURN account")
	Account findByUserId(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account{username:$username})-[:HAS]->(domain:Domain) RETURN domain")
	public Set<Domain> getDomainsForUser(@Param("username") String username);
	
	@Query("MATCH (account:Account {username:$user_id})-[]->(d:DiscoveryRecord) WHERE datetime(d.started_at).month=$month return d")
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(@Param("username") String username, 
														  @Param("month") int month);
	
	@Query("MATCH (account:Account {username:$acct_key})-[hd:HAS]->(Domain{key:$domain_key}) DELETE hd")
	public void removeDomain(@Param("acct_key") String acct_key, @Param("domain_key") String domain_key);

	@Query("MATCH (account:Account {username:$acct_key})-[]->(d:Domain) MATCH (d)-[:HAS_TEST]-(t:Test) MATCH (t)-[:HAS_TEST_RECORD]->(tr:TestRecord) WHERE datetime(tr.ran_at).month=$month RETURN COUNT(tr)")
	public int getTestCountByMonth(@Param("acct_key") String acct_key, 
								   @Param("month") int month);

	@Query("MATCH (account:Account{user_id:$user_id})-[edge]->() DELETE edge")
	public void deleteAccountEdges(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account{user_id:$user_id}) DELETE account")
	public void deleteAccount(@Param("user_id") String user_id);

	@Query("MATCH (account:Account{username:$username})-[:HAS]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t) MATCH (t)-[:HAS_TEST_RECORD]->(tr) RETURN tr")
	public List<TestRecord> getTestRecords(@Param("username") String username, @Param("url") String url);

	@Query("MATCH (account:Account{api_key:$api_key}) RETURN account")
	public Account getAccountByApiKey(@Param("api_key") String api_key);

	@Query("MATCH (t:Account{username:$username}),(a:Domain{key:$domain_key}) CREATE (t)-[r:BELONGS_TO]->(a) RETURN r")
	public void addDomain(@Param("domain_key") String key, @Param("username") String username);

	@Query("MATCH (account:Account{username:$username})-[:HAS]->(domain:Domain{url:$url}) RETURN domain LIMIT 1")
	public Domain findDomain(@Param("username") String username, @Param("url") String url);

	@Query("MATCH (t:Account{username:$username}),(a:AuditRecord) WHERE id(a)=$audit_record_id CREATE (t)-[r:HAS]->(a) RETURN r")
	public AuditRecord addAuditRecord(@Param("username") String username, @Param("audit_record_id") long audit_record_id);
}
