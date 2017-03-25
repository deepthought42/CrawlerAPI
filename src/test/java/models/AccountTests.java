package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Account;
import com.qanairy.models.ServicePackage;
import com.qanairy.models.dto.AccountRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package Repository
 */
public class AccountTests {
	
	@Test
	public void accountCreateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		Account acct = new Account("Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1");
		AccountRepository acct_repo = new AccountRepository();
		
		Account created_acct = acct_repo.create(connection, acct);
		Assert.assertTrue(created_acct != null);
		Account acct_record = acct_repo.find(connection, created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(created_acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(created_acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(created_acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(created_acct.getServicePackage().getName()));
	}
	
	
	@Test
	public void accountUpdateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		Account acct = new Account("Test Org2", new ServicePackage("Test Package", 80, 5), "acct_test1");
		AccountRepository acct_repo = new AccountRepository();
		
		Account created_acct = acct_repo.create(connection, acct);
		Assert.assertTrue(created_acct != null);
		
		created_acct.setPaymentAcctNum("acct_test1 update");
		Account updated_acct = acct_repo.update(connection, acct);
		Assert.assertTrue(created_acct != null);

		Account acct_record = acct_repo.find(connection, created_acct.getKey()); 
		Assert.assertTrue(acct_record.getKey().equals(updated_acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(updated_acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(updated_acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(updated_acct.getServicePackage().getName()));
	}
	
	
	@Test
	public void accountFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		AccountRepository acct_repo = new AccountRepository();

		Account acct = new Account("Find Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1 update");
		acct = acct_repo.create(orient_connection, acct);
		Account acct_record = acct_repo.find(orient_connection, acct.getKey());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(acct.getServicePackage().getName()));
	}
}
