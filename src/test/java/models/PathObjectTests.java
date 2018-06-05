package models;

import java.util.ArrayList;
import java.util.HashMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.ActionPOJO;
import com.qanairy.models.PageElementPOJO;
import com.qanairy.models.dao.PathObjectDao;
import com.qanairy.models.dao.impl.PathObjectDaoImpl;
import com.qanairy.persistence.Attribute;
import com.qanairy.persistence.PathObject;

/**
 * Defines all tests for the service package Repository
 */
public class PathObjectTests {
	
	@Test(groups="Regression")
	public void assertActionAsPathObjectPersists(){
		PathObject path_obj = new ActionPOJO("test_click");
		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		path_obj_dao.save(path_obj);
		
		PathObject action_record = path_obj_dao.find(path_obj.getKey());
		Assert.assertEquals(action_record.getKey(), path_obj.getKey());
		Assert.assertEquals(action_record.getType(), path_obj.getType());
	}	
	
	@Test(groups="Regression")
	public void assertPageElementPathObjectPersists(){
		PathObject path_obj = new PageElementPOJO("This is a button", "//button", "input", new ArrayList<Attribute>(), new HashMap<String, String>());
		PathObjectDao path_obj_dao = new PathObjectDaoImpl();
		path_obj_dao.save(path_obj);
		
		PathObject action_record = path_obj_dao.find(path_obj.getKey());
		Assert.assertEquals(action_record.getKey(), path_obj.getKey());
		Assert.assertEquals(action_record.getType(), path_obj.getType());
	}	
}
