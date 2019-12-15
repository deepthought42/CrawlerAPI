package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;

public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{key}}) OPTIONAL MATCH z=(e)-->(x) RETURN z")
	public ElementState findByKey(@Param("user_id") String user_id, @Param("key") String key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH (f)-[]->(e:ElementState{key:{key}}) OPTIONAL MATCH z=(e)-->(x) RETURN z")
	public ElementState findFormElementByKey(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}) MATCH (e)-[hr:HAS]->(:Rule{key:{key}}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p) MATCH (p)-[]->(f) MATCH (f)-[]->(e:ElementState{key:{element_key}}),(r:Rule{key:{rule_key}}) CREATE element=(e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p) MATCH (p)-[]->(f) MATCH (f)-[]->(e:ElementState{key:{element_key}}) MATCH (e)-[:HAS]->(r:Rule{key:{rule_key}}) RETURN r")
	public Rule getElementRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
}
