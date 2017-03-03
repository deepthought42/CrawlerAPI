package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Domain;
import com.qanairy.persistence.IDomain;

/**
 * Defines all tests for the service package POJO
 */
public class DomainTests {
	@Test
	public void domainCreateRecord(){
		Domain domain = new Domain("Test domain");

		//IDomain acct_record = acct.convertToRecord(orient_connection);
		IDomain domain_record = domain.create();
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
	
	@Test
	public void accountUpdateRecord(){
		Domain domain = new Domain("Test Domain");

		IDomain domain_record = domain.update();
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
}
