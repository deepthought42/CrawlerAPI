package models;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Domain;
import com.qanairy.models.Group;
import com.qanairy.persistence.IDomain;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package POJO
 */
public class DomainTests {
	
	/**
	 * 
	 */
	@Test
	public void domainCreateRecord(){
		Domain domain = new Domain("Test.test");

		//IDomain acct_record = acct.convertToRecord(orient_connection);
		IDomain domain_record = domain.create(new OrientConnectionFactory());
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
	
	/**
	 * 
	 */
	@Test
	public void accountUpdateRecord(){
		Domain domain = new Domain("Test.test", new ArrayList<com.qanairy.models.Test>(), new ArrayList<Group>());
		
		IDomain domain_record = domain.update(new OrientConnectionFactory());
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		
		//Assert.assertTrue(domain_record.getGroups() == domain.getGroups().size());
		//Assert.assertTrue(domain_record.getTests().size() == domain.getTests().size());
	}
	
	/**
	 * 
	 */
	@Test
	public void accountFindRecord(){
		Domain domain = new Domain("Test Domain");

		IDomain domain_record = domain.update(new OrientConnectionFactory());
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
}
