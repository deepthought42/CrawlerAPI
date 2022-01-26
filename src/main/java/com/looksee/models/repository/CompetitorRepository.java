package com.looksee.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.looksee.models.Competitor;

@Repository
public interface CompetitorRepository extends Neo4jRepository<Competitor, Long> {

}
