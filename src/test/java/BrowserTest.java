import java.net.MalformedURLException;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.minion.browsing.Page;
import com.minion.structs.Path;

public class BrowserTest {

	@Test
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		System.out.println("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("<html><head></head></html>"));
	}
	
	@Test
	public void verifyTestConstructor(){
		Page page  = new Page();
		try {
			page.setUrl(new URL("http://localhost"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		com.minion.tester.Test test = new com.minion.tester.Test(new Path(), new Page(), page.getUrl().getHost());
		
		Assert.assertTrue(test.getDomain().equals(page.getUrl().getHost()));
		Assert.assertTrue(test.getKey() != null);
	}
}
