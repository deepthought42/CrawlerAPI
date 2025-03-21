package com.crawlerApi.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.LatLng;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface LatLngRepository extends Neo4jRepository<LatLng, Long> {
	
	@Query("MATCH (e:LatLng{key:$key}) RETURN e LIMIT 1")
	public LatLng findByKey(@Param("key") String key);
}
