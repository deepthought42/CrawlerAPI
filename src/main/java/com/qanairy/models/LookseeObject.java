package com.qanairy.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Universal object that contains values that are expected to exist on all persistable objects within the database
 * @author brand
 *
 */
@NodeEntity
public abstract class LookseeObject {
	
	@GeneratedValue
    @Id
	private Long id;
	
	private String key;
	private LocalDateTime created_at;
	
	public LookseeObject() {
		setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
	}
	
	public LookseeObject(String key) {
		setKey(key);
		setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}
	
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public abstract String generateKey();

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public LocalDateTime getCreatedAt() {
		return created_at;
	}

	public void setCreatedAt(LocalDateTime created_at) {
		this.created_at = created_at;
	}
}
