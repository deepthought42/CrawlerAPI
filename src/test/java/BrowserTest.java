import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.qanairy.models.PageStatePOJO;
import com.qanairy.persistence.PageElement;
import com.qanairy.persistence.PageState;
import com.qanairy.persistence.PathObject;
import com.qanairy.persistence.ScreenshotSet;

/**
 * 
 */
public class BrowserTest {

	@Test(groups="Regression")
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		//System.err.println("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("<html><head></head></html>"));
	}
	
	
	@Test(groups="Regression")
	public void verifyGenerateConcatForXpath(){
		String src_example = "This is a embedded \"path\"";
		String clean_src = Browser.generateConcatForXPath(src_example);// cleanSrc(src_example);
		//System.err.println("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("concat('This is a embedded ', '\"', 'path', '\"', '')"));
	}
	
	
	@Test(groups="Regression")
	public void verifyTestConstructor(){
		PageState page;
		try {
			page = new PageStatePOJO("<html>localhost</html>",
					"http://localhost", 
					new ArrayList<ScreenshotSet>(),
					new ArrayList<PageElement>(), 
					false);
			
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page.getKey());
			
			List<PathObject> path_objects = new ArrayList<PathObject>();
			path_objects.add(page);
			
			com.qanairy.persistence.Test test = new com.qanairy.models.TestPOJO(path_keys, path_objects, page, "Testing Test 1");
			
			Assert.assertEquals(test.getPathKeys().size(), path_keys.size());
			Assert.assertEquals(test.getPathObjects().size(), path_objects.size());
			Assert.assertEquals(test.getRunTime(), 0L);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
