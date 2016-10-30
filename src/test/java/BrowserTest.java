import org.testng.Assert;
import org.testng.annotations.Test;

import com.minion.browsing.Browser;

public class BrowserTest {

	@Test
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		System.out.println("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("<html><head></head></html>"));
	}
}
