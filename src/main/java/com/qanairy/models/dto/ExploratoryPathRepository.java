package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.actors.BrowserActor;
import com.minion.persistence.edges.IPathEdge;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.IPathObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PathRepository implements IPersistable<Path, IPath> {
    private static final Logger log = LoggerFactory.getLogger(BrowserActor.class);

	public IPath convertToRecord(OrientConnectionFactory connection, Path path) {
		path.setKey(generateKey(path));
		Iterable<IPath> paths = (Iterable<IPath>) DataAccessObject.findByKey(generateKey(path), connection, IPath.class);
		
		int cnt = 0;
		Iterator<IPath> iter = paths.iterator();
		IPath path_record = null;
		while(iter.hasNext()){
			iter.next();
			cnt++;
		}
		log.info("# of existing Path records with key "+path.getKey() + " :: "+cnt);
		
		if(cnt == 0){
			path_record = connection.getTransaction().addVertex("class:"+IPath.class.getCanonicalName()+","+UUID.randomUUID(), IPath.class);
			path_record.setKey(path.getKey());
		}
		else{
			path_record = paths.iterator().next();
		}

		boolean first_pass = true;
		IPathObject last_path_obj = null;

		for(PathObject obj: path.getPath()){
			log.info("setting data for last object");
			if(obj == null){
				break;
			}
			
			PathObjectRepository path_object_record = new PathObjectRepository();
			IPathObject persistablePathObj = path_object_record.convertToRecord(connection, obj);
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
		
		path_record.setUsefulness(path.isUseful());
		
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
		PathObjectRepository path_object_record = new PathObjectRepository();

		for(PathObject obj : path.getPath()){
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
	 * Path only allows you to update usefulness and if it spans multiple domains
	 */
	@Override
	public Path create(OrientConnectionFactory connection, Path path) {
		
		IPath path_record = find(connection, path.getKey());
		
		if(path_record == null){
			path_record = this.convertToRecord(connection, path);
			connection.save();
		}
		return path;
	}

	@Override
	public Path update(OrientConnectionFactory connection, Path path) {
		IPath path_record = find(connection, path.getKey());
		
		if(path != null){
			path_record.setSpansMultipleDomains(path.checkIfSpansMultipleDomains());
			path_record.setUsefulness(path.isUseful());
			connection.save();
		}
		return path;
	}

	@Override
	public Path convertFromRecord(IPath obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath find(OrientConnectionFactory connection, String key) {
		// TODO Auto-generated method stub
		return null;
	}
}