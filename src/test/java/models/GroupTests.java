package models;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Group;
import com.qanairy.models.dto.GroupRepository;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * Defines all tests for the service package POJO
 */
public class GroupTests {
	
	/**
	 * 
	 */
	@Test
	public void groupCreateRecord(){
		Group group = new Group("Regression", new ArrayList<com.qanairy.models.Test>());
		GroupRepository group_repo = new GroupRepository();

		Group created_group = group_repo.create(new OrientConnectionFactory(), group);
		
		
		Assert.assertTrue(created_group.getKey().equals(group.getKey()));
		Assert.assertTrue(created_group.getDescription().equals(group.getDescription()));
		Assert.assertTrue(created_group.getName().equals(group.getName()));
	}
	
	/**
	 * 
	 */
	@Test
	public void groupUpdateRecord(){
		Group group = new Group("Smoke", new ArrayList<com.qanairy.models.Test>());
		GroupRepository group_repo = new GroupRepository();
		group.setKey(group_repo.generateKey(group));

		Group group_record = group_repo.update(new OrientConnectionFactory(), group);
		
		Assert.assertTrue(group_record.getKey().equals(group.getKey()));
		Assert.assertTrue(group_record.getDescription().equals(group.getDescription()));
		Assert.assertTrue(group_record.getName().equals(group.getName()));
	}
	
	/**
	 * 
	 */
	@Test
	public void groupFindRecord(){
		Group group = new Group("Smoke", new ArrayList<com.qanairy.models.Test>());
		GroupRepository group_repo = new GroupRepository();
		group.setKey(group_repo.generateKey(group));
		Group group_record = group_repo.update(new OrientConnectionFactory(), group);
		
		Assert.assertTrue(group_record.getKey().equals(group.getKey()));
		Assert.assertTrue(group_record.getDescription().equals(group.getDescription()));
		Assert.assertTrue(group_record.getName().equals(group.getName()));
	}
}
