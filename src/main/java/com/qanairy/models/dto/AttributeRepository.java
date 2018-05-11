package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.qanairy.models.Attribute;
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
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(attr.getVals().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAttribute save(OrientConnectionFactory connection, Attribute attr) {
		if((attr.getKey() == null || attr.getKey().isEmpty()) && attr.getName() != null){
			attr.setKey(generateKey(attr));
		}
		@SuppressWarnings("unchecked")
		Iterable<IAttribute> attribute_records = (Iterable<IAttribute>) DataAccessObject.findByKey(attr.getKey(), connection, IAttribute.class);

		Iterator<IAttribute> iter = attribute_records.iterator();
		
		IAttribute attribute_record = null;
		if( !iter.hasNext()){
			attribute_record = connection.getTransaction().addVertex("class:"+IAttribute.class.getSimpleName()+","+UUID.randomUUID(), IAttribute.class);
			attribute_record.setName(attr.getName());
			attribute_record.setVals(attr.getVals());
			attribute_record.setKey(attr.getKey());
		}
		else{
			attribute_record = iter.next();
			attr.setKey(attribute_record.getKey());
			attr.setName(attribute_record.getName());
			attr.setVals(attribute_record.getVals());
		}
		
		return attribute_record;
	}

	@Override
	public Attribute load(IAttribute attr_record) {
		return new Attribute(attr_record.getKey(), attr_record.getName(),  attr_record.getVals());
	}

	@Override
	public Attribute find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAttribute> attributes = (Iterable<IAttribute>) DataAccessObject.findByKey(key, connection, IAttribute.class);
		Iterator<IAttribute> iter = attributes.iterator();

		if(iter.hasNext()){
			return load(iter.next());
		}

		return null;
	}

	@Override
	public List<Attribute> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}
