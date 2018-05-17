package com.qanairy.models.dao;

import java.util.Iterator;
import com.qanairy.persistence.Rule;

/**
 * 
 */
public interface RuleDao {
	public Rule save(Rule rule);
	public Rule find(String key);
	public Iterator<? extends Rule> findAll();
}
