package com.qanairy.models;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.experience.PerformanceInsight;

/**
 * 
 */
@NodeEntity
public class Page implements Persistable{

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String url;
	private String path;
	private Double performance_score;
	private Double accessibility_score;
	private Double seo_score;
	private Double overall_score;

	@Relationship(type = "HAS")
	private List<PerformanceInsight> performance_insights;
	
	@Relationship(type = "HAS")
	private List<AuditRecord> audit_records;

	@Relationship(type = "HAS")
	private Set<PageState> page_states;

	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getUrl());
	}
	
	public Page() {
		setPerformanceInsights(new ArrayList<>());
		setPageStates( new HashSet<>() );
	}
	
	public Page(String url) throws MalformedURLException{
		setPerformanceInsights(new ArrayList<>());
		setUrl(url);
		setPath(new URL(url).getPath());
		setPageStates( new HashSet<>() );
		setKey(generateKey());
	}
	
	
	public Set<PageState> getPageStates() {
		return page_states;
	}
	
	public void setPageStates(Set<PageState> page_states) {
		this.page_states = page_states;
	}

	public boolean addPageState(PageState page_state) {
		return page_states.add(page_state);
	}
	
	/**
	 * Checks if Pages are equal
	 * 
	 * @param page
	 *            the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Page))
			return false;

		Page that = (Page) o;	
		
		return this.getUrl().equals(that.getUrl());
	}
	
	//GETTERS AND SETTERS

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public List<PerformanceInsight> getPerformanceInsights() {
		return performance_insights;
	}

	public void setPerformanceInsights(List<PerformanceInsight> performance_insights) {
		this.performance_insights = performance_insights;
	}

	public void addPerformanceInsight(PerformanceInsight performance_insight) {
		this.performance_insights.add( performance_insight );
	}
	
	public List<AuditRecord> getAuditRecords() {
		return audit_records;
	}

	public void setAuditRecords(List<AuditRecord> audit_records) {
		this.audit_records = audit_records;
	}

	public void addAuditRecord(AuditRecord audit_record) {
		this.audit_records.add( audit_record );
	}
	
	public long getId(){
		return this.id;
	}

	public Double getPerformanceScore() {
		return performance_score;
	}

	public void setPerformanceScore(Double score) {
		this.performance_score = score;
	}

	public Double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(Double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	public Double getSeoScore() {
		return seo_score;
	}

	public void setSeoScore(Double seo_score) {
		this.seo_score = seo_score;
	}

	public Double getOverallScore() {
		return overall_score;
	}

	public void setOverallScore(Double overall_score) {
		this.overall_score = overall_score;
	}
}
