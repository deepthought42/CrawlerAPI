package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.TestRecord;

public interface TestRecordRepository extends Neo4jRepository<TestRecord, Long> {
	public TestRecord findByKey(@Param("key") String key);
}
