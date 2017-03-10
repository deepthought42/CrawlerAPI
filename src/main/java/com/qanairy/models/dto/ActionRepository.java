package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.Action;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAction;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ActionRepository implements IPersistable<Action, IAction>{

	private static final Logger log = LoggerFactory.getLogger(Action.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Action action) {
		return action.getName() + ":"+ action.getValue().hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public IAction convertToRecord(OrientConnectionFactory rl_conn, Action action) {
		IAction action_record = rl_conn.getTransaction().addVertex("class:"+IAction.class.getCanonicalName()+","+UUID.randomUUID(), IAction.class);
		action_record.setName(action.getName());
		action_record.setKey(action.getKey());
		action_record.setType(action.getClass().getName());
		action_record.setValue(action.getValue());
		
		return action_record;
	}

	/**
	 * {@inheritDoc}
	 */
	public Action create(OrientConnectionFactory connection, Action action) {
		IAction action_record = find(connection, action.getKey());
		
		if(action_record != null){
			action_record = this.convertToRecord(connection, action);
			connection.save();
		}
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	public Action update(OrientConnectionFactory connection, Action action) {
		IAction action_record = find(connection, action.getKey());
		if(action_record != null){
			action_record.setName(action.getName());
			action_record.setType(action.getType());
			action_record.setValue(action.getValue());
			connection.save();
		}
		
		return action;
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public Action convertFromRecord(IAction data) {
		Action action = new Action(data.getName(), data.getValue());
		action.setType(Action.class.getSimpleName());
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAction find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAction> actions = (Iterable<IAction>) DataAccessObject.findByKey(key, connection, IAction.class);
		Iterator<IAction> iter = actions.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}
}