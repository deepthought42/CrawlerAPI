package models;

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.AccountPOJO;
import com.qanairy.models.QanairyUserPOJO;
import com.qanairy.models.dao.AccountDao;
import com.qanairy.models.dao.impl.AccountDaoImpl;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.Account;
import com.qanairy.persistence.DiscoveryRecord;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.QanairyUser;
import com.qanairy.persistence.TestRecord;

/**
 * Defines all tests for the service package Repository
 */
public class AccountTests {
	
	@Test(groups="Regression")
	public void accountCreateRecordWithoutUsers(){
		try{
			Account acct = new AccountPOJO("Test Org", "#00000012SD", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		}catch(AssertionError e){
			Assert.assertTrue(true);
			return;
		}
		Assert.assertFalse(true);
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
	}
	
	@Test(groups="Regression")
	public void accountUpdateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		Account acct = new Account("Test Org2", "acct_test1", "test_subscription", new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountRepository acct_repo = new AccountRepository();
		
		IAccount created_acct = acct_repo.save(connection, acct);
		Assert.assertTrue(created_acct != null);
		
		created_acct.setCustomerToken("acct_test1 update");
		IAccount updated_acct = acct_repo.save(connection, acct);
		Assert.assertTrue(created_acct != null);

		Account acct_record = acct_repo.find(connection, created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(updated_acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(updated_acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(updated_acct.getCustomerToken()));
		Assert.assertTrue(acct_record.getServicePackage().equals(updated_acct.getServicePackage()));
	}
	
	
	@Test(groups="Regression")
	public void accountFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		AccountRepository acct_repo = new AccountRepository();

		Account acct = new Account("Find Test Org", "acct_test1 update", "test_subscription", new ArrayList<QanairyUser>(), new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		acct_repo.save(orient_connection, acct);
		Account acct_record = acct_repo.find(orient_connection, acct.getKey());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertTrue(acct_record.getServicePackage().equals(acct.getServicePackage()));
	}
}
