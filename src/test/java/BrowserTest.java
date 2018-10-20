import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.minion.browsing.Browser;
import com.qanairy.models.PageElement;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.services.BrowserService;

/**
 * 
 */
public class BrowserTest {

	@Test(groups="Regression")
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("<html><head></head></html>"));
	}
	
	
	@Test(groups="Regression")
	public void verifyGenerateConcatForXpath(){
		String src_example = "This is a embedded \"path\"";
		BrowserService service = new BrowserService();
		String clean_src = service.generateConcatForXPath(src_example);// cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("concat('This is a embedded ', '\"', 'path', '\"', '')"));
	}
	
	
	@Test(groups="Regression")
	public void verifyTestConstructor(){
		try {
			Set<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
			screenshots.add(new ScreenshotSet("http://qanairy.com", "chrome"));

			Set<PageElement> elements = new HashSet<PageElement>();
			
			PageState page = new PageState("<html>localhost</html>",
					"http://localhost", 
					screenshots,
					elements,
					false);
			
			List<String> path_keys = new ArrayList<String>();
			path_keys.add(page.getKey());
			
			List<PathObject> path_objects = new ArrayList<PathObject>();
			path_objects.add(page);
			
			com.qanairy.models.Test test = new com.qanairy.models.Test(path_keys, path_objects, page, "Testing Test 1");
			
			Assert.assertEquals(test.getPathKeys().size(), path_keys.size());
			Assert.assertEquals(test.getPathObjects().size(), path_objects.size());
			Assert.assertEquals(test.getRunTime(), 0L);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
