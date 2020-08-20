package com.qanairy.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.Action;
import com.qanairy.models.ElementState;
import com.qanairy.models.journeys.Step;

@Repository
public interface StepRepository extends Neo4jRepository<Step, Long>{

	public Step findByKey(@Param("step_key") String step_key);

	@Query("MATCH (:ElementInteractionStep{key:{step_key}})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);
	
	@Query("MATCH (:ElementInteractionStep{key:{step_key}})-[]->(a:Action) RETURN a")
	public Action getAction(@Param("step_key") String step_key);
}
