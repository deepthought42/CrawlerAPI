package com.looksee.models.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.DomainSettings;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * 
 */
@Repository
@Retry(name = "neoforj")
public interface DomainSettingsRepository extends Neo4jRepository<DomainSettings, Long> {
	
	@Query("MATCH (settings:DomainSettings{key:$key}) RETURN settings LIMIT 1")
	public DomainSettings findByKey(@Param("key") String key, @Param("account_id") String account_id);
	
	@Query("MATCH (d:Domain)-[]->(setting:DomainSetting) WHERE id(d)=$domain_id SET setting.expertise=$expertise RETURN setting")
	public DomainSettings updateExpertiseSetting(@Param("domain_id") long domain_id, @Param("expertise") String expertise);
}
