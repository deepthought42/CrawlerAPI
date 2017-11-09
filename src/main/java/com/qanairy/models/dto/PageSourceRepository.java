package com.qanairy.models.dto;

import java.util.List;

import com.qanairy.models.PageSource;
import com.qanairy.persistence.IPersistable;


public class PageSourceRepository implements IPersistable<PageSource, IPageSource> {

	@Override
	public String generateKey(PageSource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPageSource convertToRecord(OrientConnectionFactory connection, PageSource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageSource convertFromRecord(IPageSource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageSource create(OrientConnectionFactory connection, PageSource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageSource update(OrientConnectionFactory connection, PageSource obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageSource find(OrientConnectionFactory connection, String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PageSource> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

}
