package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(PathObject.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(PathObject attr) {
		return attr.getType().hashCode()+"";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPathObject convertToRecord(OrientConnectionFactory connection, PathObject path_obj) {
		IPathObject attribute_record = connection.getTransaction().addVertex("class:"+PathObject.class.getCanonicalName()+","+UUID.randomUUID(), IPathObject.class);
		attribute_record.setType(path_obj.getType());
		attribute_record.setKey(generateKey(path_obj));
		
		return attribute_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject create(OrientConnectionFactory conn, PathObject attr) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection, attr);
		orient_connection.save();
		
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject update(OrientConnectionFactory conn, PathObject attr) {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		convertToRecord(connection, attr);
		connection.save();
		
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathObject convertFromRecord(IPathObject data){
		String type = data.getType();
		
		//log.info("data type :: " + data.getType()+" :: "+data.getClass().getName());
		log.info("current class name :: " + type);
		if(type.equals(Page.class)){
		//if(type.equals(Page.class.getName())){
			log.info("converting from page");
			//IPage page_record = ((IPage)data);
			//Page page_obj = new Page();
			Page page_obj = new Page();
			Iterable<IPage> page_iter = (Iterable<IPage>) DataAccessObject.findByKey(data.getKey(), IPage.class);
			page_obj = Page.convertFromRecord(page_iter.iterator().next());
			log.info("coverted page from record :: " + page_obj +" :: ");
			page_obj.setType(type);
			return page_obj;
		}
		else if(type.equals(PageElement.class.getName())){
			log.info("converting from page element");

			//IPageElement page_elem_record = ((IPageElement)data);
			PageElement page_elem_obj = null;
			Iterator<IPageElement> page_elem_record = (Iterator<IPageElement>) DataAccessObject.findByKey(data.getKey(), IPageElement.class).iterator();
			
			//List<PageElement> page_elem_records = new ArrayList<PageElement>();
			//for(IPageElement elem_record : page_elem_record){
				if(page_elem_record.next() instanceof IPageElement){
					page_elem_obj = PageElement.convertFromRecord((IPageElement)page_elem_record.next());
					//page_elem_records.add(page_elem_obj);
				}
			//}
			
			log.info("coverted page element from record :: " + page_elem_obj +" :: ");
			page_elem_obj.setType(type);
			return page_elem_obj;
		}
		else if(type.equals(Action.class.getName())){			
			Action action = new Action();
			
			Iterable<IAction> iaction = (Iterable<IAction>)DataAccessObject.findByKey(data.getKey(), IAction.class);
			action.setType(type);
			return action.convertFromRecord(iaction.iterator().next());
		}
		
		return null;
	}

	@Override
	public IPathObject find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IPathObject> svc_pkgs = (Iterable<IPathObject>) DataAccessObject.findByKey(key, connection, IPathObject.class);
		Iterator<IPathObject> iter = svc_pkgs.iterator();
		
		IPathObject account = null; 
		if(iter.hasNext()){
			account = iter.next();
		}
		
		return account;
	}
}