package com.qanairy.models;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAction;
import com.qanairy.persistence.IPage;
import com.qanairy.persistence.IPageElement;
import com.qanairy.persistence.IPathObject;
import com.qanairy.persistence.IPersistable;

/**
 * An object wrapper that allows data to be dynamically placed in data structures
 * 
 * @author Brandon Kindred
 * @param <V extends IPathObject> 
 *
 */
public abstract class PathObject<V extends IPathObject> implements IPersistable<V>{
    private static final Logger log = LoggerFactory.getLogger(PathObject.class);
    private String type = null;
    
    public String getType(){
    	return this.type;
    }
    
    /**
     * Sets type to the classname passed. System generally expects classname to be simpleClassName()
     * @param classname
     */
    public void setType(String classname){
    	this.type = classname;
    }
    
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
	
	public abstract PathObject<?> clone();
}
