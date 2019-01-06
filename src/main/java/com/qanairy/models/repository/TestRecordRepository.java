package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.TestRecord;

@Repository
public interface TestRecordRepository extends Neo4jRepository<TestRecord, Long> {
	public TestRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (tr:TestRecord{key:{key}}) SET tr.status={status} RETURN tr")
	public TestRecord updateStatus(@Param("key") String key, @Param("status") String status);
}
