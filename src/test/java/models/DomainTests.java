package models;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.DomainPOJO;
import com.qanairy.models.dao.DomainDao;
import com.qanairy.models.dao.impl.DomainDaoImpl;
import com.qanairy.persistence.Domain;

/**
 * Defines all tests for the service package POJO
 */
public class DomainTests {
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void domainCreateRecord(){
		Domain domain = new DomainPOJO( "http", "Test.test", "chrome", "");
		DomainDao domain_dao = new DomainDaoImpl();

		Domain created_domain = domain_dao.save(domain);
		
		
		Assert.assertEquals(created_domain.getKey(), domain.getKey());
		Assert.assertEquals(created_domain.getUrl(), domain.getUrl());
		Assert.assertEquals(created_domain.getLogoUrl(), domain.getLogoUrl());
		Assert.assertEquals(created_domain.getProtocol(), domain.getProtocol());
		Assert.assertEquals(created_domain.getTestCount(), domain.getTestCount());
		Assert.assertEquals(created_domain.getUrl(), domain.getUrl());
		Assert.assertEquals(created_domain.getTests().size(), domain.getTests().size());
		Assert.assertEquals(created_domain.getTestUsers().size(), domain.getTestUsers().size());
	}
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void domainUpdateRecord(){
		Domain domain = new DomainPOJO("test_key_here", "Test.test", "Test.test", new ArrayList<com.qanairy.persistence.Test>(), "http", 0);
		DomainDaoImpl domain_dao = new DomainDaoImpl();
		domain.setKey(domain_dao.generateKey(domain));

		Domain domain_record = domain_dao.save(domain);
		
		Assert.assertEquals(domain_record.getKey(), domain.getKey());
		Assert.assertEquals(domain_record.getUrl(), domain.getUrl());
		Assert.assertEquals(domain_record.getLogoUrl(), domain.getLogoUrl());
		Assert.assertEquals(domain_record.getProtocol(), domain.getProtocol());
		Assert.assertEquals(domain_record.getTestCount(), domain.getTestCount());
		Assert.assertEquals(domain_record.getUrl(), domain.getUrl());
		Assert.assertEquals(domain_record.getTests().size(), domain.getTests().size());
		Assert.assertEquals(domain_record.getTestUsers().size(), domain.getTestUsers().size());
	}
}
