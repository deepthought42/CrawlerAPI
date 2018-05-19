package models;

import java.util.ArrayList;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.AccountPOJO;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.impl.AccountDaoImpl;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.TestRecord;

/**
 * Defines all tests for the service package Repository
 */
public class AccountTests {
	
	@Test(groups="Regression")
	public void accountCreateRecordWithoutUsers(){
		Account acct = new AccountPOJO("Test Org", "#00000012SD", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountDao acct_dao = new AccountDaoImpl();
		Account acct_record = acct_dao.save(acct);
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertEquals(acct_record.getSubscriptionToken(), acct.getSubscriptionToken());
		Assert.assertEquals(acct_record.getDiscoveryRecords(), acct.getDiscoveryRecords());
		Assert.assertEquals(acct_record.getDomains(), acct.getDomains());
		Assert.assertEquals(acct_record.getOnboardedSteps(), acct.getOnboardedSteps());
		Assert.assertEquals(acct_record.getTestRecords(), acct.getTestRecords());
	}
	
	@Test(groups={"Account","Regression"})
	public void accountCreateRecordWithUsers(){		
		Account acct = new AccountPOJO("Test Org", "#00000012SD", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountDao acct_dao = new AccountDaoImpl();
		acct_dao.save(acct);
		
		Account acct_record = acct_dao.find(acct.getKey()); 
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertEquals(acct_record.getSubscriptionToken(), acct.getSubscriptionToken());
		Assert.assertEquals(acct_record.getDiscoveryRecords(), acct.getDiscoveryRecords());
		Assert.assertEquals(acct_record.getDomains(), acct.getDomains());
		Assert.assertEquals(acct_record.getOnboardedSteps(), acct.getOnboardedSteps());
		Assert.assertEquals(acct_record.getTestRecords(), acct.getTestRecords());
	}
	
	@Test(groups="Regression")
	public void accountUpdateRecord(){
		Account acct = new AccountPOJO("Test Org2", "acct_test1", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountDaoImpl acct_dao = new AccountDaoImpl();
		
		Account created_acct = acct_dao.save(acct);
		Assert.assertTrue(created_acct != null); 
		
		created_acct.setCustomerToken("acct_test1 update");
		Account updated_acct = acct_dao.save(acct);
		Assert.assertTrue(created_acct != null);

		Account acct_record = acct_dao.find(created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertEquals(acct_record.getSubscriptionToken(), acct.getSubscriptionToken());
		Assert.assertEquals(acct_record.getDiscoveryRecords(), acct.getDiscoveryRecords());
		Assert.assertEquals(acct_record.getDomains(), acct.getDomains());
		Assert.assertEquals(acct_record.getOnboardedSteps(), acct.getOnboardedSteps());
		Assert.assertEquals(acct_record.getTestRecords(), acct.getTestRecords());
	}
	
	
	@Test(groups="Regression")
	public void accountFindRecord(){
		AccountDao acct_dao = new AccountDaoImpl();

		Account acct = new AccountPOJO("Find Test Org", "acct_test1 update", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		acct_dao.save(acct);
		Account acct_record = acct_dao.find(acct.getKey());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertEquals(acct_record.getSubscriptionToken(), acct.getSubscriptionToken());
		Assert.assertEquals(acct_record.getDiscoveryRecords(), acct.getDiscoveryRecords());
		Assert.assertEquals(acct_record.getDomains(), acct.getDomains());
		Assert.assertEquals(acct_record.getOnboardedSteps(), acct.getOnboardedSteps());
		Assert.assertEquals(acct_record.getTestRecords(), acct.getTestRecords());
	}
}
