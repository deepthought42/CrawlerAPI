package com.qanairy.models.dao;

import com.qanairy.persistence.Attribute;

/**
 * Defines how a persisted {@link Attribute} can be interacted with
 */
public interface AttributeDao {
	public Attribute save(Attribute attribute);
	public Attribute find(String key);
}
