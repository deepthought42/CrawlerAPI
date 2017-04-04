package models;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.testng.annotations.Test;

import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PathTests {
	
	@Test(groups="Path")
	public void createPath(){
		Path path = new Path();
		path.setIsUseful(false);
		path.setSpansMultipleDomains(false);
		
		Page page = new Page();
		page.setLandable(true);
		page.setScreenshot("Test screenshot url");
		page.setSrc("src goes here 1");
		page.setElements(new ArrayList<PageElement>());
		try {
			page.setUrl(new URL("http://www.test.com/test1"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		//page.setKey(page.generateKey());
		path.add(page);
		//path.setKey(path.generateKey());

		
		Page page2 = new Page();
		page2.setLandable(true);
		page2.setScreenshot("Test screenshot url");
		page2.setSrc("src goes here 2");
		page2.setElements(new ArrayList<PageElement>());

		try {
			page2.setUrl(new URL("http://www.test.com/test2"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		path.add(page2);
		
		Page page3 = new Page();
		page3.setLandable(true);
		page3.setScreenshot("Test screenshot url");
		page3.setSrc("src goes here 3");
		page3.setElements(new ArrayList<PageElement>());

		try {
			page3.setUrl(new URL("http://www.test.com/test3"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		path.add(page3);
		
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		PathRepository path_repo = new PathRepository();
		path_repo.convertToRecord(orient_connection, path);
		orient_connection.save();
	}
	
	@Test
	public void testPathClone(){
		assert false;
	}
	
	@Test
	public void testPathCorrectnessUpdate(){
		
	}
	
	@Test
	public void testDeleteRecord(){
		
	}
}
