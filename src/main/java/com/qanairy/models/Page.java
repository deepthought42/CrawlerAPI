package com.qanairy.models;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Page implements Persistable{

	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private String url;
	private String path;
	
	@Relationship(type = "HAS")
	private Set<PageState> page_states;

	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(getUrl());
	}
	
	public Page(String url){
		setUrl(url);
		setKey(generateKey());
		setPageStates(new HashSet<PageState>());
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

	public Set<PageState> getPageStates() {
		return page_states;
	}

	public void setPageStates(Set<PageState> page_states) {
		this.page_states = page_states;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
