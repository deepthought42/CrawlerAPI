package com.qanairy.models.dao.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.ActionDao;
import com.qanairy.models.dao.PageElementDao;
import com.qanairy.models.dao.PageStateDao;
import com.qanairy.models.dao.PathDao;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.Path;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.edges.PathEdge;

public class PathDaoImpl implements PathDao {
	private static Logger log = LoggerFactory.getLogger(PathDao.class);
	
	@Override
	public Path save(Path path) {
		path.setKey(generateKey(path));
		OrientConnectionFactory connection = new OrientConnectionFactory();

		Path path_record = find(path.getKey());
		
		if(path_record == null){
			path_record = connection.getTransaction().addFramedVertex(Path.class);
			path_record.setKey(path.getKey());
		}

		PathObject last_path_obj = null;
		int idx = -1;
		for(PathObject obj: path.getPath()){
			if(obj instanceof PageState){
				PageStateDao page_repo = new PageStateDaoImpl();
				PageState persistablePathObj = page_repo.save((PageState)obj);
				
				if(last_path_obj == null){
					path_record.setPathStartsWith(persistablePathObj);
				}
				else{
					Iterable<? extends PathEdge> edges = last_path_obj.getPathEdges();
					boolean path_edge_exists = false;
					for(PathEdge edge : edges){
						if(edge.getPathKey().equals(path.getKey())){
							path_edge_exists = true;
							break;
						}
					}
					
					if(!path_edge_exists){
						PathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
						path_edge.setPathKey(path.getKey());
						path_edge.setTransitionIndex(idx);
					}
				}
				
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof PageElement){
				PageElementDao page_elem_repo = new PageElementDaoImpl();
				PathObject persistablePathObj = page_elem_repo.save((PageElement)obj);

				if(last_path_obj == null){
					path_record.setPathStartsWith(persistablePathObj);
				}
				else{
					PathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					path_edge.setPathKey(path.getKey());
					path_edge.setTransitionIndex(idx);
				}
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof Action){
				ActionDao action_repo = new ActionDaoImpl();
				Action persistablePathObj = action_repo.save((Action)obj);

				if(last_path_obj == null){
					path_record.setPathStartsWith(persistablePathObj);
				}
				else{
					PathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					path_edge.setPathKey(path.getKey());
					path_edge.setTransitionIndex(idx);
				}
				
				last_path_obj = persistablePathObj;
			}
			
			idx++;
		}
		
		path_record.setIsUseful(path.isUseful());
		path_record.setSpansMultipleDomains(path.getIfSpansMultipleDomains());
		return path_record;
	}

	@Override
	public Path find(String key) {
		Path path = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			path = connection.getTransaction().getFramedVertices("key", key, Path.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Path path) {
		String path_key = "";
		
		for(PathObject obj : path.getPath()){
			if(obj == null){
				continue;
			}
			if(obj.getType().equals("Page")){
				PageStateDaoImpl page_dao = new PageStateDaoImpl();
				path_key += page_dao.generateKey((PageState)obj) + "::";
				log.debug("Page key :: "+path_key);
			}
			else if(obj.getType().equals("PageElement")){
				PageElementDaoImpl page_elem_dao = new PageElementDaoImpl();
				path_key += page_elem_dao.generateKey((PageElement)obj) + "::";
				log.debug("Page element key:: "+path_key);
			}
			else if(obj.getType().equals("Action")){
				ActionDaoImpl action_dao = new ActionDaoImpl();
				path_key += action_dao.generateKey((Action)obj) + "::";
				log.debug("Action key :: "+path_key);
			}
		}
		
		return path_key;
	}
}
