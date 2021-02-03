package com.qanairy.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.CrawlStat;

public interface CrawlStatRepository extends Neo4jRepository<CrawlStat, Long> {
	
}
