package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import com.qanairy.models.FormRecord;

public interface FormRecordRepository extends Neo4jRepository<FormRecord, Long> {

}
