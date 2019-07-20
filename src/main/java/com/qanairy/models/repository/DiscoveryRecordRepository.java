package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;

public interface DiscoveryRecordRepository extends Neo4jRepository<DiscoveryRecord, Long> {
	public DiscoveryRecord findByKey(@Param("key") String key);

	@Query("MATCH (a:Account)-[:HAS_DISCOVERY_RECORD]->(:DiscoveryRecord{key:{key}}) RETURN a")
	public List<Account> getAllAccounts(@Param("key") String key);
}
