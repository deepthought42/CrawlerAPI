package com.qanairy.models;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines a name and color used to group {@link Test}s
 */
public class Group implements IPersistable<IGroup>{
	private String key;
	private String name;
	private List<Test> test;
	private String description;
	
	/**
	 * Construct a new grouping	
	 * 
	 * @param name 		name of the group
	 * @param test		{@link List} of {@link Test}s
	 * @param description describes group
	 */
	public Group(String name, List<Test> test){
		this.setName(name);
		this.setDescription("");
		this.generateKey();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<Test> getTest() {
		return test;
	}

	public void setTest(List<Test> test) {
		this.test = test;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return "group:"+this.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	public IGroup convertToRecord(OrientConnectionFactory connection) {
		
		IGroup group = find(connection);
		
		if(group == null){
			group = connection.getTransaction().addVertex("class:"+IGroup.class.getCanonicalName()+","+UUID.randomUUID(), IGroup.class);
			group.setKey(this.getKey());
			group.setDescription(this.getDescription());
			group.setName(this.getName());
		}

		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	public IGroup create(OrientConnectionFactory conn) {
		IGroup group = find(conn);
		
		if(group == null){
			group = this.convertToRecord(conn);
			conn.save();
		}
		return group;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Only allows for description to be updated
	 */
	public IGroup update(OrientConnectionFactory conn) {
		IGroup group = this.find(conn);
		if(group != null){
			group.setDescription(this.getDescription());
			conn.save();
		}
		
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IGroup find(OrientConnectionFactory connection) {
		@SuppressWarnings("unchecked")
		Iterable<IGroup> domains = (Iterable<IGroup>) DataAccessObject.findByKey(this.getKey(), connection, IGroup.class);
		Iterator<IGroup> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}

	private void setDescription(String description){
		this.description = description;
	}
	
	private String getDescription() {
		return this.description;
	}
}
