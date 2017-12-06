package models;

import java.io.IOException;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qanairy.models.Domain;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageSource;
import com.qanairy.models.Path;
import com.qanairy.models.dto.TestRepository;
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
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html></html>",
								 "http://www.test.test", "", new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"));
			test.setKey(test_repo.generateKey(test));
			com.qanairy.models.Test test_record = test_repo.create(new OrientConnectionFactory(), test);
			
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
			TestRepository test_repo = new TestRepository();
			Page page = new Page("<html><body></body></html>",
								 "http://www.test.test", "", new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"));
			test.setKey(test_repo.generateKey(test));
			com.qanairy.models.Test test_record_create = test_repo.create(new OrientConnectionFactory(), test);

			com.qanairy.models.Test test_record = test_repo.update(new OrientConnectionFactory(), test_record_create);
			
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
			Page page = new Page("<html></html>",
								 "http://www.test.test", "", new ArrayList<PageElement>(), true);
			Path path = new Path();
			path.add(page);
			TestRepository test_repo = new TestRepository();

			test = new com.qanairy.models.Test(path, page, new Domain("www.test.test", "", "http"));
			test = test_repo.create(orient_connection, test);
			com.qanairy.models.Test test_record = test_repo.find(orient_connection, test.getKey());
			
			Assert.assertTrue(test_record.getKey().equals(test.getKey()));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
