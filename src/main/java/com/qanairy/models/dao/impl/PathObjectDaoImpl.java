package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.dao.ActionDao;
import com.qanairy.models.dao.PageElementDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.PathObjectDao;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;

/**
 * 
 */
public class PathObjectDaoImpl implements PathObjectDao {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PathObjectDaoImpl.class);
	
	/**
	 * {@inheritDoc}
	 */
	public String generateKey(PathObject path_obj) {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public PathObject save(PathObject path_obj) {		
		if(path_obj instanceof PageState){
			PathObject page = (PageState)path_obj;
			PageStateDao page_dao = new PageStateDaoImpl();
			path_obj = page_dao.save((PageState)page);
		}
		else if(path_obj instanceof PageElement){
			PageElementDao page_element_dao = new PageElementDaoImpl();
			path_obj = page_element_dao.save((PageElement)path_obj);
		}
		else if(path_obj instanceof Action){
			System.err.println("Saving action...");
			ActionDao action_dao = new ActionDaoImpl();
			path_obj = action_dao.save((Action)path_obj);
		}
		return path_obj;
	}

	@Override
	public PathObject find(String key) {
		PathObject path_obj = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			path_obj = connection.getTransaction().getFramedVertices("key", key, PathObject.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting path object record from database");
		}
		connection.close();
		return path_obj;
	}

}
