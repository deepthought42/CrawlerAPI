package models;

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.QanairyUser;
import com.qanairy.models.TestRecord;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package Repository
 */
public class AccountTests {
	
	@Test(groups="Regression")
	public void accountCreateRecordWithoutUsers(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		Account acct = new Account("Test Org", "Test Package", "#00000012SD", "test_subscription", new ArrayList<QanairyUser>(), new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountRepository acct_repo = new AccountRepository();
		
		Account created_acct = acct_repo.create(connection, acct);
		Assert.assertTrue(created_acct != null);
		Account acct_record = acct_repo.find(connection, created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(created_acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(created_acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(created_acct.getCustomerToken()));
		Assert.assertTrue(acct_record.getServicePackage().equals(created_acct.getServicePackage()));
	}
	
	@Test(groups={"Account","Regression"})
	public void accountCreateRecordWithUsers(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		
		List<QanairyUser> users = new ArrayList<QanairyUser>();
		QanairyUser user = new QanairyUser("Test user 1");
		users.add(user);

		Account acct = new Account("Test Org", "Test Package", "#00000012SD", "test_subscription", users, new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountRepository acct_repo = new AccountRepository();
		
		Account created_acct = acct_repo.create(connection, acct);
		Assert.assertTrue(created_acct != null);
		Account acct_record = acct_repo.find(connection, created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(created_acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(created_acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(created_acct.getCustomerToken()));
		Assert.assertTrue(acct_record.getServicePackage().equals(created_acct.getServicePackage()));
	}
	
	@Test(groups="Regression")
	public void accountUpdateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();

		Account acct = new Account("Test Org2", "Test Package", "acct_test1", "test_subscription", new ArrayList<QanairyUser>(), new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		AccountRepository acct_repo = new AccountRepository();
		
		Account created_acct = acct_repo.create(connection, acct);
		Assert.assertTrue(created_acct != null);
		
		created_acct.setCustomerToken("acct_test1 update");
		Account updated_acct = acct_repo.update(connection, acct);
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

		Account acct = new Account("Find Test Org", "Test Package", "acct_test1 update", "test_subscription", new ArrayList<QanairyUser>(), new ArrayList<DiscoveryRecord>(), new ArrayList<TestRecord>(), new ArrayList<String>());
		acct = acct_repo.create(orient_connection, acct);
		Account acct_record = acct_repo.find(orient_connection, acct.getKey());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getCustomerToken().equals(acct.getCustomerToken()));
		Assert.assertTrue(acct_record.getServicePackage().equals(acct.getServicePackage()));
	}
}
