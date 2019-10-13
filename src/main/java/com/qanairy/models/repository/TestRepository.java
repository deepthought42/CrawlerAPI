package com.qanairy.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;

/**
 *
 */
public interface TestRepository extends Neo4jRepository<Test, Long> {
	public Test findByKey(@Param("key") String key);
	public Test findByName(@Param("name") String name);

	@Query("MATCH (d:Domain)-[:HAS_TEST]->(t:Test{key:{key}}) RETURN d")
	public Domain getDomain();

	@Query("MATCH p=(t:Test{key:{key}})-[:HAS_GROUP]->() RETURN p")
	public Test getFullTestUsingKey(@Param("key") String key);

	@Query("MATCH p=(t:Test{key:{key}})-[r:HAS_TEST_RECORD]->(tr:TestRecord) RETURN tr ORDER BY tr.ran_at DESC LIMIT 1")
	public TestRecord getMostRecentRecord(@Param("key") String key);

	@Query("MATCH (t:Test{key:{key}})-[r:HAS_PATH_OBJECT]->(p) OPTIONAL MATCH b=(p)-[:HAS]->() RETURN p,b")
	public List<PathObject> getPathObjects(@Param("key") String key);

	@Query("MATCH (t:Test{key:{key}})-[r:HAS_TEST_RECORD]->(tr:TestRecord) MATCH a=(tr)-[:HAS_PAGE_STATE]->(p) RETURN a ORDER BY tr.ran_at DESC")
	public List<TestRecord> findAllTestRecords(@Param("key") String key);

	@Query("MATCH (t:Test)-[r:HAS_PATH_OBJECT]->(e:ElementState{key:{element_state_key}}) MATCH (t)-[]->(p:PageState{key:{page_state_key}}) RETURN t")
	public List<Test> findTestWithElementState(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	@Query("MATCH (t)-[r:HAS_PATH_OBJECT]->(p:PageState{key:{page_state_key}}) RETURN t")
	public List<Test> findTestWithPageState(@Param("page_state_key") String key);

	@Query("MATCH (:Test{key:{key}})-[:HAS_GROUP]-(g) RETURN g")
	public Set<Group> getGroups(@Param("key") String key);
	
	@Query("MATCH (t:Test{key:{key}}),(tr:TestRecord{key:{test_record_key}}) CREATE (t)-[r:HAS_TEST_RECORD]->(tr) RETURN r")
	public void addTestRecord(@Param("key") String key, @Param("test_record_key") String test_record_key);
	
	@Query("MATCH (:Test{key:{key}})-[:HAS_RESULT]->(p:PageState) RETURN p")
	public PageState getResult(@Param("key") String key);
	
	@Query("MATCH (t:Test) WHERE {path_obj_key} in t.path_keys RETURN t")
	public Set<Test> findAllTestRecordsContainingKey(@Param("path_obj_key") String path_object_key);
}
