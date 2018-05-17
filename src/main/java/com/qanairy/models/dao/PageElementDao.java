package com.qanairy.models.dao;

import com.qanairy.persistence.PageElement;

public interface PageElementDao {
	public void save(PageElement element);
	public PageElement find(String key);
}
