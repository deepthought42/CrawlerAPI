package models;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Domain;
import com.qanairy.models.dto.DomainRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package POJO
 */
public class DomainTests {
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void domainCreateRecord(){
		Domain domain = new Domain( "http", "Test.test", "chrome", "");
		DomainRepository domain_repo = new DomainRepository();

		Domain created_domain = domain_repo.create(new OrientConnectionFactory(), domain);
		
		
		Assert.assertTrue(created_domain.getKey().equals(domain.getKey()));
		Assert.assertTrue(created_domain.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void accountUpdateRecord(){
		Domain domain = new Domain("Test.test", "Test.test", "", new ArrayList<com.qanairy.models.Test>(), "http", null);
		DomainRepository domain_repo = new DomainRepository();
		domain.setKey(domain_repo.generateKey(domain));

		Domain domain_record = domain_repo.update(new OrientConnectionFactory(), domain);
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		
		//Assert.assertTrue(domain_record.getGroups() == domain.getGroups().size());
		//Assert.assertTrue(domain_record.getTests().size() == domain.getTests().size());
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void accountFindRecord(){
		Domain domain = new Domain("http", "Test Domain", "chrome", "" );
		DomainRepository domain_repo = new DomainRepository();
		domain.setKey(domain_repo.generateKey(domain));
		Domain domain_record = domain_repo.update(new OrientConnectionFactory(), domain);
		
		Assert.assertTrue(domain_record.getKey().equals(domain.getKey()));
		Assert.assertTrue(domain_record.getUrl().equals(domain.getUrl()));
		//Assert.assertTrue(domain_record.getGroups().equals(domain.getGroups()));
		//Assert.assertTrue(domain_record.getTests().equals(domain.getTests()));
	}
}
