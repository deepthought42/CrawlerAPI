package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.journeys.Step;

public interface StepRepository extends Neo4jRepository<Step, Long>{

	@Query("MATCH (p:Step{key:{step_key}}) RETURN p LIMIT 1")
	Step findByKey(@Param("step_key") String step_key);
	
}
