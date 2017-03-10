package models;

import java.io.IOException;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.structs.Path;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.persistence.ITest;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 *
 */
public class TestTests {
	
	/**
	 * 
	 */
	@Test
	public void testCreateRecord(){
		com.qanairy.models.Test test;
		try {
			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			ITest test_record = test.create(new OrientConnectionFactory());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * 
	 */
	@Test
	public void testUpdateRecord(){
		com.qanairy.models.Test test;
		try {
			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			ITest test_record = test.update(new OrientConnectionFactory());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void testFindRecord(){
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();

		com.qanairy.models.Test test;
		try {
			test = new com.qanairy.models.Test(new Path(), new Page("<html></html>","http://www.test.test", "", new ArrayList<PageElement>(), true), new Domain("http://www.test.test"));
			test.create(orient_connection);
			ITest test_record = test.find(orient_connection);
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
