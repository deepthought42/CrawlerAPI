package com.qanairy.models.dao;

import com.qanairy.persistence.DiscoveryRecord;

public interface DiscoveryRecordDao {
	public void save(DiscoveryRecord record);
	public DiscoveryRecord find(String key);
}
