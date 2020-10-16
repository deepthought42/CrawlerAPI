package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qanairy.models.audit.Observation;

@Repository
public interface ObservationRepository extends Neo4jRepository<Observation, Long> {
	public Observation findByKey(@Param("key") String key);
}
