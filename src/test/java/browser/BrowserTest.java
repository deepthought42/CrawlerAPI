package browser;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

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
	
	@Test
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue("<html><head></head></html>".equals(clean_src));
	}
	
	//@Test
	public void verifyGenerateConcatForXpath(){
		String src_example = "This is a embedded \"path\"";
		BrowserService service = new BrowserService();
		String clean_src = service.generateConcatForXPath(src_example);// cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue("concat('This is a embedded ', '\"', 'path', '\"', '')".equals(clean_src));
	}
	
	
	//@Test
	public void verifyTestConstructor() throws NoSuchAlgorithmException{
		try {
			Set<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
			screenshots.add(new ScreenshotSet("https://s3-us-west-2.amazonaws.com/qanairy/www.zaelab.com/pagestate::861a2edcfedf97c7ab4040a2420a6b86fe5e2db543880136567daecc6e20e8711f1f8a02586a3eca4a0aa17503ce368560516d456c244f100bd14b0df79ad896006803ca0a01edf30090275bb60e800335163d667c10480416a832009a0050e0805d061d52b010f3561f1744708f6df7d65462cf1386bd2cf2c320c5385576b31c30aa0e6ca5f7b6cc922ad5083aa8b35c5e8a15eaa08c78ca0fece91038015638f76931404c7000c86854a151e0988a2fb6c481c668b83164ba74040a9f13b09282a008d31e6a95313a4853eca2ec142c6222f1c528cd2988b63aa3a8a63ea1558c21fde256736ef5882719c644511842c9999788c389a6e0247031a033a7c67/viewport.png", "chrome"));

			Set<PageElement> elements = new HashSet<PageElement>();
			
			PageState page = new PageState(	"http://localhost", 
					screenshots,
					elements,
					false,
					"");
			
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
