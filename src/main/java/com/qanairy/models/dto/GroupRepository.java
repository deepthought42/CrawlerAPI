package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
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

		IGroup group_record = connection.getTransaction().addVertex("class:"+IGroup.class.getSimpleName()+","+UUID.randomUUID(), IGroup.class);
		group_record.setKey(group.getKey());
		group_record.setDescription(group.getDescription());
		group_record.setName(group.getName());

		return group_record;
	}

	/**
	 * {@inheritDoc}
	 */
	public Group create(OrientConnectionFactory conn, Group group) {
		Group group_record = find(conn, generateKey(group));
		
		if(group_record == null){
			convertToRecord(conn, group);
			//conn.save();
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
		@SuppressWarnings("unchecked")
		Iterable<IGroup> domains = (Iterable<IGroup>) DataAccessObject.findByKey(group.getKey(), conn, IGroup.class);
		Iterator<IGroup> iter = domains.iterator();
		
		if(iter.hasNext()){
			IGroup group_record = iter.next();
			group_record.setDescription(group.getDescription());
			conn.save();
		}
		
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Group find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IGroup> domains = (Iterable<IGroup>) DataAccessObject.findByKey(key, connection, IGroup.class);
		Iterator<IGroup> iter = domains.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public Group convertFromRecord(IGroup obj) {
		return new Group(obj.getKey(), obj.getName(), obj.getTests(), obj.getDescription());
	}

	@Override
	public List<Group> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}	
}