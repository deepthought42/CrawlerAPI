package com.looksee.models.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.Screenshot;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.PageAuditRecord;

/**
 * 
 */
@Repository
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKeyAndUsername(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKey(@Param("key") String key);

	@Deprecated
	@Query("MATCH (:Account{username:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH a=(p)-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumsContainsForUserAndDomain(@Param("user_id") String user_id, @Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{url:$url})-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumAndPageUrl(@Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{full_page_checksum:$screenshot_checksum}) MATCH a=(p)-[h:HAS_CHILD]->() RETURN a")
	public List<PageState> findByFullPageScreenshotChecksum(@Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[*]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStatesForUser(@Param("user_id") String user_id, @Param("page_key") String key);

	@Query("MATCH (p:PageState{key:$page_key})-[]->(e:ElementState) RETURN e")
	public List<ElementState> getElementStates(@Param("page_key") String key);

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[*]->(e:ElementState{name:'a'}) RETURN e")
	public List<ElementState> getLinkElementStates(@Param("user_id") String user_id, @Param("page_key") String key);

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("page_key") String page_key);

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) WHERE $screenshot_checksum IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("user_id") String user_id, @Param("screenshot_checksum") String screenshot_checksum);

	@Query("MATCH (a:Account{username:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(f:Form{key:$form_key}) RETURN p")
	public List<PageState> findPageStatesWithForm(@Param("user_id") String user_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (d:Domain{url:$url})-[:HAS]->(ps:PageState{src_checksum:$src_checksum}) MATCH a=(ps)-[h:HAS]->() RETURN a")
	public List<PageState> findBySourceChecksumForDomain(@Param("url") String url, @Param("src_checksum") String src_checksum);
	
	@Query("MATCH (ps:PageState{key:$page_state_key})<-[]-(a:Audit) RETURN a")
	public List<Audit> getAudits(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(a:Audit{subcategory:$subcategory}) RETURN a")
	public Audit findAuditBySubCategory(@Param("subcategory") String subcategory, @Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[:HAS]->(e:ElementState{classification:'leaf'}) where e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_key") String page_state_key);

	@Query("ps:PageState{key:$page_state_key}) return p LIMIT 1")
	public PageState getParentPage(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{url:$url}) RETURN p ORDER BY p.created_at DESC LIMIT 1")
	public PageState findByUrl(@Param("url") String url);

	@Query("MATCH (p:PageState),(element:ElementState{key:$element_key}) WHERE id(p)=$page_id CREATE (p)-[h:HAS]->(element) RETURN element")
	public ElementState addElement(@Param("page_id") long page_id, @Param("element_key") String element_key);

	@Query("MATCH (p:PageState)-[]->(element:ElementState{key:$element_key}) WHERE id(p)=$page_id RETURN element")
	public Optional<ElementState> getElementState(@Param("page_id") long page_id, @Param("element_key") String element_key);

	@Query("MATCH (a:PageAuditRecord)-[:HAS]->(ps:PageState) WHERE id(ps)=$id RETURN a ORDER BY a.created_at DESC LIMIT 1")
	public PageAuditRecord getAuditRecord(@Param("id") long id);
}
