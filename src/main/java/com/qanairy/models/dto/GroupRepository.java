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
		return "group:"+group.getName().toLowerCase();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IGroup save(OrientConnectionFactory connection, Group group) {
		@SuppressWarnings("unchecked")
		Iterator<IGroup> iter = (Iterator<IGroup>) ((Iterable<IGroup>) DataAccessObject.findByKey(generateKey(group), connection, IGroup.class)).iterator();
		IGroup group_record= null;
		if(iter.hasNext()){
			group_record = iter.next();
			group.setDescription(group_record.getDescription());
			group.setName(group_record.getName());
		}
		else{
			group_record = connection.getTransaction().addVertex("class:"+IGroup.class.getSimpleName()+","+UUID.randomUUID(), IGroup.class);
			group_record.setKey(generateKey(group));
			group_record.setDescription(group.getDescription());
			group_record.setName(group.getName());
		}
		return group_record;
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
			return load(iter.next());
		}
		
		return null;
	}

	@Override
	public Group load(IGroup obj) {		
		return new Group(obj.getKey(), obj.getName(), obj.getDescription());
	}

	@Override
	public List<Group> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}	
}