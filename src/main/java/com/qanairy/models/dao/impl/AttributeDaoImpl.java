package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import com.qanairy.models.dao.AttributeDao;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.OrientConnectionFactory;

public class AttributeDaoImpl implements AttributeDao {
	
	@Override
	public Attribute save(Attribute attribute) {
		attribute.setKey(generateKey(attribute));

		OrientConnectionFactory connection = new OrientConnectionFactory();
		Attribute attribute_record = find(attribute.getKey());

		if(attribute_record == null){
			attribute_record = connection.getTransaction().addFramedVertex(Attribute.class);
			attribute_record.setName(attribute.getName());
			attribute_record.setVals(attribute.getVals());
			attribute_record.setKey(attribute.getKey());
		}
		
		return attribute_record;
	}

	@Override
	public Attribute find(String key) {
		Attribute attr = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			attr = connection.getTransaction().getFramedVertices("key", key, Attribute.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();
		return attr;
	}
}
