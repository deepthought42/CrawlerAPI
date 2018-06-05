package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.TestUserPOJO;
import com.qanairy.models.dao.TestUserDao;
import com.qanairy.models.dao.impl.TestUserDaoImpl;
import com.qanairy.persistence.TestUser;

public class TestUserTests {
	
	@Test(groups="regression")
	public void assertTestUserPersists(){
		TestUser user = new TestUserPOJO("username", "password", "role");
		
		TestUserDao test_user_dao = new TestUserDaoImpl();
		TestUser current_user = test_user_dao.save(user);
		
		Assert.assertEquals(current_user.getKey(), user.getKey());
		Assert.assertEquals(current_user.getPassword(), user.getPassword());
		Assert.assertEquals(current_user.getRole(), user.getRole());
		Assert.assertEquals(current_user.getUsername(), user.getUsername());	
	}
}
