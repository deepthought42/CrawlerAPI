package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import com.qanairy.persistence.edges.IPathEdge;
import com.orientechnologies.common.io.OIOException;
import com.qanairy.models.Action;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAction;
import com.qanairy.persistence.IPage;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.IPathObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PathRepository implements IPersistable<Path, IPath> {
	private static Logger log = LoggerFactory.getLogger(PathRepository.class);

	public IPath save(OrientConnectionFactory connection, Path path) {
		if(path.getKey() == null || path.getKey().length()==0){
			String path_key = generateKey(path);
			path.setKey(path_key);
		}
		@SuppressWarnings("unchecked")
		Iterator<IPath> path_iter = ((Iterable<IPath>) DataAccessObject.findByKey(path.getKey(), connection, IPath.class)).iterator();

		IPath path_record = null;
		
		if(!path_iter.hasNext()){
			path_record = connection.getTransaction().addVertex("class:"+IPath.class.getSimpleName()+","+UUID.randomUUID(), IPath.class);
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
					Iterable<IPathEdge> edges = last_path_obj.getPathEdges();
					boolean path_edge_exists = false;
					for(IPathEdge edge : edges){
						if(edge.getPathKey().equals(path.getKey())){
							path_edge_exists = true;
							break;
						}
					}
					
					if(!path_edge_exists){
						IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
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
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					path_edge.setPathKey(path.getKey());
					path_edge.setTransitionIndex(idx);
				}
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof Action){
				ActionRepository action_repo = new ActionRepository();
				IAction persistablePathObj = action_repo.save(connection, (Action)obj);

				if(last_path_obj == null){
					path_record.setPath(persistablePathObj);
				}
				else{
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Path path) {
		String path_key = "";
		
		for(PathObject obj : path.getPath()){
			if(obj == null){
				continue;
			}
			if(obj.getType().equals("Page")){
				PageRepository page_record = new PageRepository();
				path_key += page_record.generateKey((Page)obj) + "::";
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

	/**
	 * {@inheritDoc}
	 * 
	 * Path only allows you to update usefulness and if it spans multiple domains
	 */
	@Override
	public Path create(OrientConnectionFactory connection, Path path) {
		this.save(connection, path);
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path update(OrientConnectionFactory connection, Path path) {
		Path path_record = find(connection, path.getKey());
		
		if(path != null){
			path_record.setSpansMultipleDomains(path.checkIfSpansMultipleDomains());
			path_record.setIsUseful(path.isUseful());
			connection.save();
		}
		return path;
	}

	/**
	 * @pre path != null
	 */
	@Override
	public Path load(IPath path) throws NullPointerException {
		assert(path != null);
		
		IPathObject path_obj = path.getPath();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		String key = path.getKey();
		String last_path_obj_key = null;
		int idx = 0;

		while(path_obj != null && path_obj.getPathEdges() != null
				&& (last_path_obj_key == null || !last_path_obj_key.equals(path_obj.getKey()))){
			Iterator<IPathEdge> path_edges = path_obj.getPathEdges().iterator();
			last_path_obj_key = path_obj.getKey();
			PathObjectRepository path_obj_repo = new PathObjectRepository();
			path_obj_list.add(path_obj_repo.load(path_obj));

			while(path_edges.hasNext()){
				IPathEdge edge = path_edges.next();
				
				if(edge != null && edge.getPathKey().equals(key) && edge.getTransitionIndex()==idx){
					path_obj = edge.getPathObjectIn();
					break;
				}
			}
			idx++;

		}
		
		return new Path(key, path.isUseful(), path.isSpansMultipleDomains(), path_obj_list);
	}

	@Override
	public Path find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(key, connection, IPath.class);
		Iterator<IPath> iter = paths.iterator();
		
		if(iter.hasNext()){
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public List<Path> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}