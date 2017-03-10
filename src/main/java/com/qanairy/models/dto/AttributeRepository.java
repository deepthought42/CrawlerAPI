package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.Attribute;
import com.qanairy.models.Attribute;
import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IAttribute;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class AttributeRepository implements IPersistable<Attribute, IAttribute> {

	/**
	 * {@inheritDoc}
	 */
	public String generateKey(Attribute attr) {
		return attr.getName().hashCode()+":";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAttribute convertToRecord(OrientConnectionFactory connection, Attribute attr) {
		IAttribute attribute_record = connection.getTransaction().addVertex("class:"+Attribute.class.getCanonicalName()+","+UUID.randomUUID(), IAttribute.class);
		attribute_record.setName(attr.getName());
		attribute_record.setVals(attr.getVals());
		attribute_record.setKey(attr.getKey());
		
		return attribute_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Attribute create(OrientConnectionFactory conn, Attribute attr) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection, attr);
		orient_connection.save();
		
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Attribute update(OrientConnectionFactory conn, Attribute attr) {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		convertToRecord(connection, attr);
		connection.save();
		
		return attr;
	}
}