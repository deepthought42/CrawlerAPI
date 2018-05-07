package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import com.qanairy.models.Action;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAction;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ActionRepository implements IPersistable<Action, IAction>{

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Action.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Action action) {
		return action.getName() + ":"+ action.getValue().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public IAction save(OrientConnectionFactory conn, Action action) {
		action.setKey(generateKey(action));
		
		@SuppressWarnings("unchecked")
		Iterable<IAction> actions = (Iterable<IAction>) DataAccessObject.findByKey(action.getKey(), conn, IAction.class);
		Iterator<IAction> iter = actions.iterator();
		
		IAction action_record = null;
		if(!iter.hasNext()){
			action_record = conn.getTransaction().addVertex("class:"+IAction.class.getSimpleName()+","+UUID.randomUUID(), IAction.class);
			action_record.setName(action.getName());
			action_record.setKey(action.getKey());
			action_record.setType(Action.class.getSimpleName());
			action_record.setValue(action.getValue());
		}
		else{
			action_record = iter.next();
			action.setKey(action_record.getKey());
			action.setType(action_record.getType());
			action.setValue(action_record.getValue());
		}
		return action_record;
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public Action load(IAction data) {
		return new Action(data.getName(), data.getValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Action find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAction> actions = (Iterable<IAction>) DataAccessObject.findByKey(key, connection, IAction.class);
		Iterator<IAction> iter = actions.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			load(iter.next());
		}
		
		return null;
	}

	@Override
	public List<Action> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}