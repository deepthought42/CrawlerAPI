package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.GroupPOJO;
import com.qanairy.models.dao.GroupDao;
import com.qanairy.models.dao.impl.GroupDaoImpl;
import com.qanairy.persistence.Group;

/**
 * Defines all tests for the service package POJO
 */
public class GroupTests {
	
	/**
	 * 
	 */
	@Test(groups="Regression")
	public void groupCreateRecord(){
		Group group = new GroupPOJO("Regression");
		GroupDao group_dao = new GroupDaoImpl();

		Group created_group = group_dao.save(group);
		
		Assert.assertEquals(created_group.getKey(),group.getKey());
		Assert.assertTrue(created_group.getDescription().equals(group.getDescription()));
		Assert.assertTrue(created_group.getName().equals(group.getName()));
	}
}
