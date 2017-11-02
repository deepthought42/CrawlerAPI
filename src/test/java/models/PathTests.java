package models;
import org.testng.annotations.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.minion.aws.UploadObjectSingleOperation;
import com.minion.browsing.Browser;
import com.qanairy.models.Action;
import com.qanairy.models.Attribute;
import com.qanairy.models.Page;
import com.qanairy.models.PageElement;
import com.qanairy.models.Path;
import com.qanairy.models.PathObject;
import com.qanairy.models.dto.PathRepository;
import com.qanairy.persistence.IPath;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class PathTests {
	private static Logger log = LoggerFactory.getLogger(PathTests.class);

	@Test(groups="Path")
	public void pathDatabaseRecordConfirmation(){
		Path path = new Path();
		path.setIsUseful(false);
		path.setSpansMultipleDomains(false);
		
		List<Attribute> attributes = new ArrayList<Attribute>();
		List<String> attr_strings = new ArrayList<String>();
		attr_strings.add("spacejam");
		attributes.add(new Attribute("class", attr_strings));
		
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

		path.add(page);
		
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", "purple");
		
		PageElement page_element = new PageElement("test element", "//div", "div", attributes, css_map);
		path.add(page_element);
		
		Action action = new Action("click");
				
		path.add(action);
		
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		PathRepository path_repo = new PathRepository();
		path_repo.convertToRecord(orient_connection, path);
		orient_connection.save();
		
		//look up path and verify all elements
		Path path_record = path_repo.find(orient_connection, path.getKey());
		
		log.info("path object record type : "+path_record.getPath().get(0).getType());
		Assert.assertTrue(path_record.getPath().get(0).getType().equals("Page"));
		
		log.info("path object record type 1: "+path_record.getPath().get(1).getType());
		Assert.assertTrue(path_record.getPath().get(1).getType().equals("PageElement"));
		
		log.info("path object record type 2: "+path_record.getPath().get(2));
		Assert.assertTrue(path_record.getPath().get(2).getType().equals("Action"));
	}
	
	/*@Test
	public void createPath(){
		Path path = new Path(null);
		Page page = null;
		try {
			Browser browser = new Browser("http://localhost", "phantomjs");
			URL page_url = new URL(browser.getDriver().getCurrentUrl());
			page = new Page(browser.getDriver().getPageSource(), 
							browser.getDriver().getCurrentUrl(), 
							UploadObjectSingleOperation.saveImageToS3(Browser.getScreenshot(browser.getDriver()), page_url.getHost(), page_url.getPath().toString()), 
							Browser.getVisibleElements(browser.getDriver(), ""));
			browser.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		path.getPath().add(page);
	}
	*/
	
	@Test
	public void testPathClone(){
		assert true;
	}
	
	@Test
	public void testPathCorrectnessUpdate(){
		
	}
	
	@Test
	public void testDeleteRecord(){
		
	}
}
