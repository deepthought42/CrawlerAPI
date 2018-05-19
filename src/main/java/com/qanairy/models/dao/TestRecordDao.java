package com.qanairy.models.dao;

import com.qanairy.persistence.TestRecord;

public interface TestRecordDao {
	public TestRecord save(TestRecord record);
	public TestRecord find(String key);
}
