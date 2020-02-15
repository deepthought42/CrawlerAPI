package com.qanairy.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 */
@NodeEntity
public class PathSnippet implements Persistable {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathSnippet.class);
	
    @GeneratedValue
    @Id
	private Long id;
	
	private List<String> path_keys;
	private Date created_at;
	private boolean leaves_domain;
	
	@JsonIgnore
	@Relationship(type = "HAS")
	private List<PathObject> path_objects;
	
	PathSnippet(){
		setPathObjects(new ArrayList<>());
	}

	public List<PathObject> getPathObjects() {
		return this.path_objects;
	}
	
	public boolean addPathObject(PathObject obj) {
		return getPathObjects().add(obj);
	}
	
	private void setPathObjects(ArrayList<PathObject> path_object_list) {
		this.path_objects = path_object_list;
	}

	public List<String> getPathKeys() {
		return path_keys;
	}

	public void setPathKeys(List<String> path_keys) {
		this.path_keys = path_keys;
	}

	public Date getCreatedAt() {
		return created_at;
	}

	public void setCreatedAt(Date created_at) {
		this.created_at = created_at;
	}

	public boolean isLeavesDomain() {
		return leaves_domain;
	}

	public void setLeavesDomain(boolean leaves_domain) {
		this.leaves_domain = leaves_domain;
	}
	
	
}
