package com.qanairy.models.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IDiscoveryRecord;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.OrientConnectionFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * 
 */
@Component
public class DiscoveryRecordRepository implements IPersistable<DiscoveryRecord, IDiscoveryRecord> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(DiscoveryRecord discovery_record) {
		return discovery_record.getDate().toString();
	}

	@Override
	public IDiscoveryRecord convertToRecord(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		discovery_record.setKey(generateKey(discovery_record));
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(discovery_record.getKey(), connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		IDiscoveryRecord idiscovery_record = null;  

		if(!iter.hasNext()){
			idiscovery_record = connection.getTransaction().addVertex("class:"+IDiscoveryRecord.class.getSimpleName()+","+UUID.randomUUID(), IDiscoveryRecord.class);
			idiscovery_record.setKey(discovery_record.getKey());
			idiscovery_record.setDate(discovery_record.getDate());
		}
		else{
			idiscovery_record = iter.next();
		}
		
		return idiscovery_record;
	}

	@Override
	public DiscoveryRecord convertFromRecord(IDiscoveryRecord discovery_record) {
		return new DiscoveryRecord(discovery_record.getKey(), discovery_record.getDate());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord create(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		discovery_record.setKey(generateKey(discovery_record));

		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(discovery_record.getKey(), connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		  
		if(!iter.hasNext()){
			convertToRecord(connection, discovery_record);
		}
		
		return discovery_record;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord update(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		if(discovery_record.getKey() == null){
			discovery_record.setKey(generateKey(discovery_record));
		}
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(discovery_record.getKey(), connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		  
		IDiscoveryRecord idiscovery_record = null;
		if(iter.hasNext()){
			idiscovery_record = iter.next();
		}
		return convertFromRecord(idiscovery_record);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> svc_pkgs = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(key, connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = svc_pkgs.iterator();
		
		if(iter.hasNext()){
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public List<DiscoveryRecord> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterator<OrientVertex> iter = ((Iterable<OrientVertex>) DataAccessObject.findAll(conn, IDiscoveryRecord.class)).iterator();
		
		List<DiscoveryRecord> discovery_records = new ArrayList<DiscoveryRecord>();
		while(iter.hasNext()){
			OrientVertex v = iter.next();
			discovery_records.add(convertFromRecord((IDiscoveryRecord)v));
		}
		
		return discovery_records;
	}
}