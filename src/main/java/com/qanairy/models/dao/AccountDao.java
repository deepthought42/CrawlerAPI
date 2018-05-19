package com.qanairy.models.dao;

import java.util.List;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.TestRecord;

public interface AccountDao {
	public Account find(String key);
	public Account save(Account account);
	public void remove(Account account);
	public void updateSubscription(String key, String token);
	public void updateCustomerToken(String key, String token);
	public List<DiscoveryRecord> getAllDiscoveryRecords();
	public List<DiscoveryRecord> getDiscoveryRecordsByMonth(int month);
	public List<TestRecord> getAllTestRecords();
	public List<TestRecord> getTestRecordsByMonth(int month);
}
