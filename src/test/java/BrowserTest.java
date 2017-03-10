import java.net.MalformedURLException;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.browsing.Browser;
import com.minion.structs.Path;
import com.qanairy.models.Domain;
import com.qanairy.models.Page;

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
			e.printStackTrace();
		}
		com.qanairy.models.Test test = new com.qanairy.models.Test(new Path(), new Page(), new Domain(page.getUrl().getHost()));
		Assert.assertTrue(test.getDomain().getUrl().toString().equals(page.getUrl().getHost()));
		Assert.assertTrue(test.getKey() != null);
	}
}
