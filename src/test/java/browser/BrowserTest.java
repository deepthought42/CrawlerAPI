package browser;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.junit.Test;
import org.openqa.grid.common.exception.GridException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.minion.browsing.Browser;
import com.qanairy.helpers.BrowserConnectionHelper;
import com.qanairy.models.enums.BrowserEnvironment;
import com.qanairy.models.enums.BrowserType;
import com.qanairy.services.BrowserService;

/**
 * 
 */
public class BrowserTest {
	
	@Test
	public void verifyCleanAttributeValuesString(){
		String src_example = "PDF: Pearson\'s Watson-Glaser II Critical Thinking Appraisal and CPP\'s CPI 260 assessment";
		String clean_src = BrowserService.cleanAttributeValues(src_example);
		System.err.println("clean src: " +clean_src);
		assertTrue("PDF: Pearson's Watson-Glaser II Critical Thinking Appraisal and CPP's CPI 260 assessment".equals(clean_src));
		
		src_example = "section[contains(@class,\"dashboard-content-wrapper \t     col-lg-10 col-lg-offset-1 \t\t    col-xs-12 col-xs-offset-0    bordered-box\")]";
		clean_src = BrowserService.cleanAttributeValues(src_example);
		System.err.println("clean src: " +clean_src);
		assertTrue("section[contains(@class,\\\"dashboard-content-wrapper col-lg-10 col-lg-offset-1 col-xs-12 col-xs-offset-0 bordered-box\\\")]".equals(clean_src));
	}

	
	@Test
	public void verifyUrlReaderForHttps() throws MalformedURLException {
		URL url = new URL("https://misorobotics.com/wp-content/plugins/contact-form-7-style/css/responsive.css?ver=3.1.8");
		try {
			String output = Browser.URLReader(url);
			System.out.println("output           :: "+output);
			assertTrue(output!= null);
			assertTrue(!output.isEmpty());
		} catch (KeyManagementException | NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	//@Test
	public void verifyGenerateParentXpath(){
		try{
			Browser browser = BrowserConnectionHelper.getConnection(BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY);
			browser.navigateTo("https://staging-marketing.qanairy.com");
			WebElement element = browser.getDriver().findElement(By.xpath("//li//a[contains(@href,'features.html')]/../../.."));
			Map<String, String> attributes = browser.extractAttributes(element);

			BrowserService browser_service = new BrowserService();
			String xpath = browser_service.generateXpath(element, browser.getDriver(), attributes);
			System.err.println("XPATH :: " + xpath);
			//log.info("clean src: " +clean_src);
		//	Assert.assertTrue("concat('This is a embedded ', '\"', 'path', '\"', '')".equals(clean_src));
		}
		catch(Exception e){
			
		}
	}
	
	public void scrollToElementInChrome() throws MalformedURLException{
		Browser browser = BrowserConnectionHelper.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
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
				Browser browser = BrowserConnectionHelper.getConnection(BrowserType.FIREFOX, BrowserEnvironment.DISCOVERY);
				browser.navigateTo("https://qa-testbed.qanairy.com/elements/index.html");
				WebElement element = browser.getDriver().findElement(By.xpath("//button"));
				
				Map<String, String> attributes = browser.extractAttributes(element);
				
				assertTrue(attributes.containsKey("id"));
				assertEquals(1, attributes.get("id").length());
				
				assertTrue(attributes.containsKey("class"));
				assertEquals(3, attributes.get("class").length());
				
				assertTrue(attributes.containsKey("style"));
				assertEquals(1, attributes.get("style").length());
				break;
			}catch(GridException e){
				
			}
			catch(WebDriverException e){
				
			}
			cnt++;
		}while(cnt<5);
	}
}
