package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.qanairy.models.Action;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.PathObject;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAction;
import com.qanairy.persistence.IPage;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IPathObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PathObjectRepository implements IPersistable<PathObject, IPathObject> {
	private static Logger log = LogManager.getLogger(PathObject.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(PathObject attr) {
		return this.getClass().getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPathObject convertToRecord(OrientConnectionFactory connection, PathObject path_obj) {
		
		IPathObject path_object_record = connection.getTransaction().addVertex("class:I"+path_obj.getClass().getSimpleName()+","+UUID.randomUUID(), IPathObject.class);
		path_object_record.setType(path_obj.getType());
		path_object_record.setKey(generateKey(path_obj));
		System.err.println("Converting path object to record");
		if(path_obj instanceof Page){
			
		}
		else if(path_obj instanceof PageElement){
			
		}
		else if(path_obj instanceof Action){
	
		}
		return path_object_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject create(OrientConnectionFactory conn, PathObject attr) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection, attr);
		orient_connection.close();
		
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject update(OrientConnectionFactory conn, PathObject attr) {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		convertToRecord(connection, attr);
		connection.close();
		
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject convertFromRecord(IPathObject data){
		String type = data.getType();
		
		//System.err.println("data type :: " + data.getType()+" :: "+data.getClass().getName());
		if(type.equals("Page")){
		//if(type.equals(Page.class.getName())){
			log.debug("converting from page");
			//IPage page_record = ((IPage)data);
			//Page page_obj = new Page();
			Page page_obj = null;
			PageRepository page_record = new PageRepository();
			@SuppressWarnings("unchecked")
			Iterator<IPage> page_iter = ((Iterable<IPage>) DataAccessObject.findByKey(data.getKey(), IPage.class)).iterator();
			if(page_iter.hasNext()){
				page_obj = page_record.convertFromRecord(page_iter.next());
			}
			page_obj.setType(type);
			return page_obj;
		}
		else if(type.equals("PageElement")){
			log.debug("converting from page element");

			//IPageElement page_elem_record = ((IPageElement)data);
			PageElement page_elem_obj = null;
			@SuppressWarnings("unchecked")
			Iterator<IPageElement> page_elem_record_iter = (Iterator<IPageElement>) DataAccessObject.findByKey(data.getKey(), IPageElement.class).iterator();

			//List<PageElement> page_elem_records = new ArrayList<PageElement>();
			//for(IPageElement elem_record : page_elem_record){
				IPageElement page_elem_record = page_elem_record_iter.next();
				if(page_elem_record != null){
					PageElementRepository page_elem_repo = new PageElementRepository();

					page_elem_obj = page_elem_repo.convertFromRecord(page_elem_record);
					page_elem_obj.setType(type);
					//page_elem_records.add(page_elem_obj);
				}
			//}
			
			return page_elem_obj;
		}
		else if(type.equals("Action")){			
			Action action = new Action();
			
			@SuppressWarnings("unchecked")
			Iterable<IAction> iaction = (Iterable<IAction>)DataAccessObject.findByKey(data.getKey(), IAction.class);
			action.setType(type);
			
			ActionRepository action_record = new ActionRepository();
			return action_record.convertFromRecord(iaction.iterator().next());
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IPathObject> svc_pkgs = (Iterable<IPathObject>) DataAccessObject.findByKey(key, connection, IPathObject.class);
		Iterator<IPathObject> iter = svc_pkgs.iterator();
		
		PathObject path_obj = null; 
		if(iter.hasNext()){
			path_obj = convertFromRecord(iter.next());
		}
		
		return path_obj;
	}

	@Override
	public List<PathObject> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}