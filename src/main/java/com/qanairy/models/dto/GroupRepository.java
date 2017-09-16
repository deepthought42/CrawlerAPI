package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.Group;
import com.qanairy.models.Test;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IGroup;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.ITest;
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
		@SuppressWarnings("unchecked")
		Iterator<IGroup> iter = (Iterator<IGroup>) ((Iterable<IGroup>) DataAccessObject.findByKey(generateKey(group), connection, IGroup.class)).iterator();
		IGroup group_record= null;
		if(iter.hasNext()){
			group_record = iter.next();
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
		List<Test> tests = new ArrayList<Test>();
		TestRepository test_repo = new TestRepository();
		if(obj.getTests() != null){
			Iterator<ITest> test_iter = obj.getTests().iterator();
			while(test_iter.hasNext()){
				tests.add(test_repo.convertFromRecord(test_iter.next()));
			}
		}
		return new Group(obj.getKey(), obj.getName(), tests, obj.getDescription());
	}

	@Override
	public List<Group> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}	
}