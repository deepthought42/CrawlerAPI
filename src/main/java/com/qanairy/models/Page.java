package com.qanairy.models;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.experience.PerformanceInsight;

@NodeEntity
public class Page implements Persistable{

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String url;
	private String path;
	
	@Relationship(type = "HAS")
	private List<Template> templates;

	@Relationship(type = "HAS")
	private List<PerformanceInsight> performance_insights;

	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getUrl());
	}
	
	public Page() {}
	
	public Page(String url){
		setTemplates(new ArrayList<>());
		setPerformanceInsights(new ArrayList<>());
		setUrl(url);
		setKey(generateKey());
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

	public List<Template> getTemplates() {
		return templates;
	}

	public void setTemplates(List<Template> templates) {
		this.templates = templates;
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
	
	public long getId(){
		return this.id;
	}
}
