package com.qanairy.models.dao;

import com.qanairy.persistence.PageState;

/**
 * 
 */
public interface PageStateDao {
	public PageState save(PageState page);
	public PageState find(String key);
}
