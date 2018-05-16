package models;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.AttributePOJO;
import com.qanairy.models.dao.AttributeDao;
import com.qanairy.models.dao.impl.AttributeDaoImpl;
import com.qanairy.persistence.Attribute;
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
		
		Attribute attr = new AttributePOJO("class", attributes);
		AttributeDao attr_repo = new AttributeDaoImpl();
		attr_repo.save(attr);

		Attribute attr_record = attr_repo.find(attr.getKey()); 
		Assert.assertTrue(attr_record.getKey().equals(attr.getKey()));
		Assert.assertTrue(attr_record.getName().equals(attr.getName()));
		Assert.assertTrue(attr_record.getVals().equals(attr.getVals()));
	}
}
