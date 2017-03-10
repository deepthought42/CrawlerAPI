package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.Group;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class GroupRepository implements IPersistable<Group, IGroup> {


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(Group group) {
		return "group:"+group.getName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IGroup convertToRecord(OrientConnectionFactory connection, Group group) {
		
		IGroup group_record = find(connection, generateKey(group));
		
		if(group_record == null){
			group_record = connection.getTransaction().addVertex("class:"+IGroup.class.getCanonicalName()+","+UUID.randomUUID(), IGroup.class);
			group_record.setKey(group.getKey());
			group_record.setDescription(group.getDescription());
			group_record.setName(group.getName());
		}

		return group_record;
	}

	/**
	 * {@inheritDoc}
	 */
	public Group create(OrientConnectionFactory conn, Group group) {
		IGroup group_record = find(conn, generateKey(group));
		
		if(group_record == null){
			group_record = convertToRecord(conn, group);
			conn.save();
		}
		group.setKey(generateKey(group));
		return group;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Only allows for description to be updated
	 */
	public Group update(OrientConnectionFactory conn, Group group) {
		IGroup group_record = find(conn, generateKey(group));
		if(group_record != null){
			group_record.setDescription(group.getDescription());
			conn.save();
		}
		
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IGroup find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IGroup> domains = (Iterable<IGroup>) DataAccessObject.findByKey(key, connection, IGroup.class);
		Iterator<IGroup> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}

	@Override
	public Group convertFromRecord(IGroup obj) {
		a// TODO Auto-generated method stub
		return null;
	}	
}