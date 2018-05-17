package models;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.ActionPOJO;
import com.qanairy.models.dao.ActionDao;
import com.qanairy.models.dao.impl.ActionDaoImpl;
import com.qanairy.persistence.Action;

/**
 * Defines all tests for the service package Repository
 */
public class ActionTests {
	
	@Test(groups="Regression")
	public void assertActionWithoutValuePersists(){
		Action action= new ActionPOJO("test_click");
		ActionDao action_dao = new ActionDaoImpl();
		action_dao.save(action);
		
		Action action_record = action_dao.find(action.getKey());
		Assert.assertEquals(action_record.getKey(), action.getKey());
		Assert.assertEquals(action_record.getName(), action.getName());
		Assert.assertEquals(action_record.getValue(), action.getValue());
	}
	
	@Test(groups="Regression")
	public void assertActionWithValuePersists(){
		Action action= new ActionPOJO("test_click", "test_value");
		ActionDao action_dao = new ActionDaoImpl();
		action_dao.save(action);
		
		Action action_record = action_dao.find(action.getKey());
		Assert.assertEquals(action_record.getKey(), action.getKey());
		Assert.assertEquals(action_record.getName(), action.getName());
		Assert.assertEquals(action_record.getValue(), action.getValue());
	}
}
