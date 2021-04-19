package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.audit.UXIssueMessage;

@Repository
public interface UXIssueMessageRepository extends Neo4jRepository<UXIssueMessage, Long>  {
	public UXIssueMessage findByKey(@Param("key") String key);
}
