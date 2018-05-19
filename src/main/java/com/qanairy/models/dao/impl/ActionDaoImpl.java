package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.ActionDao;
import com.qanairy.persistence.Action;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ActionDaoImpl implements ActionDao {
	private static Logger log = LoggerFactory.getLogger(Action.class);

	
	@Override
	public Action save(Action action) {
		action.setKey(generateKey(action));
		
		Action action_record = find(action.getKey());
		OrientConnectionFactory connection = new OrientConnectionFactory();

		if(action_record == null){
			action_record = connection.getTransaction().addFramedVertex(Action.class);
			action_record.setName(action.getName());
			action_record.setKey(action.getKey());
			action_record.setType(Action.class.getSimpleName());
			action_record.setValue(action.getValue());
		}
		
		return action_record;
	}

	@Override
	public Action find(String key) {
		Action attr = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			attr = connection.getTransaction().getFramedVertices("key", key, Action.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting action record from database");
		}
		connection.close();
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Action action) {
		return action.getName() + ":"+ action.getValue().hashCode();
	}
}
