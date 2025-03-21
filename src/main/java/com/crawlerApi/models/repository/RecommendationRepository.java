package com.crawlerApi.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.audit.recommend.Recommendation;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface RecommendationRepository extends Neo4jRepository<Recommendation, Long> {

}
