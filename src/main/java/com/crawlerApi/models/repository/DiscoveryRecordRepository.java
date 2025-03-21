package com.crawlerApi.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.crawlerApi.models.Account;
import com.crawlerApi.models.DiscoveryRecord;

public interface DiscoveryRecordRepository extends Neo4jRepository<DiscoveryRecord, Long> {
	public DiscoveryRecord findByKey(@Param("key") String key);

	@Query("MATCH (a:Account)-[:HAS_DISCOVERY_RECORD]->(:DiscoveryRecord{key:$key}) RETURN a")
	public List<Account> getAllAccounts(@Param("key") String key);

	//public void getAllDiscoveriesForTimeframe(int low, int high);
}
