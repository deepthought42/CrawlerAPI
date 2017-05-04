package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.BrowserActor;
import com.minion.persistence.edges.IPathEdge;
import com.qanairy.models.ExploratoryPath;
import com.qanairy.models.Page;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ExploratoryPathRepository implements IPersistable<ExploratoryPath, IExploratoryPath> {
    private static final Logger log = LoggerFactory.getLogger(BrowserActor.class);

	public ExploratoryPath convertToRecord(OrientConnectionFactory connection, ExploratoryPath path) {
		path.setKey(generateKey(path));
		Iterable<IExploratoryPath> paths = (Iterable<IExploratoryPath>) DataAccessObject.findByKey(generateKey(path), connection, IExploratoryPath.class);
		
		int cnt = 0;
		Iterator<IExploratoryPath> iter = paths.iterator();
		IExploratoryPath path_record = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		log.info("# of existing ExploratoryPath records with key "+path.getKey() + " :: "+cnt);
		
		if(cnt == 0){
			path_record = connection.getTransaction().addVertex("class:"+IExploratoryPath.class.getSimpleName()+","+UUID.randomUUID(), IExploratoryPath.class);
			path_record.setKey(path.getKey());
		}
		else{
			path_record = paths.iterator().next();
		}

		boolean first_pass = true;
		IExploratoryPathObject last_path_obj = null;

		for(ExploratoryPathObject obj: path.getExploratoryPath()){
			log.info("setting data for last object");
			if(obj == null){
				break;
			}
			
			ExploratoryPathObjectRepository path_object_record = new ExploratoryPathObjectRepository();
			IExploratoryPathObject persistableExploratoryPathObj = path_object_record.convertToRecord(connection, obj);
			if(first_pass){
				log.info("First object detected : "+persistableExploratoryPathObj.getClass());
				path_record.setExploratoryPath(persistableExploratoryPathObj);
				
				log.info("adding object to path");
				first_pass = false;
			}
			else{
				log.info("setting next object in path using IExploratoryPathEdge");
				IExploratoryPathEdge path_edge = last_path_obj.addExploratoryPathEdge(persistableExploratoryPathObj);
				
				log.info("Setting path key on IExploratoryPathEdge");
				path_edge.setExploratoryPathKey(path.getKey());
			}
			last_path_obj = persistableExploratoryPathObj;
		}
		
		path_record.setUsefulness(path.isUseful());
		
		log.info("Is spans multiple domains set : " + path.getSpansMultipleDomains());
		path_record.setSpansMultipleDomains(path.getSpansMultipleDomains());
		return path_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(ExploratoryPath path) {
		String path_key = "";
		ExploratoryPathObjectRepository path_object_record = new ExploratoryPathObjectRepository();

		for(ExploratoryPathObject obj : path.getExploratoryPath()){
			if(obj == null){
				break;
			}
			path_key += path_object_record.generateKey(obj) + ":"+hashCode()+":";
		}

		return path_key;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * ExploratoryPath only allows you to update usefulness and if it spans multiple domains
	 */
	@Override
	public ExploratoryPath create(OrientConnectionFactory connection, ExploratoryPath path) {
		
		IExploratoryPath path_record = find(connection, path.getKey());
		
		if(path_record == null){
			path_record = this.convertToRecord(connection, path);
			connection.save();
		}
		return path;
	}

	@Override
	public ExploratoryPath update(OrientConnectionFactory connection, ExploratoryPath path) {
		ExploratoryPath path_record = find(connection, path.getKey());
		
		if(path != null){
			path_record.setSpansMultipleDomains(path.checkIfSpansMultipleDomains());
			path_record.setUsefulness(path.isUseful());
			connection.save();
		}
		return path;
	}

	@Override
	public static Path convertFromRecord(IPath ipath) {
		Path path = new Path();
		log.info("converting path record to object");
		path.setIsUseful(ipath.isUseful());
		
		String path_key = ipath.getKey();
		log.info("Path key for path Object :: "+path_key);
		
		log.info("setting key");
		path.setKey(path_key);
			
		log.info("setting if spans multiple domains");
		path.setSpansMultipleDomains(ipath.isSpansMultipleDomains());
		
		log.info("getting initial path vertex in path");
		IPathObject path_obj = ipath.getPath();
		
		//Page page = new Page();
		Iterator<IPage> ipage = (Iterator<IPage>) DataAccessObject.findByKey(ipath.getPath().getKey(), IPage.class).iterator();
		//path.setPath(new ArrayList<PathObject>());
		log.info("page found");
		Page page = Page.convertFromRecord(ipage.next());
		path.getPath().add(page);
		page.setType(Page.class.getSimpleName());

		int count = 0;
		while(path_obj.getNext() != null){
			log.info("Path object is being observed "+path_obj);
			int matching_edge_cnt = 0;
			Iterator<IPathEdge> path_edge = path_obj.getPathEdges().iterator();
			
			while(path_edge.hasNext()){
				IPathEdge next_path_edge = path_edge.next();
				String key = next_path_edge.getPathKey();
				log.info("Observing edge with key " + key);

				if(key.equals(path_key)){
					log.info("Edge with path key located");
					IPathObject path_obj_out = next_path_edge.getPathObjectOut();
					
					log.info("looping through  page elements and adding them to path object " + count);
					PathObject this_path_obj = PathObject.convertFromRecord(path_obj_out);
					log.info("retrieved path object : " + this_path_obj);
					path.add(this_path_obj);
					matching_edge_cnt++;
					break;
				}
			}

			if(matching_edge_cnt == 0){
				break;
			}
			PathObject this_path_obj = PathObject.convertFromRecord(path_obj.getNext());
			log.info("retrieved path object : " + this_path_obj);
			path.add(this_path_obj);
			path_obj = path_obj.getNext();
			
			count++;
		}

		log.info("PATH OBJECT NEXT :: "+path_obj.getNext());
		/*while(path_obj != null && path_obj.getNext() != null){
			log.info("looping through  page elements and adding them to path object");
			PathObject this_path_obj = PathObject.convertFromRecord(path_obj.getNext());
			log.info("retrieved path object : " + this_path_obj);
			path.add(this_path_obj);
			path_obj = path_obj.getNext();
		}
		*/
		//log.info("path object type : " + path_obj.getType());
		//log.info("path object canonical class name : " + path_obj.getClass().getCanonicalName());
		log.info("building path object record");
		
		return path;
	}

	@Override
	public ExploratoryPath find(OrientConnectionFactory connection, String key) {
		// TODO Auto-generated method stub
		return null;
	}
}