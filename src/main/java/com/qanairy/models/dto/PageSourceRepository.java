package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.PageSource;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPageSource;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;


public class PageSourceRepository implements IPersistable<PageSource, IPageSource> {
	private static Logger log = LoggerFactory.getLogger(PageSourceRepository.class);

	@Override
	public String generateKey(PageSource page_src) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(page_src.getSrc())+":1";   
	}

	@Override
	public IPageSource convertToRecord(OrientConnectionFactory connection, PageSource page_src) {
		if(page_src.getKey() == null || page_src.getKey().isEmpty() && page_src.getSrc() != null){
			page_src.setKey(generateKey(page_src));
		}
		
		@SuppressWarnings("unchecked")
		Iterator<IPageSource> pages_iter = ((Iterable<IPageSource>) DataAccessObject.findByKey(page_src.getKey(), connection, IPageSource.class)).iterator();
		
		IPageSource page_src_record = null;
		if(pages_iter.hasNext()){
			page_src_record = pages_iter.next();
		}
		else{
			page_src_record = connection.getTransaction().addVertex("class:"+IPageSource.class.getSimpleName()+","+UUID.randomUUID(), IPageSource.class);
			page_src_record.setKey(page_src.getKey());
			page_src_record.setSrc(page_src.getSrc());
		}

		return page_src_record;
	}

	@Override
	public PageSource convertFromRecord(IPageSource page_src_record) {
		return new PageSource(page_src_record.getKey(), page_src_record.getSrc());
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
