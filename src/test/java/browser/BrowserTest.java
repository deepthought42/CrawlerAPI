package browser;

import static org.junit.Assert.*;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;

import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.qanairy.models.Attribute;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.PathObject;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.services.BrowserService;

/**
 * 
 */
@SpringBootTest
public class BrowserTest {

	@Test
	public void verifyCleanSrc(){
		String src_example = "<html><head></head><canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"1596\"></canvas></html>";
		
		String clean_src = Browser.cleanSrc(src_example);
		//log.info("clean src: " +clean_src);
		Assert.assertTrue("<html><head></head></html>".equals(clean_src));
	}
	
	@Test
	public void verifyEscapeQuotes(){
		String src_example = "PDF: Pearson\'s Watson-Glaser II Critical Thinking Appraisal and CPP\'s CPI 260 assessment";
		String clean_src = BrowserService.escapeQuotes(src_example);// cleanSrc(src_example);
		System.err.println("clean src: " +clean_src);
		assertTrue("PDF: Pearson's Watson-Glaser II Critical Thinking Appraisal and CPP's CPI 260 assessment".equals(clean_src));
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
			List<ElementState> elements = new ArrayList<ElementState>();
			
			PageState page = new PageState(	"http://localhost", 
					"https://s3-us-west-2.amazonaws.com/qanairy/www.zaelab.com/pagestate::861a2edcfedf97c7ab4040a2420a6b86fe5e2db543880136567daecc6e20e8711f1f8a02586a3eca4a0aa17503ce368560516d456c244f100bd14b0df79ad896006803ca0a01edf30090275bb60e800335163d667c10480416a832009a0050e0805d061d52b010f3561f1744708f6df7d65462cf1386bd2cf2c320c5385576b31c30aa0e6ca5f7b6cc922ad5083aa8b35c5e8a15eaa08c78ca0fece91038015638f76931404c7000c86854a151e0988a2fb6c481c668b83164ba74040a9f13b09282a008d31e6a95313a4853eca2ec142c6222f1c528cd2988b63aa3a8a63ea1558c21fde256736ef5882719c644511842c9999788c389a6e0247031a033a7c67/viewport.png",
					elements,
					false,
					"",
					0,0,
					1288,844,
					"chrome");
			
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
	
	public void scrollToElementInChrome() throws MalformedURLException{
		Browser browser = BrowserConnectionFactory.getConnection("chrome", BrowserEnvironment.DISCOVERY);
		browser.navigateTo("https://qa-testbed.qanairy.com/viewport_pages/element_out_of_view_y_axis.html");
		WebElement element = browser.getDriver().findElement(By.xpath("//button"));
		
		browser.scrollToElement(element);
		
		assertEquals(0, browser.getXScrollOffset());
		assertEquals(553, browser.getYScrollOffset());
	}
	
	public void verifyAttributes() throws MalformedURLException{
		int cnt = 0;
		do{
			try{
				Browser browser = BrowserConnectionFactory.getConnection("firefox", BrowserEnvironment.DISCOVERY);
				browser.navigateTo("https://qa-testbed.qanairy.com/elements/index.html");
				WebElement element = browser.getDriver().findElement(By.xpath("//button"));
				
				Set<Attribute> attributes = browser.extractAttributes(element);
		
				Map<String, List<String>> attribute_map = new HashMap<String, List<String>>();
				for(Attribute attr : attributes){
					attribute_map.put(attr.getName(), attr.getVals());
				}
				
				assertTrue(attribute_map.containsKey("id"));
				assertEquals(1, attribute_map.get("id").size());
				
				assertTrue(attribute_map.containsKey("class"));
				assertEquals(3, attribute_map.get("class").size());
				
				assertTrue(attribute_map.containsKey("style"));
				assertEquals(1, attribute_map.get("style").size());
				break;
			}catch(GridException e){
				
			}
			catch(WebDriverException e){
				
			}
			cnt++;
		}while(cnt<5);
	}
}
