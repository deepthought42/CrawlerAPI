package com.qanairy.models.dao;

import com.qanairy.persistence.DiscoveryRecord;

public interface DiscoveryRecordDao {
	public DiscoveryRecord save(DiscoveryRecord record);
	public DiscoveryRecord find(String key);
}
