package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static Logger log = LogManager.getLogger(PathRepository.class);

	public IPath convertToRecord(OrientConnectionFactory connection, Path path) {
		String path_key = generateKey(path);
		path.setKey(path_key);
		
		Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(path_key, connection, IPath.class);
		
		Iterator<IPath> iter = paths.iterator();
		IPath path_record = null;

		System.err.println("# of existing Path records with key "+path.getKey() + " :: " + path.getPath().size());
		
		if(!iter.hasNext()){
			path_record = connection.getTransaction().addVertex("class:"+IPath.class.getSimpleName()+","+UUID.randomUUID(), IPath.class);
			path_record.setKey(path_key);
		}
		else{
			path_record = iter.next();
		}

		IPathObject last_path_obj = null;
		
		//NEED TO EXCHANGE PATH.getKey() for generating key 
		for(PathObject obj: path.getPath()){
			if(obj instanceof Page){
				PageRepository page_repo = new PageRepository();
				IPage persistablePathObj = page_repo.convertToRecord(connection, (Page)obj);
				
				if(last_path_obj == null){
					System.err.println("First object detected : "+persistablePathObj.getType());
					path_record.setPath(persistablePathObj);
				}
				else{
					System.err.println("setting page next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					System.err.println("Setting path key on IPathEdge :: "+path_key );
					path_edge.setPathKey(path_key);
				}
				
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof PageElement){
				PageElementRepository page_elem_repo = new PageElementRepository();
				IPageElement persistablePathObj = page_elem_repo.convertToRecord(connection, (PageElement)obj);

				if(last_path_obj == null){
					System.err.println("First object detected : "+persistablePathObj.getClass());
					path_record.setPath(persistablePathObj);
				}
				else{
					System.err.println("setting page element as next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					System.err.println("Setting path key on IPathEdge :: "+path_key);
					path_edge.setPathKey(path_key);
				}
				last_path_obj = persistablePathObj;
			}
			else if(obj instanceof Action){
				ActionRepository action_repo = new ActionRepository();
				IAction persistablePathObj = action_repo.convertToRecord(connection, (Action)obj);

				if(last_path_obj == null){
					System.err.println("First object detected : "+persistablePathObj.getClass());
					path_record.setPath(persistablePathObj);
				}
				else{
					System.err.println("setting Action as next object in path using IPathEdge");
					IPathEdge path_edge = last_path_obj.addPathEdge(persistablePathObj);
					
					System.err.println("Setting path key on Action IPathEdge :: "+path_key);
					path_edge.setPathKey(path_key);
				}
				
				last_path_obj = persistablePathObj;
			}
		}
		
		path_record.setIsUseful(path.isUseful());
		
		System.err.println("Is spans multiple domains set : " + path.getSpansMultipleDomains());
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
			if(obj.getType().equals("Page")){
				PageRepository page_record = new PageRepository();
				path_key += page_record.generateKey((Page)obj) + "::";
				System.err.println("Page key :: "+path_key);
			}
			else if(obj.getType().equals("PageElement")){
				PageElementRepository page_elem_record = new PageElementRepository();
				path_key += page_elem_record.generateKey((PageElement)obj) + "::";
				System.err.println("Page element key:: "+path_key);
			}
			else if(obj.getType().equals("Action")){
				ActionRepository action_record = new ActionRepository();
				path_key += action_record.generateKey((Action)obj) + "::";
				System.err.println("Action key :: "+path_key);
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

	@Override
	public Path convertFromRecord(IPath obj) {
		IPathObject path_obj = obj.getPath();
		List<PathObject> path_obj_list = new ArrayList<PathObject>();
		int index = 0;
		IPathObject last_path_obj = null;
		while(path_obj != null && (last_path_obj == null || !last_path_obj.getKey().equals(path_obj.getKey()))){
			Iterator<IPathEdge> path_edges = path_obj.getPathEdges().iterator();
			System.err.println("getting path edges");
			last_path_obj = path_obj;
			PathObjectRepository path_obj_repo = new PathObjectRepository();
			path_obj_list.add(path_obj_repo.convertFromRecord(path_obj));
			while(path_edges.hasNext()){
				index++;
				IPathEdge edge = path_edges.next();
				System.err.println("retrieving next edge on iteration :: "+index + " :: with key == "+ edge.getPathKey());
				if(edge != null && edge.getPathKey().equals(obj.getKey()) ){
					System.err.println("Edge key matches object key");

					path_obj = edge.getPathObjectIn();
					System.err.println("Path obj in :: "+edge.getPathObjectIn());
					break;
				}
			}
		}
		
		for(PathObject path_obj2 : path_obj_list){
			if(path_obj2 == null){
				continue;
			}
			System.err.println("Path Object Type : "+path_obj2.getType());
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