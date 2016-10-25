package com.minion.browsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.actions.Action;
import com.minion.persistence.IAction;
import com.minion.persistence.IPage;
import com.minion.persistence.IPageElement;
import com.minion.persistence.IPathObject;
import com.minion.persistence.IPersistable;

/**
 * An object wrapper that allows data to be dynamically placed in data structures
 * 
 * @author Brandon Kindred
 * @param <V>
 *
 */
public abstract class PathObject<V extends IPathObject> implements IPersistable<V>{
    private static final Logger log = LoggerFactory.getLogger(PathObject.class);
    
	private PathObject<?> next = null;

	/**
	 * {@inheritDoc}
	 */
	/*@Override
	public V convertToRecord(OrientConnectionFactory connection) {
		IPathObject path = connection.getTransaction().addVertex("class:IPathObject,"+UUID.randomUUID(), IPathObject.class);
		//path.setKey(this.generateKey());
		path.setData(this.);
		log.info("Starting conversion from path objects to their respective types");
		IPathObject persistablePathObj =  connection.getTransaction().addVertex("class:PathObjectRepository,"+UUID.randomUUID(), IPathObject.class);
		persistablePathObj.setData(this.getData());
		persistablePathObj.setNext(this.getNext().convertToRecord(connection));
		return persistablePathObj;
	}
	*/
	/**
	 * {@inheritDoc}
	 */
	/*@Override
	public IPersistable<V> create() {
		Iterator<V> path_obj_iter = this.findByKey(this.generateKey()).iterator();
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
		
		return null;
	}*/

	/**
	 * {@inheritDoc}
	 */
	/*@Override
	public IPersistable<V> update(V existing_obj) {
		Iterator<V> path_obj_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(path_obj_iter.hasNext()){
			path_obj_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.generateKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(cnt == 0){
			connection.getTransaction().addVertex("class:"+IPathObject.class.getCanonicalName()+","+UUID.randomUUID(), IPathObject.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}*/


	public static PathObject<?> convertFromRecord(IPathObject data){
		String type = data.getType();
		
		//log.info("data type :: " + data.getType()+" :: "+data.getClass().getName());
		log.info("current class name :: " + type);
		if(type.equals(Page.class)){
		//if(type.equals(Page.class.getName())){
			log.info("converting from page");
			//IPage page_record = ((IPage)data);
			//Page page_obj = new Page();
			Page page_obj = new Page();
			Iterable<IPage> page_iter = page_obj.findByKey(data.getKey());
			page_obj = Page.convertFromRecord(page_iter.iterator().next());
			log.info("coverted page from record :: " + page_obj +" :: ");
			return page_obj;
		}
		else if(type.equals(PageElement.class.getName())){
			log.info("converting from page element");

			//IPageElement page_elem_record = ((IPageElement)data);
			PageElement page_elem_obj = new PageElement();
			Iterable<IPageElement> page_elem_record = page_elem_obj.findByKey(data.getKey());
			page_elem_obj = page_elem_obj.convertFromRecord(page_elem_record.iterator().next());
			
			log.info("coverted page element from record :: " + page_elem_obj +" :: ");
			return page_elem_obj;
		}
		else if(type.equals(Action.class.getName())){			
			Action action = new Action();
			Iterable<IAction> iaction = action.findByKey(data.getKey());

			action = action.convertFromRecord(iaction.iterator().next());
			return action;
		}
		
		return null;
	}
	
	public abstract PathObject<?> clone();
}
