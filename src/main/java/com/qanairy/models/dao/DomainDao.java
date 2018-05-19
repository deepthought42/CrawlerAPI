package com.qanairy.models.dao;

import com.qanairy.persistence.Domain;

/**
 * 
 */
public interface DomainDao {
	public Domain save(Domain domain);
	public Domain find(String key);
}
