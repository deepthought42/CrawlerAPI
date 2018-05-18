package com.qanairy.models.dao;

import com.qanairy.persistence.TestRecord;

public interface TestRecordDao {
	public void save(TestRecord record);
	public TestRecord find(String key);
}
