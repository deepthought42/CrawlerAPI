package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.TestUser;

public interface TestUserRepository extends Neo4jRepository<TestUser, Long> {
	public TestUser findByKey(@Param("key") String key);
	public TestUser findByUsername(@Param("key") String username);
}
