package com.qanairy.models.repository;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Action;
import com.qanairy.models.Domain;
import com.qanairy.models.Form;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestUser;

/**
 * 
 */
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	public Domain findByKey(@Param("key") String key);
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(p:PageState) MATCH a=(p)-[r:HAS_SCREENSHOT]->(s:ScreenshotSet) RETURN p,r,s")
	public Set<PageState> getPageStates(@Param("domain_host") String host);

	@Query("MATCH a=(p:PageState)-[:HAS_SCREENSHOT]->() WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN a")
	public Set<PageState> getResults(@Param("domain_host") String host);

	@Query("MATCH a=(p:PageElement)-[]->() WHERE (:Domain{host:{domain_host}})-[:HAS_TEST]->(:Test) RETURN a")
	public Set<PageElement> getPageElements(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) RETURN a")
	public Set<Action> getActions(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(p) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) MATCH z=(p)-[]->() MATCH (t)-[:HAS_RESULT]->(p1) MATCH x=(p1)-[]->() RETURN a,z,x as w")
	public Set<PathObject> getPathObjects(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() RETURN a,b,c as d")
	public Set<Test> getTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH (t)-[]->(p:PageState) MATCH (p)-[]->(f:Form) MATCH a=(f)-[:FORM__TAG]->() MATCH b=()-[:SUBMIT__FIELD]->() MATCH c=(f)-[:FORM__FIELDS]->()  return a,b,c as d")
	public Set<Form> getForms(@Param("domain_host") String host);
	
	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test{status:'UNVERIFIED'}) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH c=(p)-[:HAS_SCREENSHOT]->() RETURN a,c as d")
	public Set<Test> getUnverifiedTests(@Param("domain_host") String host);

	@Query("MATCH (:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p:PageState) MATCH b=(t)-[]->() MATCH c=(p)-[]->() WHERE t.status='PASSING' OR t.status='FAILING' RETURN a,b,c as d")
	public Set<Test> getVerifiedTests(@Param("domain_host") String host);

	@Query("MATCH (n:Domain{host:{domain_host}})-[:HAS_TEST]->(t:Test) RETURN COUNT(t)")
	public int getTestCount(@Param("domain_host") String host);

	@Query("MATCH (:Domain{key:{domain_key}})-[:HAS_TEST_USER]->(t:TestUser) RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_key") String domain_key);

}
