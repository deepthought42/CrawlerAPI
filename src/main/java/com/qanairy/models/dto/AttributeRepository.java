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
		return attr.getName().hashCode()+":";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAttribute convertToRecord(OrientConnectionFactory connection, Attribute attr) {
		IAttribute attribute_record = connection.getTransaction().addVertex("class:"+Attribute.class.getSimpleName()+","+UUID.randomUUID(), IAttribute.class);
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
		Attribute found_attr = find(conn, attr.getKey());

		if( found_attr == null ){
			this.convertToRecord(conn, attr);
		}
		return attr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Attribute update(OrientConnectionFactory conn, Attribute attr) {
		OrientConnectionFactory connection = new OrientConnectionFactory();
		@SuppressWarnings("unchecked")
		Iterable<IAttribute> svc_pkgs = (Iterable<IAttribute>) DataAccessObject.findByKey(attr.getKey(), conn, IAttribute.class);
		Iterator<IAttribute> iter = svc_pkgs.iterator();

		if(iter.hasNext()){
			convertToRecord(connection, attr);
			connection.save();
		}

		return attr;
	}

	@Override
	public Attribute convertFromRecord(IAttribute attr_record) {
		return new Attribute(attr_record.getKey(), attr_record.getName(),  attr_record.getVals());
	}

	@Override
	public Attribute find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IAttribute> svc_pkgs = (Iterable<IAttribute>) DataAccessObject.findByKey(key, connection, IAttribute.class);
		Iterator<IAttribute> iter = svc_pkgs.iterator();

		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}

		return null;
	}

	@Override
	public List<Attribute> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}
}
