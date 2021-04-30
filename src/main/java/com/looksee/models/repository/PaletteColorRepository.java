package com.looksee.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.audit.PaletteColor;

@Repository
public interface PaletteColorRepository extends Neo4jRepository<PaletteColor, Long> {
	public PaletteColor findByKey(@Param("key") String key);
}
