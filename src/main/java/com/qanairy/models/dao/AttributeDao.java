package com.qanairy.models.dao;

import com.qanairy.persistence.Attribute;

/**
 * Defines how a persisted {@link Attribute} can be interacted with
 */
public interface AttributeDao {
	public void save(Attribute attribute);
	public Attribute find(String key);
}
