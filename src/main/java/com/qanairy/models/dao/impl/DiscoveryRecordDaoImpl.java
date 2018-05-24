package com.qanairy.models.dao.impl;


import java.util.NoSuchElementException;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines how a {@link DiscoveryRecord} database object can be interacted with.
 */
public class DiscoveryRecordDaoImpl implements DiscoveryRecordDao {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord save(DiscoveryRecord record) {
		assert record != null;
		DiscoveryRecord discovery_record = find(record.getKey());

		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(discovery_record == null){
			System.out.println("Converting discovery record obj to db record");
			discovery_record = connection.getTransaction().addFramedVertex(DiscoveryRecord.class);
			discovery_record.setKey(record.getKey());
			discovery_record.setStartTime(record.getStartTime());
			discovery_record.setBrowserName(record.getBrowserName());
			discovery_record.setDomainUrl(record.getDomainUrl());
		}

		discovery_record.setExaminedPathCount(record.getExaminedPathCount());
		discovery_record.setLastPathRanAt(record.getLastPathRanAt());
		discovery_record.setTestCount(record.getTestCount());
		discovery_record.setTotalPathCount(record.getTotalPathCount());
		
		return discovery_record;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public DiscoveryRecord find(String key) {
		DiscoveryRecord record = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			record = connection.getTransaction().getFramedVertices("key", key, DiscoveryRecord.class).next();
		}catch(NoSuchElementException e){
			System.err.println("could not find record");
		}
		connection.close();
		return record;
	}
}
