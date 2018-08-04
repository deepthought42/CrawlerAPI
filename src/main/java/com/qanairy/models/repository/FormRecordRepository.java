package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import com.qanairy.models.FormRecord;
import org.springframework.data.repository.query.Param;

public interface FormRecordRepository extends Neo4jRepository<FormRecord, Long> {

	public FormRecord findByKey(@Param("key") String key);

}
