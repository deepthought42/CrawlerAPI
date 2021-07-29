package com.looksee.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.rules.Rule;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neo4j")
public interface ElementRepository extends Neo4jRepository<Element, Long> {
	
	@Query("MATCH (e:Element{key:$key}) RETURN e LIMIT 1")
	public Element findByKey(@Param("key") String key);
	
	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{key:$key}) OPTIONAL MATCH z=(e)-->(x) RETURN e LIMIT 1")
	public Element findByKeyAndUserId(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{key:$element_key}) MATCH (e)-[hr:HAS]->(:Rule{key:$key}) DELETE hr")
	public void removeRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("key") String key);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{key:$element_key}) MATCH (e)-[hr:HAS]->(r) RETURN r")
	public Set<Rule> getRules(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{key:$element_key}),(r:Rule{key:$rule_key}) CREATE element=(e)-[hr:HAS]->(r) RETURN r")
	public Rule addRuleToFormElement(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{key:$element_key}) MATCH (e)-[:HAS]->(r:Rule{key:$rule_key}) RETURN r LIMIT 1")
	public Rule getElementRule(@Param("user_id") String user_id, @Param("element_key") String element_key, @Param("rule_key") String rule_key);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(e:Element{outer_html:$outer_html}) RETURN e LIMIT 1")
	public Element findByOuterHtml(@Param("user_id") String user_id, @Param("outer_html") String snippet);

	@Query("MATCH (:Account{user_id:$user_key})-[*]->(es:Element{key:$element_key}) Match (es)-[hbm:HAS]->(b:BugMessage) DELETE hbm,b")
	public void clearBugMessages(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (:Account{user_id:$user_key})-[]-(d:Domain) MATCH (d)-[]->(page:PageVersion) MATCH (page)-[*]->(e:Element{key:$element_key}) MATCH (e)-[:HAS_CHILD]->(es:Element) RETURN es")
	public List<Element> getChildElementsForUser(@Param("user_id") String user_id, @Param("element_key") String element_key);

	@Query("MATCH (e:Element{key:$element_key})-[:HAS_CHILD]->(es:Element) RETURN es")
	public List<Element> getChildElements(@Param("element_key") String element_key);

	@Query("MATCH (e:Element{key:$parent_key})-[:HAS_CHILD]->(es:Element{key:$child_key}) RETURN es")
	public List<Element> getChildElementForParent(@Param("parent_key") String parent_key, @Param("child_key") String child_key);

	@Query("MATCH (:Account{user_id:$user_key})-[]->(d:Domain{url:$url}) MATCH (d)-[*]->(p:PageVersion{key:$page_state_key}) MATCH (p)-[]->(parent_elem:Element) MATCH (parent_elem)-[:HAS]->(e:Element{key:$element_key}) RETURN parent_elem LIMIT 1")
	public Element getParentElement(@Param("user_id") String user_id, @Param("url") Domain url, @Param("page_state_key") String page_state_key, @Param("element_key") String element_key);

	@Query("MATCH (p:PageVersion{key:$page_state_key})-[*]->(parent_elem:Element) MATCH (parent_elem)-[:HAS_CHILD]->(e:Element{key:$element_state_key}) RETURN parent_elem LIMIT 1")
	public Element getParentElement(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	@Query("MATCH (parent:Element{key:$parent_key}),(child:Element{key:$child_key}) CREATE (parent)-[:HAS_CHILD]->(child) RETURN parent")
	public void addChildElement(@Param("parent_key") String parent_key, @Param("child_key") String child_key);
}
