package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.qanairy.models.message.BugMessage;

/**
 * 
 */
@Repository
public interface BugMessageRepository extends Neo4jRepository<BugMessage, Long> {
}
