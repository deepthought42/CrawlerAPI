package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.minion.persistence.edges.IPathEdge;
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
	private static Logger log = Logger.getLogger(PathRepository.class);

	public IPath convertToRecord(OrientConnectionFactory connection, Path path) {
		path.setKey(generateKey(path));
		Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(path.getKey(), connection, IPath.class);
		
		Iterator<IPath> iter = paths.iterator();
		IPath path_record = null;

		log.info("# of existing Path records with key "+path.getKey() + " :: " + path.getPath().size());
		
		if(!iter.hasNext()){
			path_record = connection.getTransaction().addVertex("class:"+IPath.class.getSimpleName()+","+UUID.randomUUID(), IPath.class);
			path_record.setKey(path.getKey());
		}
		else{
			path_record = iter.next();
		}

		boolean first_pass = true;
		IPathObject last_path_obj = null;

		for(PathObject obj: path.getPath()){
			log.info("setting data for last object");
			if(obj == null){
				break;
			}
			
			if(obj instanceof Page){
				PageRepository page_repo = new PageRepository();
				IPage persistablePathObj = page_repo.convertToRecord(connection, (Page)obj);
				
				if(first_pass){
					log.info("First object detected : "+persistablePathObj.getClass());
					path_record.setPath(persistablePathObj);
					
					log.info("adding object to path");
					first_pass = false;
				}
				else{
					log.info("setting next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					log.info("Setting path key on IPathEdge");
					path_edge.setPathKey(path.getKey());
				}
				
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof PageElement){
				PageElementRepository page_elem_repo = new PageElementRepository();
				IPageElement persistablePathObj = page_elem_repo.convertToRecord(connection, (PageElement)obj);

				if(first_pass){
					log.info("First object detected : "+persistablePathObj.getClass());
					path_record.setPath(persistablePathObj);
					
					log.info("adding object to path");
					first_pass = false;
				}
				else{
					log.info("setting next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					log.info("Setting path key on IPathEdge");
					path_edge.setPathKey(path.getKey());
				}
				last_path_obj = persistablePathObj;
			}
			if(obj instanceof Action){
				ActionRepository action_repo = new ActionRepository();
				IAction persistablePathObj = action_repo.convertToRecord(connection, (Action)obj);

				if(first_pass){
					log.info("First object detected : "+persistablePathObj.getClass());
					path_record.setPath(persistablePathObj);
					
					log.info("adding object to path");
					first_pass = false;
				}
				else{
					log.info("setting next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					log.info("Setting path key on IPathEdge");
					path_edge.setPathKey(path.getKey());
				}
				
				last_path_obj = persistablePathObj;
			}
			
			
			//last_path_obj = persistablePathObj;
			
		}
		
		path_record.setIsUseful(path.isUseful());
		
		log.info("Is spans multiple domains set : " + path.getSpansMultipleDomains());
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
			if(obj instanceof Page){
				PageRepository page_record = new PageRepository();
				path_key += page_record.generateKey((Page)obj) + "::";

			}
			else if(obj instanceof PageElement){
				PageElementRepository page_elem_record = new PageElementRepository();
				path_key += page_elem_record.generateKey((PageElement)obj) + "::";
			}
			else if(obj instanceof Action){
				ActionRepository action_record = new ActionRepository();
				path_key += action_record.generateKey((Action)obj) + "::";
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
		
		Path path_tmp = find(connection, path.getKey());
		
		if(path_tmp == null){
			this.convertToRecord(connection, path);
			connection.save();
		}
		return path;
	}

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

	@Override
	public Path convertFromRecord(IPath obj) {
		IPathObject path_obj = obj.getPath();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		while(path_obj != null){
			Iterator<IPathEdge> path_edges = path_obj.getPathEdges().iterator();
			while(path_edges.hasNext()){
				IPathEdge edge = path_edges.next();
				if(edge.getPathKey().equals(obj.getKey()) ){
					PathObjectRepository path_obj_repo = new PathObjectRepository();
					path_obj_list.add(path_obj_repo.convertFromRecord(path_obj));
					path_obj = edge.getPathObjectOut();
					break;
				}
			}
		}
		
		return new Path(obj.getKey(), obj.isUseful(), obj.isSpansMultipleDomains(), path_obj_list);
	}

	@Override
	public Path find(OrientConnectionFactory connection, String key) {
			Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(key, connection, IPath.class);
			Iterator<IPath> iter = paths.iterator();
			
			if(iter.hasNext()){
				return convertFromRecord(iter.next());
			}
			return null;
	}

	@Override
	public List<Path> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}