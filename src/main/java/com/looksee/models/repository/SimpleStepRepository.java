package com.looksee.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.journeys.SimpleStep;

@Repository
public interface SimpleStepRepository extends Neo4jRepository<SimpleStep, Long> {

	@Query("MATCH (step:SimpleStep{key:$step_key}) MATCH p=(step)-[*1]->(e) RETURN p")
	public SimpleStep findByKey(@Param("step_key") String step_key);

	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (s:SimpleStep),(p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep),(p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep),(p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);
}
