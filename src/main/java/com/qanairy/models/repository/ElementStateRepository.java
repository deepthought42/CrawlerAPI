package com.qanairy.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Attribute;
import com.qanairy.models.Domain;
import com.qanairy.models.ElementState;
import com.qanairy.models.rules.Rule;

public interface ElementStateRepository extends Neo4jRepository<ElementState, Long> {
	@Query("MATCH (:Account{user_id:{user_id}})-[]-(d:Domain) MATCH (d)-[]->(page:Page) MATCH (page)-[*]->(e:ElementState{key:{key}}) RETURN e LIMIT 1")
	public ElementState findByKey(@Param("user_id") String user_id, @Param("key") String key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{key}}) OPTIONAL MATCH z=(e)-->(x) RETURN e LIMIT 1")
	public ElementState findFormElementByKey(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{element_key}}) MATCH (e)-[hr:HAS]->(:Rule{key:{key}}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{element_key}}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{element_key}}),(r:Rule{key:{rule_key}}) CREATE element=(e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{element_key}}) MATCH (e)-[:HAS]->(r:Rule{key:{rule_key}}) RETURN r LIMIT 1")
	public Rule getElementRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{key:{element_key}}) MATCH (e)-[:HAS_ATTRIBUTE]->(r:Attribute) RETURN r")
	public List<Attribute> getElementAttributes(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(e:ElementState{outer_html:{outer_html}}) RETURN e LIMIT 1")
	public ElementState findByOuterHtml(@Param("user_id") String user_id, @Param("outer_html") String snippet);

	@Query("MATCH (:Account{user_id:{user_id}})-[*]->(es:ElementState{key:{element_key}}) Match (es)-[hbm:HAS]->(b:BugMessage) DELETE hbm,b")
	public void clearBugMessages(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]-(d:Domain) MATCH (d)-[]->(page:Page) MATCH (page)-[*]->(e:ElementState{key:{element_key}}) MATCH (e)-[:HAS]->(es:ElementState) RETURN es")
	public List<ElementState> getChildElements(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[*]->(p:PageState{key:{page_state_key}}) MATCH (p)-[]->(parent_elem:ElementState) MATCH (parent_elem)-[:HAS]->(e:ElementState{key:{element_state_key}}) RETURN parent_elem LIMIT 1")
	public ElementState getParentElement(@Param("user_id") String user_id, @Param("url") Domain url, @Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);
}
