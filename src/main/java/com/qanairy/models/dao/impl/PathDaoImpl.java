package com.qanairy.models.dao.impl;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.dao.PathDao;
import com.qanairy.models.dto.ActionRepository;
import com.qanairy.models.dto.Page;
import com.qanairy.models.dto.PageElement;
import com.qanairy.models.dto.PageElementRepository;
import com.qanairy.models.dto.PageRepository;
import com.qanairy.models.dto.PathObject;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.Path;
import com.qanairy.persistence.edges.PathEdge;

public class PathDaoImpl implements PathDao {

	@Override
	public Path save(Path path) {
		path.setKey(generateKey(path));
		
		
		@SuppressWarnings("unchecked")
		Iterator<IPath> path_iter = ((Iterable<IPath>) DataAccessObject.findByKey(path.getKey(), connection, IPath.class)).iterator();

		IPath path_record = null;
		
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(!path_iter.hasNext()){
			path_record = connection.getTransaction().addFramedVertex(Path.class);
			path_record.setKey(path.getKey());
		}
		else{
			path_record = path_iter.next();
		}

		IPathObject last_path_obj = null;
		int idx = -1;
		for(PathObject obj: path.getPath()){
			if(obj instanceof Page){
				PageRepository page_repo = new PageRepository();
				IPage persistablePathObj = page_repo.save(connection, (Page)obj);
				
				if(last_path_obj == null){
					path_record.setPath(persistablePathObj);
				}
				else{
					Iterable<PathEdge> edges = last_path_obj.getPathEdges();
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
				PageElementRepository page_elem_repo = new PageElementRepository();
				IPageElement persistablePathObj = page_elem_repo.save(connection, (PageElement)obj);

				if(last_path_obj == null){
					path_record.setPath(persistablePathObj);
				}
				else{
					PathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					path_edge.setPathKey(path.getKey());
					path_edge.setTransitionIndex(idx);
				}
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof Action){
				ActionRepository action_repo = new ActionRepository();
				Action persistablePathObj = action_repo.save(connection, (Action)obj);

				if(last_path_obj == null){
					path_record.setPath(persistablePathObj);
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
		path_record.setSpansMultipleDomains(path.getSpansMultipleDomains());
		return path_record;
	}

	@Override
	public Path find(String key) {
		// TODO Auto-generated method stub
		return null;
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
				PageDao page_dao = new PageDaoImpl();
				path_key += page_dao.generateKey((Page)obj) + "::";
				log.debug("Page key :: "+path_key);
			}
			else if(obj.getType().equals("PageElement")){
				PageElementRepository page_elem_record = new PageElementRepository();
				path_key += page_elem_record.generateKey((PageElement)obj) + "::";
				log.debug("Page element key:: "+path_key);
			}
			else if(obj.getType().equals("Action")){
				ActionRepository action_record = new ActionRepository();
				path_key += action_record.generateKey((Action)obj) + "::";
				log.debug("Action key :: "+path_key);
			}
		}
		
		return path_key;
	}
}
