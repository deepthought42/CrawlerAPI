package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;

import org.springframework.data.repository.query.Param;

public interface FormRepository extends Neo4jRepository<Form, Long> {

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match form=(f)-[]->() RETURN form LIMIT 1")
	public Form findByKey(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(:Form{key:{key}}) RETURN p")
	public PageState getPageState(@Param("user_id") String user_id, @Param("url") String url, @Param("key") String key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match (f)-[:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match (f)-[:HAS_SUBMIT]->(e) RETURN e")
	public ElementState getSubmitElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match (f)-[:DEFINED_BY]->(e) RETURN e")
	public ElementState getFormElement(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (:Account{user_id:{user_id}})-[]->(d:Domain) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match (f)-[hbm:HAS]->(b:BugMessage) DELETE hbm,b")
	public Form clearBugMessages(@Param("user_id") String user_id, @Param("form_key") String form_key);
}
