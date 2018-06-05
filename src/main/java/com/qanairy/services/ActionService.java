package com.qanairy.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.ActionDao;
import com.qanairy.models.dao.impl.ActionDaoImpl;
import com.qanairy.persistence.Action;

/**
 * 
 */
public class ActionService {
    @SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public static Action save(Action action) {
    	ActionDao action_dao = new ActionDaoImpl();
    	return action_dao.save(action);
    }
    
    public static Action find(String key){
    	ActionDao action_dao = new ActionDaoImpl();
    	return action_dao.find(key);
    }
    
    public static List<Action> getAll(){
    	ActionDao action_dao = new ActionDaoImpl();
    	return action_dao.getAll();
    }
}
