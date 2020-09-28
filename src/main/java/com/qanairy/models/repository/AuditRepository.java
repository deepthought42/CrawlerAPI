package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.Observation;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
public interface AuditRepository extends Neo4jRepository<Audit, Long> {
	public Audit findByKey(@Param("key") String key);

	@Query("MATCH (Audit{key:{audit_key}})-[:OBSERVED]-(observation) OPTIONAL MATCH y=(observation)-->() RETURN observation, y")
	public List<Observation> findObservationsForAudit(@Param("audit_key") String audit_key);
}
