package com.crawlerApi.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.crawlerApi.models.Form;

public interface FormRepository extends Neo4jRepository<Form, Long> {

	@Deprecated
	@Query("MATCH (account:Account)-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match form=(f)-[]->() WHERE id(account)=$account_id RETURN form LIMIT 1")
	public Form findByKeyForUserAndDomain(@Param("account_id") long account_id, @Param("url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (p:Page{url:$page_url})-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match form=(f)-[]->() RETURN form LIMIT 1")
	public Form findByKey(@Param("page_url") String url, @Param("form_key") String form_key);
	
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form{key:$form_key}) Match (f)-[hbm:HAS]->(b:BugMessage) DELETE hbm,b")
	public Form clearBugMessages(@Param("user_id") String user_id, @Param("form_key") String form_key);
	
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() WHERE id(account)=$account_id return a,b,c")
	public Set<Form> getForms(@Param("account_id") long account_id, @Param("url") String url);
	
}
