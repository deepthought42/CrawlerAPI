package com.minion.browsing.actions;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.browsing.PathObject;
import com.minion.persistence.IAction;
import com.minion.persistence.IPersistable;
import com.minion.persistence.OrientConnectionFactory;

/**
 * Defines an action in name only
 * 
 * @author Brandon Kindred
 *
 */
public class Action extends PathObject<IAction>{
	private static final Logger log = LoggerFactory.getLogger(Action.class);

	private final String name;
	private final String key;
	
	/**
	 * Construct empty action object
	 */
	public Action(){
		this.name = null;
		this.key = null;
	}
	
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		this.name = action_name;
		this.key = generateKey();
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public String getKey() {
		return this.key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAction convertToRecord(OrientConnectionFactory connection) {
		log.info("Creating Action path object record");
		Iterable<IAction> actions = findByKey(this.getKey(), connection);
		
		int cnt = 0;
		Iterator<IAction> action_iter = actions.iterator();
		IAction action = null;
		while(action_iter.hasNext()){
			action_iter.next();
			cnt++;
		}
		
		if(cnt == 0){
			action = connection.getTransaction().addVertex("class:"+IAction.class.getCanonicalName()+","+UUID.randomUUID(), IAction.class);
			action.setName(this.name);
			action.setKey(this.key);
			action.setType(this.getClass().getName());
		}
		else{
			action = actions.iterator().next();
		}		
		
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAction> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAction> update() {
		Iterator<IAction> action_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(action_iter.hasNext()){
			action_iter.next();
			cnt++;
		}
		log.info("# of existing Action records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IAction action = null;
		if(cnt == 0){
			action = connection.getTransaction().addVertex(UUID.randomUUID(), IAction.class);	
		}
		else{
			action = this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IAction> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IAction.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IAction> findByKey(String generated_key, OrientConnectionFactory orient_connection) {
		return orient_connection.getTransaction().getVertices("key", generated_key, IAction.class);
	}

	public Action convertFromRecord(IAction data) {
		Action action = new Action(data.getName());
		
		return action;
	}

	@Override
	public PathObject<?> clone() {
		Action action_clone = new Action(this.getName());
		//action_clone.setNext(this.getNext());
		return action_clone;
	}

}
