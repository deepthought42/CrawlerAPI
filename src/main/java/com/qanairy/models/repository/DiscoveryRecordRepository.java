package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.DiscoveryRecord;

public interface DiscoveryRecordRepository extends Neo4jRepository<DiscoveryRecord, Long> {
	public DiscoveryRecord findByKey(@Param("key") String key);
}
