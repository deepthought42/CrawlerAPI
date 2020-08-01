package com.qanairy.models;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

import com.qanairy.models.experience.PerformanceInsight;

/**
 * 
 */
public class Page extends LookseeObject{

	private String url;
	private String path;
	
	@Relationship(type = "HAS")
	private Set<PageState> page_states;
	
	@Deprecated
	@Relationship(type = "HAS")
	private List<PerformanceInsight> performance_insights;

	//following are deprecated in favor of the Audit concept being introduced. 6/18/2020
	@Deprecated
	private Double performance_score;
	@Deprecated
	private Double accessibility_score;
	@Deprecated
	private Double seo_score;
	@Deprecated
	private Double overall_score;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "page::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getUrl());
	}
	
	public Page() {
		super();
		setPerformanceInsights(new ArrayList<>());
		setPageStates( new HashSet<>() );
	}
	
	/**
	 * 
	 * @param url
	 * 
	 * @pre url != null
	 * @pre !url.isEmpty()
	 * 
	 * @throws MalformedURLException
	 */
	@Deprecated
	public Page(String url) throws MalformedURLException{
		super();
		assert url != null;
		assert !url.isEmpty();
				
		setPerformanceInsights(new ArrayList<>());
		setUrl(url);
		setPath(new URL(url).getPath());
		setPageStates( new HashSet<>() );
		setKey(generateKey());
	}
	
	/**
	 * 
	 * @param url
	 * 
	 * @pre url != null
	 * @pre !url.isEmpty()
	 * 
	 * @throws MalformedURLException
	 */
	public Page(String url, String path){
		super();
		assert url != null;
		assert !url.isEmpty();
		assert path != null;
				
		setPerformanceInsights(new ArrayList<>());
		setUrl(url);
		setPath(path);
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
	
	@Deprecated
	public List<PerformanceInsight> getPerformanceInsights() {
		return performance_insights;
	}
	
	@Deprecated
	public void setPerformanceInsights(List<PerformanceInsight> performance_insights) {
		this.performance_insights = performance_insights;
	}

	@Deprecated
	public void addPerformanceInsight(PerformanceInsight performance_insight) {
		this.performance_insights.add( performance_insight );
	}

	@Deprecated
	public Double getPerformanceScore() {
		return performance_score;
	}

	@Deprecated
	public void setPerformanceScore(Double score) {
		this.performance_score = score;
	}

	@Deprecated
	public Double getAccessibilityScore() {
		return accessibility_score;
	}

	@Deprecated
	public void setAccessibilityScore(Double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	@Deprecated
	public Double getSeoScore() {
		return seo_score;
	}

	@Deprecated
	public void setSeoScore(Double seo_score) {
		this.seo_score = seo_score;
	}

	@Deprecated
	public Double getOverallScore() {
		return overall_score;
	}

	@Deprecated
	public void setOverallScore(Double overall_score) {
		this.overall_score = overall_score;
	}
}
