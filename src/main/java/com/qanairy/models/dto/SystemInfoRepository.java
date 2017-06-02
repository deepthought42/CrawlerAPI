package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.qanairy.models.SystemInfo;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.ISystemInfo;
import com.qanairy.persistence.OrientConnectionFactory;

public class SystemInfoRepository {

	/**
	 * {@inheritDoc}
	 */
	public static ISystemInfo save(OrientConnectionFactory conn, ISystemInfo info) {
		ISystemInfo record = find(conn, generateKey(info));
		//info.setKey(generateKey(info));

		if(record == null){
			record = conn.getTransaction().addVertex("class:"+SystemInfo.class.getSimpleName()+","+UUID.randomUUID(), ISystemInfo.class);
			record.setKey(generateKey(info));
		}
		
		record.setBrowserCount(info.getBrowserCount());
		conn.save();
		
		return info;
	}

	/**
	 * {@inheritDoc}
	 */
	public static ISystemInfo find(OrientConnectionFactory connection, String key) {
		@SuppressWarnings("unchecked")
		Iterable<ISystemInfo> system_info_records = (Iterable<ISystemInfo>) DataAccessObject.findByKey(key, connection, ISystemInfo.class);
		Iterator<ISystemInfo> iter = system_info_records.iterator();
		  
		if(iter.hasNext()){
			//figure out throwing exception because domain already exists
			return iter.next();
		}
		
		return null;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public static String generateKey(ISystemInfo info) {
		return "system_info";
	}
}
