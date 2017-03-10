package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Account;
import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.IAccount;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package POJO
 */
public class AccountTests {
	
	@Test
	public void accountCreateRecord(){
		Account acct = new Account("Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1");

		IAccount acct_record = acct.create(new OrientConnectionFactory());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(acct.getServicePackage().getName()));
	}
	
	
	@Test
	public void accountUpdateRecord(){
		Account acct = new Account("Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1 update");

		IAccount acct_record = acct.update(new OrientConnectionFactory());
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(acct.getServicePackage().getName()));
	}
	
	
	@Test
	public void accountFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		Account acct = new Account("Find Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1 update");
		acct.create(orient_connection);
		IAccount acct_record = acct.find(orient_connection);
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		Assert.assertTrue(acct_record.getServicePackage().getName().equals(acct.getServicePackage().getName()));
	}
}
