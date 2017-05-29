package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.UUID;

import com.orientechnologies.orient.core.Orient;
import com.qanairy.models.ChildModelTempDemo;
import com.qanairy.models.Group;
import com.qanairy.models.SystemInfo;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.ISystemInfo;
import com.qanairy.persistence.OrientConnectionFactory;

public class SystemInfoRepository {

	/**
	 * {@inheritDoc}
	 */
	public static SystemInfo save(OrientConnectionFactory conn, SystemInfo system_info) {
		ISystemInfo record = find(conn, generateKey(system_info));
		system_info.setKey(generateKey(system_info));

		if(record == null){
			record = conn.getTransaction().addVertex("class:"+SystemInfo.class.getSimpleName()+","+UUID.randomUUID(), ISystemInfo.class);
			record.setKey(system_info.getKey());
		}
		
		record.setBrowserCount(system_info.getBrowserCount());
		conn.save();
		
		return system_info;
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

	public static Iterator<ChildModelTempDemo> findAllGroups(OrientConnectionFactory conn, SystemInfo info){
		ISystemInfo sys_info = find(conn, info.getKey());
		return sys_info.getGroups();
		
	}
	/**
	 * {@inheritDoc}
	 */
	public static String generateKey(SystemInfo info) {
		return "system_info";
	}
}
