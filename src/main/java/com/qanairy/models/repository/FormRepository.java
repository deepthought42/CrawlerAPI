package com.qanairy.models.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.qanairy.models.ElementState;
import com.qanairy.models.Form;
import com.qanairy.models.PageState;

import org.springframework.data.repository.query.Param;

public interface FormRepository extends Neo4jRepository<Form, Long> {

	@Query("MATCH (:Account{key:{account_key}})-[]->(d:Domain{{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match form=(f)-[]->() RETURN form")
	public Form findByKey(@Param("account_key") String account_key, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("Match (p:PageState)-[:HAS]->(:Form{key:{key}}) RETURN p")
	public PageState getPageState(@Param("key") String key);

	@Query("MATCH (:Account{key:{account_key}})-[]->(d:Domain{{url:{url}}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(f:Form{key:{form_key}}) Match (f)-[:HAS]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("account_key") String account_key, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("Match (:Form{key:{key}})-[:HAS_SUBMIT]->(e) RETURN e")
	public ElementState getSubmitElement(@Param("key") String key);
	
	@Query("Match (:Form{key:{key}})-[:DEFINED_BY]->(e) RETURN e")
	public ElementState getFormElement(@Param("key") String key);
}
