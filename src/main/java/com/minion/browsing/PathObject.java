package com.minion.browsing;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.persistence.IPath;
import com.minion.persistence.IPathObject;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;

/**
 * An object wrapper that allows data to be dynamically placed in data structures
 * 
 * @author Brandon Kindred
 * @param <V>
 *
 */
public class PathObject<V> implements IPersistable<IPathObject>{
    private static final Logger log = LoggerFactory.getLogger(PathObject.class);

	private V data = null;
	private PathObject<?> next = null;
	
	public PathObject(V data){
		this.data=data;
	}
	
	/**
	 * Returns wrapped object
	 * @return
	 */
	public V getData(){
		return this.data;
	}
	

	/**
	 * Sets data for wrapper
	 * @return
	 */
	public void setData(V data){
		this.data = data;
	}

	/**
	 * Returns wrapped object
	 * @return
	 */
	public PathObject<?> getNext(){
		return this.next;
	}
	

	/**
	 * Sets data for wrapper
	 * @return
	 */
	public void setNext(PathObject<?> next){
		this.next = next;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPathObject convertToRecord(OrientConnectionFactory connection) {
		IPath path = connection.getTransaction().addVertex("class:IPathObject,"+UUID.randomUUID(), IPath.class);
		path.setKey(this.generateKey());
		
		log.info("Starting conversion from path objects to their respective types");
		IPathObject persistablePathObj =  connection.getTransaction().addVertex("class:IPathObject,"+UUID.randomUUID(), IPathObject.class);
		persistablePathObj.setData(this.getData());
		persistablePathObj.setNext(this.getNext().convertToRecord(connection));
		return persistablePathObj;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IPathObject> create() {
		Iterator<IPathObject> path_obj_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(path_obj_iter.hasNext()){
			path_obj_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.generateKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(cnt == 0){
			connection.getTransaction().addVertex("class:IPathObject,"+UUID.randomUUID(), IPathObject.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IPathObject> update(IPathObject existing_obj) {
		Iterator<IPathObject> path_obj_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(path_obj_iter.hasNext()){
			path_obj_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.generateKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(cnt == 0){
			connection.getTransaction().addVertex("class:"+ITest.class.getCanonicalName()+","+UUID.randomUUID(), IPathObject.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IPathObject> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IPathObject.class);
	}
}
