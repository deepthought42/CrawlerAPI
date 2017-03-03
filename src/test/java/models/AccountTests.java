package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Account;
import com.qanairy.models.ServicePackage;
import com.qanairy.persistence.IAccount;

/**
 * Defines all tests for the service package POJO
 */
public class AccountTests {
	@Test
	public void accountCreateRecord(){
		Account acct = new Account("Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1");

		//IAccount acct_record = acct.convertToRecord(orient_connection);
		IAccount acct_record = acct.create();
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		
		System.err.println("PACKAGE :: " + acct.getServicePackage());
		Assert.assertTrue(acct_record.getServicePackage().equals(acct.getServicePackage()));
	}
	
	@Test
	public void accountUpdateRecord(){
		Account acct = new Account("Test Org", new ServicePackage("Test Package", 80, 5), "acct_test1 update");

		//IAccount acct_record = acct.convertToRecord(orient_connection);
		IAccount acct_record = acct.update();
		
		Assert.assertTrue(acct_record.getKey().equals(acct.getKey()));
		Assert.assertTrue(acct_record.getOrgName().equals(acct.getOrgName()));
		Assert.assertTrue(acct_record.getPaymentAcctNum().equals(acct.getPaymentAcctNum()));
		
		System.err.println("PACKAGE :: " + acct_record.getServicePackage().getName());
		System.err.println("PACKAGE :: " + acct.getServicePackage().getName());

		Assert.assertTrue(acct_record.getServicePackage().getName().equals(acct.getServicePackage().getName()));
	}
}
