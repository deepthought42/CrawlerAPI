package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;


public interface AccountRepository extends Neo4jRepository<Account, Long> {
	//Account findByKey(@Param("key") String key);
	Account findByUsername(@Param("username") String username);

	@Query("MATCH p=(account:Account{user_id:{user_id}})-->() RETURN p")
	Account findByUserId(@Param("user_id") String user_id);

	
	@Query("MATCH (account:Account {user_id:{0}})-[:HAS_DOMAIN]->(domain:Domain) RETURN domain")
	public Set<Domain> getDomains(String user_id);
		
	@Query("MATCH (account:Account {username:{username}})-[]->(d:DiscoveryRecord) return d")
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(@Param("username") String username, @Param("month") int month);
	//public List<TestRecord> getAllTestRecords();
	//public List<TestRecord> getTestRecordsByMonth(int month);
	
	@Query("MATCH (account:Account {username:{acct_key}})-[hd:HAS_DOMAIN]->(Domain{key:{domain_key}}) DELETE hd")
	public void removeDomain(@Param("acct_key") String acct_key, @Param("domain_key") String domain_key);

	@Query("MATCH (account:Account {username:{acct_key}})-[:HAS_TEST_RECORD]->(t:TestRecord) RETURN COUNT(t)")
	public int getTestCountByMonth(@Param("acct_key") String acct_key, @Param("month") int month);

	@Query("MATCH (account:Account{user_id:{user_id}})-[edge]->() DELETE edge")
	public void deleteAccountEdges(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account{user_id:{user_id}}) DELETE account")
	public void deleteAccount(@Param("user_id") String user_id);

}
