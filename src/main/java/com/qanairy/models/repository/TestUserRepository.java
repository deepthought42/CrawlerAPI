package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.TestUser;

/**
 * 
 */
public interface TestUserRepository extends Neo4jRepository<TestUser, Long> {

}
