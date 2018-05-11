package models;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.dto.DiscoveryRecordRepository;
import com.qanairy.persistence.IDiscoveryRecord;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package POJO
 */
public class DiscoveryRecordTests {
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void discoveryRecordCreateRecord(){
		Date now = new Date();
		DiscoveryRecord discovery_record = new DiscoveryRecord(now, "chrome", "www.qanairy-test.com");
		DiscoveryRecordRepository discovery_record_repo = new DiscoveryRecordRepository();

		IDiscoveryRecord created_discovery_record = discovery_record_repo.save(new OrientConnectionFactory(), discovery_record);
		
		Assert.assertTrue(created_discovery_record.getKey().equals(discovery_record_repo.generateKey(discovery_record)));
		//Assert.assertTrue(created_discovery_record.getStartTime().equals(now));
		Assert.assertTrue(created_discovery_record.getBrowserName().equals(discovery_record.getBrowserName()));
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void accountUpdateRecord(){
		Date now = new Date();
		DiscoveryRecord discovery_record = new DiscoveryRecord(now, "chrome", "www.qanairy-test.com");
		DiscoveryRecordRepository discovery_record_repo = new DiscoveryRecordRepository();
		discovery_record.setKey(discovery_record_repo.generateKey(discovery_record));

		OrientConnectionFactory conn = new OrientConnectionFactory();
		IDiscoveryRecord updated_discovery_record = discovery_record_repo.save(conn, discovery_record);
		conn.close();
		Assert.assertTrue(updated_discovery_record.getStartTime().equals(now));
		Assert.assertTrue(updated_discovery_record.getBrowserName().equals(discovery_record.getBrowserName()));
	}
}
