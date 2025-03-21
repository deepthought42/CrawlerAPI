package com.crawlerApi.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.competitiveanalysis.Competitor;
import com.crawlerApi.models.competitiveanalysis.brand.Brand;

@Repository
public interface CompetitorRepository extends Neo4jRepository<Competitor, Long> {

	@Query("MATCH (c:Competitor), (brand:Brand) WHERE id(c)=$competitor_id AND id(brand)=$brand_id MERGE (c)-[r:USES]->(brand) return brand")
	public void addBrand(@Param("competitor_id") long competitor_id, @Param("brand_id") long brand_id);

	@Query("MATCH (c:Competitor)-[r:USES]->(brand:Brand) WHERE id(c)=$competitor_id RETURN brand ORDER BY brand.created_at DESC LIMIT 1")
	public Brand getMostRecentBrand(@Param("competitor_id") long competitor_id);
	
	@Query("MATCH (domain:Domain)-[]->(c:Competitor) WHERE id(domain)=$domain_id RETURN c")
	public List<Competitor> getCompetitors(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain) WITH d MATCH (competitor:Competitor) WHERE id(d)=$domain_id AND id(competitor)=$competitor_id MERGE (d)-[:COMPETES_WITH]->(competitor) RETURN competitor")
	public Competitor addCompetitor(@Param("domain_id") long domain_id, @Param("competitor_id") long competitor_id);

}
