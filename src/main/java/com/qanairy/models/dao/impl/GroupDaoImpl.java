package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import com.qanairy.models.dao.GroupDao;
import com.qanairy.persistence.Group;
import com.qanairy.persistence.OrientConnectionFactory;

public class GroupDaoImpl implements GroupDao {

	@Override
	public Group save(Group group) {
		Group group_record= find(group.getKey());
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(group_record == null){
			group_record = connection.getTransaction().addFramedVertex(Group.class);
			group_record.setKey(group.getKey());
			group_record.setDescription(group.getDescription());
			group_record.setName(group.getName());
		}
		return group_record;
	}

	@Override
	public Group find(String key) {
		Group group = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			group = connection.getTransaction().getFramedVertices("key", key, Group.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find group record");
		}
		connection.close();
		return group;
	}
}
