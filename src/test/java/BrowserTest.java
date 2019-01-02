import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
	
	@Mock
	private WebDriver driver;
	
	@Mock
	private WebElement web_element;
	
	@Mock
	private BufferedImage buffered_img;
	
	@Mock
	private Dimension dimension;
	
	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
	}
	
	//@Test
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("<html><head></head></html>"));
	}
	
	
	//@Test
	public void verifyGenerateConcatForXpath(){
		String src_example = "This is a embedded \"path\"";
		BrowserService service = new BrowserService();
		String clean_src = service.generateConcatForXPath(src_example);// cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue(clean_src.equals("concat('This is a embedded ', '\"', 'path', '\"', '')"));
	}
	
	
	@Test
	public void verifyTestConstructor(){
		try {
			Set<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
			screenshots.add(new ScreenshotSet("http://qanairy.com", "chrome"));

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
			e.printStackTrace();
		}
	}

	@Test
	public void getElementScreenshotForElementWithinView() throws IOException{
		int last_y_pos = 0;
		
		when(web_element.getSize()).thenReturn(new Dimension(10, 10));
		when(web_element.getLocation()).thenReturn(new Point(0, 0));
		when(buffered_img.getHeight()).thenReturn(800);
		when(buffered_img.getWidth()).thenReturn(1280);
		
		BufferedImage buffered_img = ImageIO.read(new File("C:\\Users\\brand\\workspace\\WebTestVisualizer\\src\\test\\resources\\screenshot.png"));
		
		BufferedImage img = Browser.getElementScreenshot(driver, web_element, buffered_img, last_y_pos, dimension);
		
		assertNotNull(img);
	}
}

