package com.looksee.models.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.audit.PaletteColor;

@Repository
public interface PaletteColorRepository extends Neo4jRepository<PaletteColor, Long> {
	public Optional<PaletteColor> findByKey(@Param("key") String key);
}
