package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.Account;
import com.qanairy.models.Domain;

public interface AccountRepository extends Neo4jRepository<Account, Long> {
	//Account findByKey(@Param("key") String key);
	Account findByUsername(@Param("username") String username);

	@Query("MATCH (account:Account {username:{0}})-[:HAS_DOMAIN]->(domain:Domain) RETURN domain")
	public Set<Domain> getDomains(String username);
	//public List<DiscoveryRecord> getAllDiscoveryRecords();
	//public List<DiscoveryRecord> getDiscoveryRecordsByMonth(int month);
	//public List<TestRecord> getAllTestRecords();
	//public List<TestRecord> getTestRecordsByMonth(int month);
	//public void removeDomain(Account account, Domain domain);
}
