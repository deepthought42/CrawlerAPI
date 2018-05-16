package models;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.DiscoveryRecordPOJO;
import com.qanairy.models.dao.DiscoveryRecordDao;
import com.qanairy.models.dao.impl.DiscoveryRecordDaoImpl;
import com.qanairy.persistence.DiscoveryRecord;
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
		DiscoveryRecord discovery_record = new DiscoveryRecordPOJO(now, "chrome", "www.qanairy-test.com");
		DiscoveryRecordDao discovery_record_dao = new DiscoveryRecordDaoImpl();

		discovery_record_dao.save(discovery_record);
		
		DiscoveryRecord created_record = discovery_record_dao.find(discovery_record.getKey());
		Assert.assertTrue(created_record.getKey().equals(discovery_record.getKey()));
		Assert.assertTrue(created_record.getBrowserName().equals(discovery_record.getBrowserName()));
		Assert.assertTrue(created_record.getDomainUrl().equals(discovery_record.getDomainUrl()));
		Assert.assertTrue(created_record.getLastPathRanAt().equals(discovery_record.getLastPathRanAt()));
		Assert.assertTrue(created_record.getTestCount() == discovery_record.getTestCount());
		Assert.assertTrue(created_record.getTotalPathCount() == discovery_record.getTotalPathCount());
		Assert.assertTrue(created_record.getExaminedPathCount() == discovery_record.getExaminedPathCount());
		Assert.assertTrue(created_record.getStartTime().equals(discovery_record.getStartTime()));
	}
}
