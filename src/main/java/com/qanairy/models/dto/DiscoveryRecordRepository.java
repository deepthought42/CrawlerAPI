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

/**
 * Handles conversions to and from graph database
 */
@Component
public class DiscoveryRecordRepository implements IPersistable<DiscoveryRecord, IDiscoveryRecord> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey(DiscoveryRecord discovery_record) {
		return discovery_record.getStartedAt().toString()+"::"+discovery_record.getBrowserName();
	}

	
	/**
	 * {@inheritDoc}
	 */
	public IDiscoveryRecord save(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		discovery_record.setKey(generateKey(discovery_record));
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(generateKey(discovery_record), connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		IDiscoveryRecord discovery_record_record = null;

		if(!iter.hasNext()){
			System.out.println("Converting discovery record obj to db record");
			discovery_record_record = connection.getTransaction().addVertex("class:"+IDiscoveryRecord.class.getSimpleName()+","+UUID.randomUUID(), IDiscoveryRecord.class);
			discovery_record_record.setKey(discovery_record.getKey());
		}
		else{
			System.out.println("Discovery record found in db. Getting from db");
			discovery_record_record = iter.next();
		}
		discovery_record_record.setStartTime(discovery_record.getStartedAt());
		discovery_record_record.setBrowserName(discovery_record.getBrowserName());
		System.out.println("Updated discovery record");
		return discovery_record_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord create(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		discovery_record.setKey(generateKey(discovery_record));
		DiscoveryRecord discovery_record_record = find(connection, generateKey(discovery_record));
		
		if(discovery_record_record == null){
			save(connection, discovery_record);
		}
		return discovery_record;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord update(OrientConnectionFactory connection, DiscoveryRecord discovery_record) {
		save(connection, discovery_record);
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(discovery_record.getKey(), connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();

		if(iter.hasNext()){
			IDiscoveryRecord discovery_record_record = iter.next();
			
			discovery_record_record.setStartTime(discovery_record.getStartedAt());
			discovery_record_record.setBrowserName(discovery_record.getBrowserName());
		}
		
		return discovery_record;
	}
	
	public IDiscoveryRecord find(String key){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(key, connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because discovery_record already exists
			return iter.next();
		}
		connection.close();
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findByKey(key, connection, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because discovery_record already exists
			return convertFromRecord(iter.next());
		}
		
		return null;
	}

	@Override
	public DiscoveryRecord convertFromRecord(IDiscoveryRecord obj) {
		return new DiscoveryRecord(obj.getKey(), obj.getStartTime(), obj.getBrowserName());
	}
	
	@Override
	public List<DiscoveryRecord> findAll(OrientConnectionFactory conn) {
		@SuppressWarnings("unchecked")
		Iterable<IDiscoveryRecord> discovery_records = (Iterable<IDiscoveryRecord>) DataAccessObject.findAll(conn, IDiscoveryRecord.class);
		Iterator<IDiscoveryRecord> iter = discovery_records.iterator();
		
		List<DiscoveryRecord> discovery_record = new ArrayList<DiscoveryRecord>();
		while(iter.hasNext()){
			IDiscoveryRecord v = iter.next();
			discovery_record.add(convertFromRecord(v));
		}
		
		return discovery_record;
	}	
}