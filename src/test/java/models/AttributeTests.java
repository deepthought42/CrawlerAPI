package models;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Attribute;
import com.qanairy.models.dto.AttributeRepository;
import com.qanairy.persistence.OrientConnectionFactory;


/**
 * Defines all tests for the service package Repository
 */
public class AttributeTests {
	
	@Test(groups="Regression")
	public void attributeCreateRecord(){
		OrientConnectionFactory connection = new OrientConnectionFactory();
		List<String> attributes = new ArrayList<String>();
		attributes.add("button");
		attributes.add("redbutton");
		Attribute attr = new Attribute("class", attributes);
		AttributeRepository attr_repo = new AttributeRepository();
		
		Attribute created_attr = attr_repo.create(connection, attr);
		Assert.assertTrue(created_attr != null);
		Attribute attr_record = attr_repo.find(connection, created_attr.getKey()); 
		Assert.assertTrue(attr_record.getKey().equals(created_attr.getKey()));
		Assert.assertTrue(attr_record.getName().equals(created_attr.getName()));
		Assert.assertTrue(attr_record.getVals().equals(created_attr.getVals()));
	}
	
	
	@Test(groups="Regression")
	public void attributeUpdateRecord(){
		/** 
		 * EMPTY BECAUSE YOU CANNOT UPDATE AN ATTRIBUTE. Once it is set it is set forever
		 * 
		 */
	}
	
	
	@Test(groups="Regression")
	public void attributeFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		AttributeRepository attr_repo = new AttributeRepository();

		List<String> attributes = new ArrayList<String>();
		attributes.add("button");
		attributes.add("redbutton");
		Attribute attr = new Attribute("class", attributes);
		attr = attr_repo.create(orient_connection, attr);
		Attribute attr_record = attr_repo.find(orient_connection, attr.getKey());
		
		Assert.assertTrue(attr_record.getKey().equals(attr.getKey()));
		Assert.assertTrue(attr_record.getKey().equals(attr.getKey()));
		Assert.assertTrue(attr_record.getName().equals(attr.getName()));
		Assert.assertTrue(attr_record.getVals().equals(attr.getVals()));
	}
}
