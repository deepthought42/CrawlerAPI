package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;

public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	@Query("MATCH (p:ElementState{key:{key}}) OPTIONAL MATCH (p)-->(x) RETURN p,x")
	public ElementState findByKey(@Param("key") String key);
	
	public ElementState findByTextAndName(@Param("text") String text, @Param("name") String name);

	public ElementState findByScreenshotChecksum(@Param("screenshot_checksum") String screenshotChecksum);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}) MATCH (e)-[hr:HAS]->(:Rule{key:{key}}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p) MATCH (p)-[]->(f) MATCH (f)-[]->(e:ElementState{key:{element_key}}),(r:Rule{key:{rule_key}}) CREATE (e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p) MATCH (p)-[]->(f) MATCH (f)-[]->(e:ElementState{key:{element_key}}) MATCH (e)-[:HAS]->(r:Rule{key:{rule_key}}) RETURN r")
	public Rule getElementRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
	
	/*
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:NumericRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addNumericRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:EmailPatternRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addEmailPatternRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:AlphabeticRestrictionRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addAlphabeticRestrictionRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:ClickableRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addClickableRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:DisabledRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addDisabledRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:NumericRestrictionRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addNumericRestrictionRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:PatternRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addPatternRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:ReadOnlyRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addReadOnlyRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:RequirementRule{key:{rule_key}})  CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addRequiredRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(e:ElementState{key:{element_key}}),(r:SpecialCharacterRestriction{key:{rule_key}}) CREATE (e)-[hr:HAS]->(r) RETURN hr")
	public void addSpecialCharacterRestriction(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);
*/
}
